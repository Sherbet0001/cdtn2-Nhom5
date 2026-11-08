package com.nhom5.partjob.service;

import com.nhom5.partjob.domain.*;
import com.nhom5.partjob.dto.ApplicationDtos.*;
import com.nhom5.partjob.exception.ApiException;
import com.nhom5.partjob.security.AuthUser;
import com.nhom5.partjob.repository.ApplicationNotificationRepository;
import com.nhom5.partjob.repository.JobApplicationRepository;
import com.nhom5.partjob.repository.JobRepository;
import com.nhom5.partjob.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final ApplicationNotificationRepository notificationRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public ApplicationService(JobApplicationRepository applicationRepository,
                              ApplicationNotificationRepository notificationRepository,
                              JobRepository jobRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.notificationRepository = notificationRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ApplicationResponse apply(Long studentId, Long jobId, ApplyRequest request) {
        Job job = jobRepository.findPublicById(jobId, JobStatus.OPEN, java.time.LocalDate.now())
                .orElseThrow(() -> ApiException.notFound("Tin tuyển dụng không tồn tại hoặc đã đóng"));
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản sinh viên"));
        Profile profile = student.getProfile();
        if (profile == null || ((profile.getCvContent() == null || profile.getCvContent().isBlank())
                && profile.getCvFileData() == null)) {
            throw ApiException.badRequest("Vui lòng nhập nội dung CV hoặc tải tệp CV trong hồ sơ trước khi ứng tuyển");
        }
        if (applicationRepository.existsByJobIdAndStudentId(jobId, studentId)) {
            throw ApiException.conflict("Bạn đã ứng tuyển công việc này");
        }

        String coverLetter = blankToNull(request.coverLetter());
        JobApplication application = applicationRepository.save(
                new JobApplication(job, student, blankToNull(profile.getCvContent()), profile.getCvFileName(),
                        profile.getCvFileContentType(), profile.getCvFileData(), coverLetter));
        return toResponse(application, true);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMine(Long studentId) {
        return applicationRepository.findAllByStudentId(studentId).stream()
                .map(application -> toResponse(application, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getForJob(Long employerId, Long jobId) {
        return applicationRepository.findAllByJobAndEmployer(jobId, employerId).stream()
                .map(application -> toResponse(application, true)).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getForEmployer(Long employerId) {
        return applicationRepository.findAllByEmployer(employerId).stream()
                .map(application -> toResponse(application, true)).toList();
    }

    @Transactional(readOnly = true)
    public CvDocument getCvFile(Long requesterId, Role requesterRole, Long applicationId) {
        JobApplication application = applicationRepository.findForCvDownload(applicationId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ ứng tuyển"));
        boolean isOwningEmployer = requesterRole == Role.EMPLOYER
                && application.getJob().getEmployer().getId().equals(requesterId);
        boolean isApplicant = requesterRole == Role.STUDENT
                && application.getStudent().getId().equals(requesterId);
        if (!isOwningEmployer && !isApplicant) {
            throw ApiException.notFound("Không tìm thấy hồ sơ ứng tuyển");
        }
        if (application.getCvFileSnapshot() == null || application.getCvFileName() == null) {
            throw ApiException.notFound("Hồ sơ này không có tệp CV đính kèm");
        }
        return new CvDocument(application.getCvFileName(), application.getCvFileContentType(),
                application.getCvFileSnapshot());
    }

    @Transactional
    public ApplicationResponse decide(Long employerId, Long applicationId, DecisionRequest request) {
        JobApplication application = applicationRepository.findByIdAndEmployerId(applicationId, employerId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ ứng tuyển"));
        if (application.getStatus() != ApplicationStatus.NEW) {
            throw ApiException.conflict("Hồ sơ này đã được xử lý");
        }

        String employerMessage = blankToNull(request.employerMessage());
        if (request.status() == ApplicationStatus.INTERVIEW) {
            if (request.interviewAt() == null || !request.interviewAt().isAfter(LocalDateTime.now())) {
                throw ApiException.badRequest("Thời gian phỏng vấn phải ở tương lai");
            }
            if (request.interviewLocation() == null || request.interviewLocation().isBlank()) {
                throw ApiException.badRequest("Vui lòng nhập địa điểm hoặc hình thức phỏng vấn");
            }
            application.scheduleInterview(request.interviewAt(), request.interviewLocation().trim(),
                    employerMessage);
        } else if (request.status() == ApplicationStatus.REJECTED) {
            application.reject(employerMessage);
        } else {
            throw ApiException.badRequest("Nhà tuyển dụng chỉ có thể mời phỏng vấn hoặc từ chối hồ sơ");
        }

        JobApplication saved = applicationRepository.save(application);
        String title = saved.getStatus() == ApplicationStatus.INTERVIEW
                ? "Bạn được mời phỏng vấn" : "Cập nhật kết quả ứng tuyển";
        String message = saved.getStatus() == ApplicationStatus.INTERVIEW
                ? "Nhà tuyển dụng đã mời bạn phỏng vấn cho vị trí \"" + saved.getJob().getTitle() + "\"."
                : "Nhà tuyển dụng đã cập nhật kết quả hồ sơ cho vị trí \"" + saved.getJob().getTitle() + "\".";
        notificationRepository.save(new ApplicationNotification(saved.getStudent(), saved, title, message));
        return toResponse(saved, true);
    }

    private static ApplicationResponse toResponse(JobApplication application, boolean includeStudent) {
        Job job = application.getJob();
        User employer = job.getEmployer();
        Profile employerProfile = employer.getProfile();
        String companyName = employerProfile == null || employerProfile.getCompanyName() == null
                || employerProfile.getCompanyName().isBlank()
                ? (employerProfile == null ? "Nhà tuyển dụng" : employerProfile.getFullName())
                : employerProfile.getCompanyName();

        User student = application.getStudent();
        Profile studentProfile = student.getProfile();
        StudentInfo studentInfo = includeStudent && studentProfile != null
                ? new StudentInfo(student.getId(), studentProfile.getFullName(), student.getEmail(),
                studentProfile.getPhone(), studentProfile.getSchool(), studentProfile.getMajor(),
                studentProfile.getBio())
                : null;
        return new ApplicationResponse(application.getId(), job.getId(), job.getTitle(), companyName,
                application.getStatus(), application.getCvSnapshot(), application.getCoverLetter(),
                application.getAppliedAt(), application.getInterviewAt(), application.getInterviewLocation(),
                application.getEmployerMessage(), application.getCvFileName(), studentInfo);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
