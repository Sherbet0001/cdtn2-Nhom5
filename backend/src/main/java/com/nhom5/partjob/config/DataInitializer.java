package com.nhom5.partjob.config;

import com.nhom5.partjob.domain.Location;
import com.nhom5.partjob.domain.Profile;
import com.nhom5.partjob.domain.Role;
import com.nhom5.partjob.domain.User;
import com.nhom5.partjob.repository.LocationRepository;
import com.nhom5.partjob.repository.ProfileRepository;
import com.nhom5.partjob.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Nap du lieu khoi dau khi CSDL con trong: tai khoan ADMIN (nhom nap san, khong cho dang ky)
 * va mot so dia diem mau de thu cap nhat ho so. Bo 1.000 tin mau se lam o buoi sau.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           ProfileRepository profileRepository,
                           LocationRepository locationRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.locationRepository = locationRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = userRepository.save(
                    new User(adminEmail.trim().toLowerCase(Locale.ROOT),
                            passwordEncoder.encode(adminPassword), Role.ADMIN));
            profileRepository.save(new Profile(admin, "Quản trị viên"));
        }
        if (locationRepository.count() == 0) {
            locationRepository.saveAll(List.of(
                    new Location("TP. Hồ Chí Minh", "Quận 1"),
                    new Location("TP. Hồ Chí Minh", "Quận 3"),
                    new Location("TP. Hồ Chí Minh", "Quận 7"),
                    new Location("TP. Hồ Chí Minh", "Bình Thạnh"),
                    new Location("TP. Hồ Chí Minh", "Gò Vấp"),
                    new Location("TP. Hồ Chí Minh", "Thủ Đức"),
                    new Location("Hà Nội", "Cầu Giấy"),
                    new Location("Hà Nội", "Đống Đa")));
        }
    }
}
