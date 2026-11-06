package com.marketplace.services;

import com.marketplace.dtos.ProfileResponse;
import com.marketplace.dtos.UpdateProfileRequest;
import com.marketplace.entities.Location;
import com.marketplace.entities.Profile;
import com.marketplace.entities.User;
import com.marketplace.entities.UserRole;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.LocationRepository;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dịch vụ xử lý nghiệp vụ Hồ sơ cá nhân cho Sinh viên (FR-05, FR-11) và Nhà tuyển dụng (FR-16).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;

    /**
     * Lấy thông tin hồ sơ cá nhân của người dùng đang đăng nhập (FR-05, FR-16).
     *
     * @param currentUserId ID của người dùng hiện tại lấy từ JWT Principal
     * @return Thông tin chi tiết hồ sơ cá nhân
     * @throws AppException Mã lỗi USER_NOT_FOUND hoặc PROFILE_NOT_FOUND nếu không tìm thấy dữ liệu
     */
    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile(Long currentUserId) {
        Profile profile = profileRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new AppException("PROFILE_NOT_FOUND", "Không tìm thấy hồ sơ cá nhân của người dùng"));
        return mapToProfileResponse(profile);
    }

    /**
     * Xem hồ sơ cá nhân của người dùng theo userId (dành cho EMPLOYER và ADMIN):
     * - ADMIN: Được phép xem hồ sơ của mọi người dùng.
     * - EMPLOYER: Chỉ được xem hồ sơ của Sinh viên đã ứng tuyển vào tin tuyển dụng do chính NTD đó sở hữu (hoặc tự xem chính mình).
     *
     * @param viewerId     ID người đang thực hiện xem (lấy từ JWT)
     * @param viewerRole   Vai trò của người xem (EMPLOYER hoặc ADMIN)
     * @param targetUserId ID người dùng cần xem hồ sơ
     * @return Thông tin chi tiết hồ sơ của người dùng
     * @throws AppException Mã lỗi FORBIDDEN_RESOURCE (403) nếu không đủ quyền sở hữu, hoặc PROFILE_NOT_FOUND (404) nếu không tồn tại
     */
    @Transactional(readOnly = true)
    public ProfileResponse getUserProfileById(Long viewerId, String viewerRole, Long targetUserId) {
        Profile targetProfile = profileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new AppException("PROFILE_NOT_FOUND", "Không tìm thấy hồ sơ cá nhân của người dùng"));

        // FR-04 & QT-AUTH: Phân quyền tầng 2 (Resource Ownership Check) cho Nhà tuyển dụng
        if (UserRole.ADMIN.name().equals(viewerRole)) {
            return mapToProfileResponse(targetProfile);
        }

        if (UserRole.EMPLOYER.name().equals(viewerRole)) {
            if (viewerId.equals(targetUserId)) {
                return mapToProfileResponse(targetProfile);
            }
            User targetUser = targetProfile.getUser();
            if (!isStudentRole(targetUser) || !hasStudentAppliedToEmployerJobs(targetUserId, viewerId)) {
                throw new AppException(
                        "FORBIDDEN_RESOURCE",
                        "Nhà tuyển dụng chỉ được phép xem hồ sơ của sinh viên đã ứng tuyển vào tin của mình"
                );
            }
            return mapToProfileResponse(targetProfile);
        }

        throw new AppException("FORBIDDEN_RESOURCE", "Bạn không có quyền xem hồ sơ của người dùng khác");
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân theo đúng vai trò:
     * - Sinh viên (FR-11): Cập nhật trường học, chuyên ngành, CV dạng văn bản và giới thiệu bản thân.
     * - Nhà tuyển dụng (FR-16): Cập nhật tên công ty/cửa hàng, người đại diện và giới thiệu đơn vị.
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param request       Dữ liệu cập nhật hồ sơ
     * @return Hồ sơ cá nhân sau khi cập nhật
     * @throws AppException Mã lỗi USER_NOT_FOUND hoặc LOCATION_NOT_FOUND nếu dữ liệu tham chiếu không tồn tại
     */
    @Transactional
    public ProfileResponse updateMyProfile(Long currentUserId, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "Không tìm thấy tài khoản người dùng"));

        // Resource Ownership Check: Chỉ lấy và cập nhật hồ sơ gắn với chính currentUserId
        Profile profile = profileRepository.findByUserId(currentUserId)
                .orElseGet(() -> Profile.builder().user(user).build());

        // FR-05: Cập nhật thông tin chung cho mọi vai trò
        profile.setFullName(request.getFullName().trim());
        profile.setPhone(normalizeNullableText(request.getPhone()));
        profile.setBio(normalizeNullableText(request.getBio()));

        if (request.getLocationId() != null) {
            Location location = locationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new AppException("LOCATION_NOT_FOUND", "Địa điểm làm việc/sinh sống đã chọn không tồn tại"));
            profile.setLocation(location);
        } else {
            profile.setLocation(null);
        }

        // Phân nhánh cập nhật theo đúng vai trò nghiệp vụ để bảo vệ tính toàn vẹn dữ liệu
        if (isStudentRole(user)) {
            // FR-11: Sinh viên cập nhật trường học, chuyên ngành và nội dung CV
            profile.setSchool(normalizeNullableText(request.getSchool()));
            profile.setMajor(normalizeNullableText(request.getMajor()));
            profile.setCvContent(normalizeNullableText(request.getCvContent()));
            profile.setCompanyName(null);
        } else if (isEmployerRole(user)) {
            // FR-16: Nhà tuyển dụng cập nhật thông tin doanh nghiệp / cửa hàng
            profile.setCompanyName(normalizeNullableText(request.getCompanyName()));
            profile.setSchool(null);
            profile.setMajor(null);
            profile.setCvContent(null);
        } else {
            // Quản trị viên (ADMIN)
            profile.setCompanyName(normalizeNullableText(request.getCompanyName()));
        }

        Profile savedProfile = profileRepository.save(profile);
        return mapToProfileResponse(savedProfile);
    }

    /**
     * Kiểm tra sinh viên đã từng nộp hồ sơ ứng tuyển vào tin tuyển dụng của nhà tuyển dụng hay chưa.
     *
     * @param studentId  ID tài khoản sinh viên
     * @param employerId ID tài khoản nhà tuyển dụng
     * @return true nếu có ít nhất 1 đơn ứng tuyển
     */
    public boolean hasStudentAppliedToEmployerJobs(Long studentId, Long employerId) {
        return profileRepository.countStudentApplicationsForEmployer(studentId, employerId) > 0;
    }

    /**
     * Kiểm tra người dùng có vai trò Sinh viên (STUDENT) hay không.
     *
     * @param user Thực thể người dùng
     * @return true nếu là STUDENT
     */
    public boolean isStudentRole(User user) {
        return user != null && user.getRole() == UserRole.STUDENT;
    }

    /**
     * Kiểm tra người dùng có vai trò Nhà tuyển dụng (EMPLOYER) hay không.
     *
     * @param user Thực thể người dùng
     * @return true nếu là EMPLOYER
     */
    public boolean isEmployerRole(User user) {
        return user != null && user.getRole() == UserRole.EMPLOYER;
    }

    private String normalizeNullableText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private ProfileResponse mapToProfileResponse(Profile profile) {
        User user = profile.getUser();
        Location location = profile.getLocation();
        String locationName = location != null
                ? location.getDistrict() + ", " + location.getCity()
                : null;

        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .locationId(location != null ? location.getId() : null)
                .locationName(locationName)
                .bio(profile.getBio())
                .school(profile.getSchool())
                .major(profile.getMajor())
                .cvContent(profile.getCvContent())
                .companyName(profile.getCompanyName())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
