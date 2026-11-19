package com.vlu.freelance.service;

import com.vlu.freelance.config.JwtTokenProvider;
import com.vlu.freelance.dto.AuthResponse;
import com.vlu.freelance.dto.LoginRequest;
import com.vlu.freelance.dto.RegisterRequest;
import com.vlu.freelance.dto.UserSummary;
import com.vlu.freelance.entity.Profile;
import com.vlu.freelance.entity.User;
import com.vlu.freelance.exception.BusinessRuleException;
import com.vlu.freelance.exception.ForbiddenException;
import com.vlu.freelance.exception.UnauthorizedException;
import com.vlu.freelance.repository.ProfileRepository;
import com.vlu.freelance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * FR-02, FR-04: Đăng nhập hệ thống cho Sinh viên (STUDENT), NTD (EMPLOYER) và Quản trị viên (ADMIN)
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

        // Kiểm tra mật khẩu (hỗ trợ BCrypt và fallback nếu nạp seed thô)
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash())
                || request.getPassword().equals(user.getPasswordHash());

        if (!passwordMatches) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác");
        }

        // FR-26: Kiểm tra trạng thái tài khoản bị khóa
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            throw new ForbiddenException("Tài khoản của bạn đã bị khóa bởi Quản trị viên (FR-26)");
        }

        // Lấy họ tên hiển thị từ tbl_profiles
        String fullName = profileRepository.findByUserId(user.getId())
                .map(Profile::getFullName)
                .orElse(user.getRole().equals("ADMIN") ? "Quản trị viên hệ thống" : user.getEmail());

        // Sinh JWT token chứa userId và role (thời hạn 2 giờ theo đặc tả NFR-02)
        String token = jwtTokenProvider.generateToken(user);

        UserSummary summary = new UserSummary(user.getId(), user.getEmail(), user.getRole(), fullName);
        return new AuthResponse(token, summary);
    }

    /**
     * FR-01: Đăng ký tài khoản Sinh viên hoặc Nhà tuyển dụng
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Email này đã được đăng ký trong hệ thống");
        }

        // Tạo tài khoản trong tbl_users
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole().toUpperCase());
        user.setStatus("ACTIVE"); // Mặc định ACTIVE theo FR-01
        User savedUser = userRepository.save(user);

        // Tạo hồ sơ đồng thời trong tbl_profiles
        Profile profile = new Profile(savedUser.getId(), request.getFullName().trim());
        profileRepository.save(profile);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", savedUser.getId());
        response.put("email", savedUser.getEmail());
        response.put("role", savedUser.getRole());
        response.put("status", savedUser.getStatus());
        return response;
    }
}
