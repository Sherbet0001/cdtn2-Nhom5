package com.nhom5.partjob.service;

import com.nhom5.partjob.domain.Profile;
import com.nhom5.partjob.domain.Role;
import com.nhom5.partjob.domain.User;
import com.nhom5.partjob.domain.UserStatus;
import com.nhom5.partjob.dto.AuthDtos.*;
import com.nhom5.partjob.exception.ApiException;
import com.nhom5.partjob.repository.ProfileRepository;
import com.nhom5.partjob.repository.UserRepository;
import com.nhom5.partjob.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Dang ky va dang nhap (FR-01, 02, 04). */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Tao dong tbl_users (BCrypt) va dong tbl_profiles trong mot giao dich. */
    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        User user = userRepository.save(
                new User(email, passwordEncoder.encode(req.password()), Role.valueOf(req.role())));
        profileRepository.save(new Profile(user, req.fullName().trim()));
        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole(), user.getStatus());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalizeEmail(req.email())).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Sai email hoặc mật khẩu");
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            throw ApiException.forbidden("Tài khoản đã bị khóa");
        }
        String fullName = profileRepository.findByUserId(user.getId()).map(Profile::getFullName).orElse(null);
        return new LoginResponse(jwtService.generateToken(user), "Bearer", jwtService.getExpirationSeconds(),
                new UserInfo(user.getId(), user.getEmail(), user.getRole(), fullName));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
