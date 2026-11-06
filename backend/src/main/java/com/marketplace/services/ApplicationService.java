package com.marketplace.services;

import com.marketplace.dtos.ApplicationResponse;
import com.marketplace.dtos.ApplyJobRequest;
import com.marketplace.dtos.UpdateApplicationStatusRequest;
import com.marketplace.entities.ApplicationStatus;
import com.marketplace.entities.Job;
import com.marketplace.entities.JobApplication;
import com.marketplace.entities.JobStatus;
import com.marketplace.entities.Notification;
import com.marketplace.entities.NotificationType;
import com.marketplace.entities.Profile;
import com.marketplace.entities.User;
import com.marketplace.entities.UserRole;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.ApplicationRepository;
import com.marketplace.repositories.JobRepository;
import com.marketplace.repositories.NotificationRepository;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý nghiệp vụ Ứng tuyển việc làm và Duyệt hồ sơ ứng viên (FR-06, FR-12, FR-19, FR-21, FR-22).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Sinh viên nộp hồ sơ ứng tuyển vào tin tuyển dụng (FR-12) và tự động tạo thông báo APPLICATION_NEW cho NTD (FR-06).
     * Điều kiện ứng tuyển:
     * 1. Tin tuyển dụng phải ở trạng thái OPEN (409 nếu không OPEN).
     * 2. Sinh viên chưa từng ứng tuyển vào tin này (409 nếu nộp trùng).
     * 3. Hồ sơ cá nhân của Sinh viên đã cập nhật nội dung CV (cv_content không rỗng).
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
        Notification notification = Notification.builder()
                .user(job.getEmployer())
                .application(savedApplication)
                .type(NotificationType.APPLICATION_NEW)
                .message("Ứng viên " + studentProfile.getFullName() + " vừa nộp hồ sơ vào tin: " + job.getTitle())
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        return mapToApplicationResponse(savedApplication, studentProfile);
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

        return applicationRepository.findByJobIdWithStudentOrderByAppliedAtDesc(jobId)
                .stream()
                .map(app -> {
                    Profile studentProfile = profileRepository.findByUserId(app.getStudent().getId()).orElse(null);
                    return mapToApplicationResponse(app, studentProfile);
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

        Notification notification = Notification.builder()
                .user(application.getStudent())
                .application(savedApplication)
                .type(notifType)
                .message(notifMessage)
                .isRead(false)
                .build();
        notificationRepository.save(notification);

        Profile studentProfile = profileRepository.findByUserId(application.getStudent().getId()).orElse(null);
        return mapToApplicationResponse(savedApplication, studentProfile);
    }

    /**
     * Kiểm tra tin tuyển dụng đang ở trạng thái mở nhận hồ sơ (OPEN).
     *
     * @param job Thực thể tin tuyển dụng
     * @return true nếu status == OPEN
     */
    public boolean isJobOpen(Job job) {
        return job != null && job.getStatus() == JobStatus.OPEN;
    }

    /**
     * Kiểm tra sinh viên đã từng nộp đơn vào tin tuyển dụng này hay chưa.
     *
     * @param jobId     ID tin tuyển dụng
     * @param studentId ID sinh viên
     * @return true nếu đã tồn tại bản ghi trong tbl_applications
     */
    public boolean hasStudentApplied(Long jobId, Long studentId) {
        return applicationRepository.existsByJobIdAndStudentId(jobId, studentId);
    }

    /**
     * Kiểm tra hồ sơ cá nhân của sinh viên đã có nội dung CV hợp lệ hay chưa.
     *
     * @param profile Hồ sơ cá nhân của sinh viên
     * @return true nếu cvContent khác null và không rỗng
     */
    public boolean hasValidCv(Profile profile) {
        return profile != null && profile.getCvContent() != null && !profile.getCvContent().trim().isEmpty();
    }

    private ApplicationResponse mapToApplicationResponse(JobApplication app, Profile studentProfile) {
        User student = app.getStudent();
        return ApplicationResponse.builder()
                .id(app.getId())
                .applicationId(app.getId())
                .jobId(app.getJob().getId())
                .jobTitle(app.getJob().getTitle())
                .studentId(student.getId())
                .studentFullName(studentProfile != null ? studentProfile.getFullName() : student.getEmail())
                .studentEmail(student.getEmail())
                .studentPhone(studentProfile != null ? studentProfile.getPhone() : null)
                .studentSchool(studentProfile != null ? studentProfile.getSchool() : null)
                .studentMajor(studentProfile != null ? studentProfile.getMajor() : null)
                .studentCvContent(studentProfile != null ? studentProfile.getCvContent() : null)
                .coverLetter(app.getCoverLetter())
                .status(app.getStatus().name())
                .appliedAt(app.getAppliedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }
}

