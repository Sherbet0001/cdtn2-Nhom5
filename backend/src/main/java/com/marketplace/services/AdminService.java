package com.marketplace.services;

import com.marketplace.dtos.AdminStatsResponse;
import com.marketplace.dtos.AdminUpdateJobStatusRequest;
import com.marketplace.dtos.AdminUserResponse;
import com.marketplace.dtos.JobResponse;
import com.marketplace.dtos.PageResponse;
import com.marketplace.dtos.ReviewResponse;
import com.marketplace.dtos.UpdateReviewStatusRequest;
import com.marketplace.dtos.UpdateUserStatusRequest;
import com.marketplace.entities.ApplicationStatus;
import com.marketplace.entities.Category;
import com.marketplace.entities.Job;
import com.marketplace.entities.JobStatus;
import com.marketplace.entities.Location;
import com.marketplace.entities.Profile;
import com.marketplace.entities.Review;
import com.marketplace.entities.ReviewStatus;
import com.marketplace.entities.User;
import com.marketplace.entities.UserRole;
import com.marketplace.entities.UserStatus;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.ApplicationRepository;
import com.marketplace.repositories.CategoryRepository;
import com.marketplace.repositories.JobRepository;
import com.marketplace.repositories.LocationRepository;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.ReviewRepository;
import com.marketplace.repositories.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý nghiệp vụ Quản trị hệ thống dành cho vai trò ADMIN
 * (FR-25, FR-26, FR-27, FR-30, FR-31).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AdminService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ReviewRepository reviewRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;

    /**
     * Xem danh sách tài khoản người dùng kèm lọc theo vai trò, trạng thái, từ khóa và phân trang (FR-25).
     *
     * @param role    Lọc theo vai trò (STUDENT / EMPLOYER / ADMIN, tùy chọn)
     * @param status  Lọc theo trạng thái (ACTIVE / LOCKED, tùy chọn)
     * @param keyword Tìm theo email, họ tên hoặc tên công ty (tùy chọn)
     * @param page    Số trang (bắt đầu từ 0)
     * @param size    Số bản ghi mỗi trang
     * @return Trang danh sách người dùng kèm thông tin hồ sơ
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getAdminUsers(
            String role,
            String status,
            String keyword,
            int page,
            int size) {

        UserRole parsedRole = parseOptionalRole(role);
        UserStatus parsedStatus = parseOptionalUserStatus(status);
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim().toLowerCase() : null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (parsedRole != null) {
                predicates.add(cb.equal(root.get("role"), parsedRole));
            }
            if (parsedStatus != null) {
                predicates.add(cb.equal(root.get("status"), parsedStatus));
            }
            if (trimmedKeyword != null && query != null) {
                String pattern = "%" + trimmedKeyword + "%";
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);

                Subquery<Long> profileSubquery = query.subquery(Long.class);
                Root<Profile> profileRoot = profileSubquery.from(Profile.class);
                profileSubquery.select(profileRoot.get("user").get("id"));
                Predicate nameMatch = cb.like(cb.lower(profileRoot.get("fullName")), pattern);
                Predicate companyMatch = cb.like(cb.lower(profileRoot.get("companyName")), pattern);
                profileSubquery.where(cb.or(nameMatch, companyMatch));

                predicates.add(cb.or(emailMatch, root.get("id").in(profileSubquery)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<User> users = userPage.getContent();

        if (users.isEmpty()) {
            return PageResponse.fromPage(userPage, Collections.emptyList());
        }

        Set<Long> userIds = users.stream().map(User::getId).collect(Collectors.toSet());
        Map<Long, Profile> profilesByUserId = profileRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));

        List<AdminUserResponse> content = users.stream()
                .map(u -> mapToAdminUserResponse(u, profilesByUserId.get(u.getId())))
                .collect(Collectors.toList());

        return PageResponse.fromPage(userPage, content);
    }

    /**
     * Khóa (LOCKED) hoặc mở khóa (ACTIVE) tài khoản người dùng (FR-26).
     * Chặn Quản trị viên tự khóa chính tài khoản đang đăng nhập của mình.
     *
     * @param adminId      ID của Admin đang thực hiện thao tác
     * @param targetUserId ID của người dùng cần cập nhật trạng thái
     * @param request      Trạng thái mới (ACTIVE hoặc LOCKED)
     * @return Thông tin tài khoản sau khi cập nhật trạng thái
     * @throws AppException Mã lỗi USER_NOT_FOUND (404), ADMIN_SELF_LOCK (409) hoặc BAD_REQUEST (400)
     */
    @Transactional
    public AdminUserResponse updateUserStatus(Long adminId, Long targetUserId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "Không tìm thấy tài khoản người dùng"));

        UserStatus targetStatus;
        try {
            targetStatus = UserStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException("BAD_REQUEST", "Trạng thái tài khoản chỉ chấp nhận ACTIVE hoặc LOCKED");
        }

        // QT-ADMIN (FR-26): Chặn Quản trị viên tự khóa tài khoản của chính mình
        if (adminId != null && adminId.equals(targetUserId) && targetStatus == UserStatus.LOCKED) {
            throw new AppException("ADMIN_SELF_LOCK", "Quản trị viên không thể tự khóa tài khoản của chính mình");
        }

        user.setStatus(targetStatus);
        User savedUser = userRepository.save(user);
        Profile profile = profileRepository.findByUserId(savedUser.getId()).orElse(null);

        return mapToAdminUserResponse(savedUser, profile);
    }

    /**
     * Xem danh sách toàn bộ tin tuyển dụng ở mọi trạng thái dành cho Quản trị viên (FR-27).
     *
     * @param status     Lọc theo trạng thái tin (OPEN / FILLED / COMPLETED / CLOSED / HIDDEN, tùy chọn)
     * @param categoryId Lọc theo danh mục ngành nghề (tùy chọn)
     * @param keyword    Tìm theo tiêu đề hoặc mô tả tin (tùy chọn)
     * @param page       Số trang (bắt đầu từ 0)
     * @param size       Số bản ghi mỗi trang
     * @return Trang danh sách tin tuyển dụng
     */
    @Transactional(readOnly = true)
    public PageResponse<JobResponse> getAdminJobs(
            String status,
            Long categoryId,
            String keyword,
            int page,
            int size) {

        JobStatus parsedStatus = parseOptionalJobStatus(status);
        String trimmedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim().toLowerCase() : null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (parsedStatus != null) {
                predicates.add(cb.equal(root.get("status"), parsedStatus));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (trimmedKeyword != null) {
                String pattern = "%" + trimmedKeyword + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleLike, descLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Job> jobPage = jobRepository.findAll(spec, pageable);
        List<Job> jobs = jobPage.getContent();

        if (jobs.isEmpty()) {
            return PageResponse.fromPage(jobPage, Collections.emptyList());
        }

        Set<Long> employerIds = jobs.stream()
                .map(j -> j.getEmployer().getId())
                .collect(Collectors.toSet());

        Map<Long, Profile> profilesByUserId = profileRepository.findAllByUserIdIn(employerIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));

        List<JobResponse> content = jobs.stream()
                .map(j -> mapToAdminJobResponse(j, profilesByUserId.get(j.getEmployer().getId())))
                .collect(Collectors.toList());

        return PageResponse.fromPage(jobPage, content);
    }

    /**
     * Ẩn hoặc khôi phục tin tuyển dụng dành cho Quản trị viên theo đúng quy tắc Mục 5 (FR-27):
     * - Admin chỉ ẩn (HIDDEN) được các tin đang ở trạng thái OPEN hoặc FILLED.
     * - Tin ở trạng thái CLOSED và COMPLETED không bị ẩn (trả về 409 INVALID_STATUS_TRANSITION).
     * - Khi khôi phục tin đang HIDDEN (request.status là OPEN, FILLED hoặc RESTORE):
     *   + Tin trở về OPEN nếu số hồ sơ ACCEPTED < slots.
     *   + Ngược lại trở về FILLED nếu số hồ sơ ACCEPTED >= slots.
     *
     * @param jobId   ID tin tuyển dụng
     * @param request Yêu cầu đổi trạng thái (HIDDEN hoặc OPEN/FILLED/RESTORE)
     * @return Thông tin tin tuyển dụng sau khi cập nhật trạng thái
     * @throws AppException Mã lỗi JOB_NOT_FOUND (404), INVALID_STATUS_TRANSITION (409) hoặc BAD_REQUEST (400)
     */
    @Transactional
    public JobResponse updateAdminJobStatus(Long jobId, AdminUpdateJobStatusRequest request) {
        Job job = jobRepository.findWithDetailsById(jobId)
                .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));

        String rawStatus = request.getStatus().trim().toUpperCase();
        JobStatus currentStatus = job.getStatus();

        if ("HIDDEN".equals(rawStatus)) {
            // QT-JOB (FR-27, Mục 5): Admin chỉ ẩn (HIDDEN) tin đang OPEN hoặc FILLED; CLOSED và COMPLETED không bị ẩn
            if (currentStatus != JobStatus.OPEN && currentStatus != JobStatus.FILLED) {
                throw new AppException(
                        "INVALID_STATUS_TRANSITION",
                        "Chỉ được phép ẩn (HIDDEN) tin tuyển dụng đang ở trạng thái OPEN hoặc FILLED (trạng thái hiện tại: "
                                + currentStatus + ")"
                );
            }
            job.setStatus(JobStatus.HIDDEN);
        } else if ("OPEN".equals(rawStatus) || "FILLED".equals(rawStatus) || "RESTORE".equals(rawStatus)) {
            // QT-JOB (FR-27, Mục 5): Khi khôi phục tin đang HIDDEN, trở về OPEN nếu số ACCEPTED < slots, ngược lại trở về FILLED
            if (currentStatus != JobStatus.HIDDEN) {
                throw new AppException(
                        "INVALID_STATUS_TRANSITION",
                        "Chỉ có thể khôi phục tin tuyển dụng đang ở trạng thái HIDDEN (trạng thái hiện tại: "
                                + currentStatus + ")"
                );
            }
            long acceptedCount = applicationRepository.countByJobIdAndStatus(jobId, ApplicationStatus.ACCEPTED);
            JobStatus restoredStatus = (acceptedCount < job.getSlots()) ? JobStatus.OPEN : JobStatus.FILLED;
            job.setStatus(restoredStatus);
        } else {
            throw new AppException("BAD_REQUEST", "Trạng thái cập nhật bởi Admin chỉ chấp nhận HIDDEN, OPEN, FILLED hoặc RESTORE");
        }

        Job savedJob = jobRepository.save(job);
        Profile employerProfile = profileRepository.findByUserId(savedJob.getEmployer().getId()).orElse(null);
        return mapToAdminJobResponse(savedJob, employerProfile);
    }

    /**
     * Xem danh sách toàn bộ đánh giá trên hệ thống kèm lọc theo trạng thái dành cho Quản trị viên (FR-30).
     *
     * @param status Lọc theo trạng thái (VISIBLE / HIDDEN, tùy chọn)
     * @param page   Số trang (bắt đầu từ 0)
     * @param size   Số bản ghi mỗi trang
     * @return Trang danh sách đánh giá
     */
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getAdminReviews(String status, int page, int size) {
        ReviewStatus parsedStatus = parseOptionalReviewStatus(status);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50)
        );

        Page<Review> reviewPage = (parsedStatus != null)
                ? reviewRepository.findByStatusOrderByCreatedAtDesc(parsedStatus, pageable)
                : reviewRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<Review> reviews = reviewPage.getContent();
        if (reviews.isEmpty()) {
            return PageResponse.fromPage(reviewPage, Collections.emptyList());
        }

        Set<Long> userIds = new HashSet<>();
        for (Review r : reviews) {
            userIds.add(r.getReviewer().getId());
            userIds.add(r.getReviewee().getId());
        }

        Map<Long, Profile> profilesByUserId = profileRepository.findAllByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));

        List<ReviewResponse> content = reviews.stream()
                .map(r -> mapToAdminReviewResponse(
                        r,
                        profilesByUserId.get(r.getReviewer().getId()),
                        profilesByUserId.get(r.getReviewee().getId())
                ))
                .collect(Collectors.toList());

        return PageResponse.fromPage(reviewPage, content);
    }

    /**
     * Ẩn (HIDDEN) hoặc hiển thị lại (VISIBLE) một đánh giá không phù hợp dành cho Quản trị viên (FR-30).
     *
     * @param reviewId ID bản ghi đánh giá
     * @param request  Trạng thái mới (VISIBLE hoặc HIDDEN)
     * @return Thông tin bản ghi đánh giá sau khi cập nhật trạng thái
     * @throws AppException Mã lỗi REVIEW_NOT_FOUND (404) hoặc BAD_REQUEST (400)
     */
    @Transactional
    public ReviewResponse updateReviewStatus(Long reviewId, UpdateReviewStatusRequest request) {
        Review review = reviewRepository.findWithDetailsById(reviewId)
                .orElseThrow(() -> new AppException("REVIEW_NOT_FOUND", "Không tìm thấy bản ghi đánh giá"));

        ReviewStatus targetStatus;
        try {
            targetStatus = ReviewStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException("BAD_REQUEST", "Trạng thái đánh giá chỉ chấp nhận VISIBLE hoặc HIDDEN");
        }

        review.setStatus(targetStatus);
        Review saved = reviewRepository.save(review);

        Profile reviewerProfile = profileRepository.findByUserId(saved.getReviewer().getId()).orElse(null);
        Profile revieweeProfile = profileRepository.findByUserId(saved.getReviewee().getId()).orElse(null);
        return mapToAdminReviewResponse(saved, reviewerProfile, revieweeProfile);
    }

    /**
     * Lấy số liệu thống kê tổng quan hệ thống dành cho Quản trị viên (FR-31).
     *
     * @return Số lượng người dùng, tin tuyển dụng, hồ sơ ứng tuyển, đánh giá và danh mục
     */
    @Transactional(readOnly = true)
    public AdminStatsResponse getSystemStats() {
        long totalStudents = userRepository.countByRole(UserRole.STUDENT);
        long totalEmployers = userRepository.countByRole(UserRole.EMPLOYER);
        long totalAdmins = userRepository.countByRole(UserRole.ADMIN);
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long lockedUsers = userRepository.countByStatus(UserStatus.LOCKED);

        long openJobs = jobRepository.countByStatus(JobStatus.OPEN);
        long filledJobs = jobRepository.countByStatus(JobStatus.FILLED);
        long completedJobs = jobRepository.countByStatus(JobStatus.COMPLETED);
        long closedJobs = jobRepository.countByStatus(JobStatus.CLOSED);
        long hiddenJobs = jobRepository.countByStatus(JobStatus.HIDDEN);

        long pendingApplications = applicationRepository.countByStatus(ApplicationStatus.PENDING);
        long acceptedApplications = applicationRepository.countByStatus(ApplicationStatus.ACCEPTED);
        long rejectedApplications = applicationRepository.countByStatus(ApplicationStatus.REJECTED);

        long visibleReviews = reviewRepository.countByStatus(ReviewStatus.VISIBLE);
        long hiddenReviews = reviewRepository.countByStatus(ReviewStatus.HIDDEN);

        return AdminStatsResponse.builder()
                .totalUsers(totalStudents + totalEmployers + totalAdmins)
                .totalStudents(totalStudents)
                .totalEmployers(totalEmployers)
                .totalAdmins(totalAdmins)
                .activeUsers(activeUsers)
                .lockedUsers(lockedUsers)
                .totalJobs(openJobs + filledJobs + completedJobs + closedJobs + hiddenJobs)
                .openJobs(openJobs)
                .filledJobs(filledJobs)
                .completedJobs(completedJobs)
                .closedJobs(closedJobs)
                .hiddenJobs(hiddenJobs)
                .totalApplications(pendingApplications + acceptedApplications + rejectedApplications)
                .pendingApplications(pendingApplications)
                .acceptedApplications(acceptedApplications)
                .rejectedApplications(rejectedApplications)
                .totalReviews(visibleReviews + hiddenReviews)
                .visibleReviews(visibleReviews)
                .hiddenReviews(hiddenReviews)
                .totalCategories(categoryRepository.count())
                .totalLocations(locationRepository.count())
                .build();
    }

    private UserRole parseOptionalRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException("BAD_REQUEST", "Vai trò lọc chỉ chấp nhận STUDENT, EMPLOYER hoặc ADMIN");
        }
    }

    private UserStatus parseOptionalUserStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException("BAD_REQUEST", "Trạng thái tài khoản lọc chỉ chấp nhận ACTIVE hoặc LOCKED");
        }
    }

    private JobStatus parseOptionalJobStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return JobStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException("BAD_REQUEST", "Trạng thái tin tuyển dụng lọc chỉ chấp nhận OPEN, FILLED, COMPLETED, CLOSED hoặc HIDDEN");
        }
    }

    private ReviewStatus parseOptionalReviewStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return ReviewStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AppException("BAD_REQUEST", "Trạng thái đánh giá lọc chỉ chấp nhận VISIBLE hoặc HIDDEN");
        }
    }

    private AdminUserResponse mapToAdminUserResponse(User user, Profile profile) {
        String locationName = null;
        if (profile != null && profile.getLocation() != null) {
            Location loc = profile.getLocation();
            locationName = loc.getDistrict() + ", " + loc.getCity();
        }
        return AdminUserResponse.builder()
                .id(user.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .fullName(profile != null ? profile.getFullName() : user.getEmail())
                .phone(profile != null ? profile.getPhone() : null)
                .companyName(profile != null ? profile.getCompanyName() : null)
                .school(profile != null ? profile.getSchool() : null)
                .major(profile != null ? profile.getMajor() : null)
                .locationName(locationName)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private JobResponse mapToAdminJobResponse(Job job, Profile employerProfile) {
        User employer = job.getEmployer();
        String employerFullName = employerProfile != null ? employerProfile.getFullName() : employer.getEmail();
        String companyName = (employerProfile != null && employerProfile.getCompanyName() != null)
                ? employerProfile.getCompanyName()
                : employerFullName;

        Category category = job.getCategory();
        Location location = job.getLocation();
        String locationName = location.getDistrict() + ", " + location.getCity();

        long applicantCount = applicationRepository.countByJobId(job.getId());
        long acceptedCount = applicationRepository.countByJobIdAndStatus(job.getId(), ApplicationStatus.ACCEPTED);

        return JobResponse.builder()
                .id(job.getId())
                .jobId(job.getId())
                .employerId(employer.getId())
                .employerEmail(employer.getEmail())
                .employerFullName(employerFullName)
                .companyName(companyName)
                .categoryId(category.getId())
                .categoryName(category.getName())
                .locationId(location.getId())
                .locationName(locationName)
                .title(job.getTitle())
                .description(job.getDescription())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryUnit(job.getSalaryUnit().name())
                .workSchedule(job.getWorkSchedule())
                .startDate(job.getStartDate())
                .endDate(job.getEndDate())
                .slots(job.getSlots())
                .applicantCount(applicantCount)
                .acceptedCount(acceptedCount)
                .status(job.getStatus().name())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private ReviewResponse mapToAdminReviewResponse(Review review, Profile reviewerProfile, Profile revieweeProfile) {
        User reviewer = review.getReviewer();
        User reviewee = review.getReviewee();
        String reviewerName = reviewerProfile != null ? reviewerProfile.getFullName() : reviewer.getEmail();
        String revieweeName = revieweeProfile != null ? revieweeProfile.getFullName() : reviewee.getEmail();

        return ReviewResponse.builder()
                .id(review.getId())
                .reviewId(review.getId())
                .jobId(review.getJob().getId())
                .jobTitle(review.getJob().getTitle())
                .reviewerId(reviewer.getId())
                .reviewerName(reviewerName)
                .reviewerRole(reviewer.getRole().name())
                .revieweeId(reviewee.getId())
                .revieweeName(revieweeName)
                .rating(review.getRating())
                .comment(review.getComment())
                .status(review.getStatus().name())
                .createdAt(review.getCreatedAt())
                .build();
    }
}

