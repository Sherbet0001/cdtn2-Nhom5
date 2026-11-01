package com.marketplace.services;

import com.marketplace.dtos.AuthResponse;
import com.marketplace.dtos.LoginRequest;
import com.marketplace.dtos.RegisterRequest;
import com.marketplace.dtos.RegisterResponse;
import com.marketplace.entities.Profile;
import com.marketplace.entities.User;
import com.marketplace.entities.UserRole;
import com.marketplace.entities.UserStatus;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.UserRepository;
import com.marketplace.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dịch vụ xử lý nghiệp vụ xác thực và định danh người dùng (FR-01, FR-02, FR-04).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * Đăng ký tài khoản mới cho Sinh viên hoặc Nhà tuyển dụng và khởi tạo đồng thời hồ sơ cá nhân.
     *
     * @param request Thông tin đăng ký (fullName, email, password, role)
     * @return Thông tin tài khoản vừa tạo (không bao giờ trả về password_hash)
     * @throws AppException Mã lỗi EMAIL_ALREADY_EXISTS nếu email đã tồn tại, hoặc INVALID_ROLE nếu vai trò không hợp lệ
     */
    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // FR-01: Kiểm tra trùng lặp email trước khi tạo tài khoản
        if (hasEmailRegistered(normalizedEmail)) {
            throw new AppException("EMAIL_ALREADY_EXISTS", "Email này đã được đăng ký trên hệ thống");
        }

        // QT-AUTH (FR-04): Chỉ cho phép tự đăng ký vai trò STUDENT hoặc EMPLOYER, tài khoản ADMIN được nạp sẵn
        UserRole targetRole;
        try {
            targetRole = UserRole.valueOf(request.getRole().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new AppException("INVALID_ROLE", "Vai trò đăng ký chỉ chấp nhận STUDENT hoặc EMPLOYER");
        }

        if (targetRole == UserRole.ADMIN) {
            throw new AppException("INVALID_ROLE", "Không được phép tự đăng ký tài khoản Quản trị viên (ADMIN)");
        }

        // QT-AUTH: Bắt buộc băm mật khẩu bằng BCrypt trước khi lưu vào tbl_users.password_hash
        User newUser = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(targetRole)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(newUser);

        // FR-01 & FR-05: Tạo đồng thời bản ghi tbl_profiles gắn 1-1 với tbl_users trong cùng giao dịch
        Profile initialProfile = Profile.builder()
                .user(savedUser)
                .fullName(request.getFullName().trim())
                .build();
        profileRepository.save(initialProfile);

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(initialProfile.getFullName())
                .role(savedUser.getRole().name())
                .status(savedUser.getStatus().name())
                .build();
    }

    /**
     * Xác thực thông tin đăng nhập và phát hành JWT Bearer Token có thời hạn 2 giờ.
     *
     * @param request Thông tin đăng nhập (email, password)
     * @return Đối tượng AuthResponse chứa accessToken, thời hạn 7200 giây và thông tin người dùng
     * @throws AppException Mã lỗi INVALID_CREDENTIALS nếu sai email/mật khẩu, hoặc ACCOUNT_LOCKED nếu tài khoản bị khóa
     */
    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // FR-02: Tra cứu tài khoản theo email
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException("INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác"));

        // QT-AUTH: Đối chiếu mật khẩu thô với chuỗi băm BCrypt trong CSDL
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException("INVALID_CREDENTIALS", "Email hoặc mật khẩu không chính xác");
        }

        // QT-AUTH (FR-26): Chặn đăng nhập nếu tài khoản đã bị Admin khóa
        if (!isAccountActive(user)) {
            throw new AppException("ACCOUNT_LOCKED", "Tài khoản của bạn đã bị khóa bởi Quản trị viên");
        }

        String fullName = profileRepository.findByUserId(user.getId())
                .map(Profile::getFullName)
                .orElse(user.getEmail());

        // QT-AUTH (FR-02, FR-04): Sinh JWT Token có hiệu lực 2 giờ (7200 giây)
        String accessToken = jwtProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getExpirationSeconds())
                .user(AuthResponse.UserSummary.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .fullName(fullName)
                        .status(user.getStatus().name())
                        .build())
                .build();
    }

    /**
     * Kiểm tra email đã tồn tại trong CSDL hay chưa.
     *
     * @param email Địa chỉ email cần kiểm tra
     * @return true nếu đã tồn tại, ngược lại false
     */
    public boolean hasEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Kiểm tra tài khoản đang ở trạng thái hoạt động (ACTIVE).
     *
     * @param user Thực thể người dùng
     * @return true nếu tài khoản đang ACTIVE
     */
    public boolean isAccountActive(User user) {
        return user != null && user.getStatus() == UserStatus.ACTIVE;
    }
}

