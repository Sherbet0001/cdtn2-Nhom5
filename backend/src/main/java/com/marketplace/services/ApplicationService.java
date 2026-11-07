package com.marketplace.services;

import com.marketplace.dtos.ApplicationResponse;
import com.marketplace.dtos.ApplyJobRequest;
import com.marketplace.dtos.UpdateApplicationStatusRequest;
import com.marketplace.entities.ApplicationStatus;
import com.marketplace.entities.Job;
import com.marketplace.entities.JobApplication;
import com.marketplace.entities.JobStatus;
import com.marketplace.entities.Location;
import com.marketplace.entities.NotificationType;
import com.marketplace.entities.Profile;
import com.marketplace.entities.User;
import com.marketplace.entities.UserRole;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.ApplicationRepository;
import com.marketplace.repositories.JobRepository;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.ReviewRepository;
import com.marketplace.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý nghiệp vụ Ứng tuyển việc làm, Theo dõi hồ sơ đã nộp và Duyệt hồ sơ ứng viên
 * (FR-06, FR-12, FR-13, FR-14, FR-19, FR-21, FR-22).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;

    /**
     * Sinh viên nộp hồ sơ ứng tuyển vào tin tuyển dụng (FR-12) và tự động tạo thông báo APPLICATION_NEW cho NTD (FR-06).
     *
     * @param studentId ID Sinh viên đang đăng nhập
     * @param jobId     ID tin tuyển dụng cần ứng tuyển
     * @param request   Nội dung thư giới thiệu (coverLetter)
     * @return Thông tin đơn ứng tuyển vừa khởi tạo với trạng thái PENDING
     * @throws AppException Mã lỗi JOB_NOT_FOUND (404), JOB_NOT_OPEN (409), APPLICATION_CONFLICT (409), hoặc CV_REQUIRED (409)
     */
    @Transactional
    public ApplicationResponse applyJob(Long studentId, Long jobId, ApplyJobRequest request) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", "Không tìm thấy tài khoản sinh viên"));

        if (student.getRole() != UserRole.STUDENT) {
            throw new AppException("FORBIDDEN_RESOURCE", "Chỉ Sinh viên mới được phép nộp hồ sơ ứng tuyển");
        }

        Job job = jobRepository.findWithDetailsById(jobId)
                .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));

        // QT-APP (FR-12): Chỉ cho phép ứng tuyển khi tin đang ở trạng thái OPEN
        if (!isJobOpen(job)) {
            throw new AppException("JOB_NOT_OPEN", "Tin tuyển dụng này hiện không còn mở nhận hồ sơ ứng tuyển");
        }

        // QT-APP (FR-12): Mỗi sinh viên chỉ được ứng tuyển tối đa 1 lần trên mỗi tin tuyển dụng
        if (hasStudentApplied(jobId, studentId)) {
            throw new AppException("APPLICATION_CONFLICT", "Bạn đã ứng tuyển công việc này trước đó");
        }

        // QT-APP (FR-11, FR-12): Bắt buộc hồ sơ cá nhân của sinh viên đã có nội dung CV (cv_content)
        Profile studentProfile = profileRepository.findByUserId(studentId)
                .orElseThrow(() -> new AppException("PROFILE_NOT_FOUND", "Không tìm thấy hồ sơ cá nhân của sinh viên"));

        if (!hasValidCv(studentProfile)) {
            throw new AppException("CV_REQUIRED", "Vui lòng cập nhật nội dung CV trong hồ sơ cá nhân trước khi ứng tuyển");
        }

        String coverLetter = (request != null && request.getCoverLetter() != null && !request.getCoverLetter().trim().isEmpty())
                ? request.getCoverLetter().trim()
                : null;

        JobApplication application = JobApplication.builder()
                .job(job)
                .student(student)
                .coverLetter(coverLetter)
                .status(ApplicationStatus.PENDING)
                .build();

        JobApplication savedApplication = applicationRepository.save(application);

        // QT-NOTIF (FR-06): Tự động ghi thông báo APPLICATION_NEW vào tbl_notifications cho Nhà tuyển dụng chủ tin
        notificationService.createNotification(
                job.getEmployer(),
                savedApplication,
                NotificationType.APPLICATION_NEW,
                "Ứng viên " + studentProfile.getFullName() + " vừa nộp hồ sơ vào tin: " + job.getTitle()
        );

        Profile employerProfile = profileRepository.findByUserId(job.getEmployer().getId()).orElse(null);
        return mapToApplicationResponse(savedApplication, studentProfile, employerProfile, studentId);
    }

    /**
     * Lấy danh sách hồ sơ đã nộp của Sinh viên đang đăng nhập, hỗ trợ lọc theo trạng thái status (FR-13, FR-14).
     *
     * @param studentId   ID Sinh viên đang đăng nhập
     * @param statusParam Trạng thái cần lọc (PENDING, ACCEPTED, REJECTED hoặc null/rỗng để lấy tất cả)
     * @return Danh sách lịch sử hồ sơ ứng tuyển của Sinh viên
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(Long studentId, String statusParam) {
        List<JobApplication> applications;
        if (statusParam != null && !statusParam.trim().isEmpty()) {
            ApplicationStatus statusFilter;
            try {
                statusFilter = ApplicationStatus.valueOf(statusParam.trim().toUpperCase());
            } catch (Exception ex) {
                throw new AppException("BAD_REQUEST", "Trạng thái lọc hồ sơ chỉ chấp nhận PENDING, ACCEPTED hoặc REJECTED");
            }
            applications = applicationRepository.findByStudentIdAndStatusOrderByAppliedAtDesc(studentId, statusFilter);
        } else {
            applications = applicationRepository.findByStudentIdOrderByAppliedAtDesc(studentId);
        }

        Profile studentProfile = profileRepository.findByUserId(studentId).orElse(null);
        return applications.stream()
                .map(app -> {
                    Profile employerProfile = profileRepository.findByUserId(app.getJob().getEmployer().getId()).orElse(null);
                    return mapToApplicationResponse(app, studentProfile, employerProfile, studentId);
                })
                .collect(Collectors.toList());
    }

    /**
     * Xem chi tiết một hồ sơ ứng tuyển và trạng thái hiện tại (FR-13).
     * Chỉ cho phép Sinh viên chủ hồ sơ hoặc Nhà tuyển dụng chủ tin xem.
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param applicationId ID hồ sơ ứng tuyển
     * @return Thông tin chi tiết hồ sơ ứng tuyển
     * @throws AppException Mã lỗi APPLICATION_NOT_FOUND (404) hoặc FORBIDDEN_RESOURCE (403) nếu không có quyền sở hữu
     */
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long currentUserId, Long applicationId) {
        JobApplication application = applicationRepository.findWithDetailsById(applicationId)
                .orElseThrow(() -> new AppException("APPLICATION_NOT_FOUND", "Không tìm thấy hồ sơ ứng tuyển"));

        // QT-APP (FR-13): Kiểm tra quyền sở hữu — chỉ SV chủ hồ sơ hoặc NTD chủ tin mới được xem
        if (!canAccessApplication(application, currentUserId)) {
            throw new AppException(
                    "FORBIDDEN_RESOURCE",
                    "Bạn chỉ được phép xem hồ sơ ứng tuyển của chính mình hoặc hồ sơ nộp vào tin do mình đăng"
            );
        }

        Profile studentProfile = profileRepository.findByUserId(application.getStudent().getId()).orElse(null);
        Profile employerProfile = profileRepository.findByUserId(application.getJob().getEmployer().getId()).orElse(null);
        return mapToApplicationResponse(application, studentProfile, employerProfile, currentUserId);
    }

    /**
     * Lấy danh sách hồ sơ ứng viên đã nộp vào một tin tuyển dụng của Nhà tuyển dụng (FR-19).
     *
     * @param employerId ID Nhà tuyển dụng đang đăng nhập
     * @param jobId      ID tin tuyển dụng
     * @return Danh sách hồ sơ ứng viên kèm thông tin CV
     * @throws AppException Mã lỗi JOB_NOT_FOUND (404) hoặc FORBIDDEN_RESOURCE (403) nếu NTD không phải chủ tin
     */
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getJobApplicationsForEmployer(Long employerId, Long jobId) {
        Job job = jobRepository.findWithDetailsById(jobId)
                .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));

        // QT-APP (FR-19): Kiểm tra quyền sở hữu — NTD chỉ được xem danh sách ứng viên của tin do chính mình đăng
        if (!job.getEmployer().getId().equals(employerId)) {
            throw new AppException("FORBIDDEN_RESOURCE", "Bạn chỉ có quyền xem danh sách ứng viên của tin tuyển dụng do mình sở hữu");
        }

        Profile employerProfile = profileRepository.findByUserId(employerId).orElse(null);
        return applicationRepository.findByJobIdWithStudentOrderByAppliedAtDesc(jobId)
                .stream()
                .map(app -> {
                    Profile studentProfile = profileRepository.findByUserId(app.getStudent().getId()).orElse(null);
                    return mapToApplicationResponse(app, studentProfile, employerProfile, employerId);
                })
                .collect(Collectors.toList());
    }

    /**
     * Nhà tuyển dụng duyệt (ACCEPTED) hoặc từ chối (REJECTED) hồ sơ ứng tuyển (FR-21, FR-22)
     * và tự động tạo thông báo trong tbl_notifications cho Sinh viên (FR-06).
     *
     * @param employerId    ID Nhà tuyển dụng đang đăng nhập
     * @param applicationId ID hồ sơ ứng tuyển
     * @param request       Trạng thái mới (ACCEPTED hoặc REJECTED)
     * @return Thông tin hồ sơ ứng tuyển sau khi cập nhật trạng thái
     * @throws AppException Mã lỗi APPLICATION_NOT_FOUND (404), FORBIDDEN_RESOURCE (403), INVALID_STATUS_TRANSITION (409), hoặc SLOTS_EXCEEDED (409)
     */
    @Transactional
    public ApplicationResponse updateApplicationStatus(
            Long employerId,
            Long applicationId,
            UpdateApplicationStatusRequest request) {

        JobApplication application = applicationRepository.findWithDetailsById(applicationId)
                .orElseThrow(() -> new AppException("APPLICATION_NOT_FOUND", "Không tìm thấy hồ sơ ứng tuyển"));

        Job job = application.getJob();

        // QT-APP (FR-21, FR-22): Kiểm tra quyền sở hữu tin tuyển dụng của NTD
        if (!job.getEmployer().getId().equals(employerId)) {
            throw new AppException("FORBIDDEN_RESOURCE", "Bạn không có quyền duyệt hồ sơ của tin tuyển dụng thuộc nhà tuyển dụng khác");
        }

        // QT-APP: Chỉ cho phép chuyển trạng thái một chiều từ PENDING -> ACCEPTED hoặc REJECTED
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new AppException("INVALID_STATUS_TRANSITION", "Hồ sơ ứng tuyển đã được xử lý trước đó, không thể thay đổi quyết định");
        }

        ApplicationStatus targetStatus = ApplicationStatus.valueOf(request.getStatus().trim().toUpperCase());

        // QT-APP (FR-21): Khóa bi quan bản ghi Job và kiểm tra không cho phép số lượng ACCEPTED vượt quá chỉ tiêu slots
        if (targetStatus == ApplicationStatus.ACCEPTED) {
            Job lockedJob = jobRepository.findByIdForUpdate(job.getId())
                    .orElseThrow(() -> new AppException("JOB_NOT_FOUND", "Không tìm thấy tin tuyển dụng"));
            long currentAccepted = applicationRepository.countByJobIdAndStatus(lockedJob.getId(), ApplicationStatus.ACCEPTED);
            if (currentAccepted >= lockedJob.getSlots()) {
                throw new AppException(
                        "SLOTS_EXCEEDED",
                        "Số lượng ứng viên được chấp nhận đã đạt tối đa chỉ tiêu (" + lockedJob.getSlots() + " slots) của tin tuyển dụng"
                );
            }
        }

        application.setStatus(targetStatus);
        JobApplication savedApplication = applicationRepository.save(application);

        // QT-NOTIF (FR-06): Tạo thông báo APPLICATION_ACCEPTED hoặc APPLICATION_REJECTED cho Sinh viên
        NotificationType notifType = (targetStatus == ApplicationStatus.ACCEPTED)
                ? NotificationType.APPLICATION_ACCEPTED
                : NotificationType.APPLICATION_REJECTED;

        String notifMessage = (targetStatus == ApplicationStatus.ACCEPTED)
                ? "Chúc mừng! Hồ sơ ứng tuyển của bạn vào vị trí \"" + job.getTitle() + "\" đã được Nhà tuyển dụng chấp nhận."
                : "Hồ sơ ứng tuyển của bạn vào vị trí \"" + job.getTitle() + "\" chưa phù hợp trong đợt tuyển này.";

        notificationService.createNotification(application.getStudent(), savedApplication, notifType, notifMessage);

        Profile studentProfile = profileRepository.findByUserId(application.getStudent().getId()).orElse(null);
        Profile employerProfile = profileRepository.findByUserId(employerId).orElse(null);
        return mapToApplicationResponse(savedApplication, studentProfile, employerProfile, employerId);
    }

    /**
     * Kiểm tra tin tuyển dụng đang ở trạng thái mở nhận hồ sơ (OPEN).
     */
    public boolean isJobOpen(Job job) {
        return job != null && job.getStatus() == JobStatus.OPEN;
    }

    /**
     * Kiểm tra sinh viên đã từng nộp đơn vào tin tuyển dụng này hay chưa.
     */
    public boolean hasStudentApplied(Long jobId, Long studentId) {
        return applicationRepository.existsByJobIdAndStudentId(jobId, studentId);
    }

    /**
     * Kiểm tra hồ sơ cá nhân của sinh viên đã có nội dung CV hợp lệ hay chưa.
     */
    public boolean hasValidCv(Profile profile) {
        return profile != null && profile.getCvContent() != null && !profile.getCvContent().trim().isEmpty();
    }

    /**
     * Kiểm tra người dùng hiện tại có quyền xem chi tiết hồ sơ ứng tuyển hay không (SV chủ hồ sơ hoặc NTD chủ tin).
     */
    public boolean canAccessApplication(JobApplication application, Long userId) {
        if (application == null || userId == null) {
            return false;
        }
        boolean isStudentOwner = application.getStudent() != null
                && application.getStudent().getId().equals(userId);
        boolean isEmployerOwner = application.getJob() != null
                && application.getJob().getEmployer() != null
                && application.getJob().getEmployer().getId().equals(userId);
        return isStudentOwner || isEmployerOwner;
    }

    private ApplicationResponse mapToApplicationResponse(
            JobApplication app,
            Profile studentProfile,
            Profile employerProfile,
            Long viewerUserId) {
        User student = app.getStudent();
        Job job = app.getJob();
        User employer = job.getEmployer();
        Location location = job.getLocation();

        String employerName = employerProfile != null ? employerProfile.getFullName() : employer.getEmail();
        String companyName = (employerProfile != null && employerProfile.getCompanyName() != null)
                ? employerProfile.getCompanyName()
                : employerName;
        String locationName = location != null ? (location.getDistrict() + ", " + location.getCity()) : null;

        Boolean hasReviewed = null;
        if (viewerUserId != null) {
            Long revieweeId = viewerUserId.equals(student.getId()) ? employer.getId() : student.getId();
            hasReviewed = reviewRepository.existsByJobIdAndReviewerIdAndRevieweeId(
                    job.getId(),
                    viewerUserId,
                    revieweeId
            );
        }

        return ApplicationResponse.builder()
                .id(app.getId())
                .applicationId(app.getId())
                .jobId(job.getId())
                .jobTitle(job.getTitle())
                .jobStatus(job.getStatus().name())
                .employerId(employer.getId())
                .employerName(employerName)
                .companyName(companyName)
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .salaryUnit(job.getSalaryUnit().name())
                .locationName(locationName)
                .studentId(student.getId())
                .studentFullName(studentProfile != null ? studentProfile.getFullName() : student.getEmail())
                .studentEmail(student.getEmail())
                .studentPhone(studentProfile != null ? studentProfile.getPhone() : null)
                .studentSchool(studentProfile != null ? studentProfile.getSchool() : null)
                .studentMajor(studentProfile != null ? studentProfile.getMajor() : null)
                .studentCvContent(studentProfile != null ? studentProfile.getCvContent() : null)
                .coverLetter(app.getCoverLetter())
                .status(app.getStatus().name())
                .hasReviewed(hasReviewed)
                .appliedAt(app.getAppliedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}
