package com.nhom5.partjob.dto;

import com.nhom5.partjob.domain.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record ApplyRequest(
            @Size(max = 3000, message = "Lời nhắn tối đa 3000 ký tự")
            String coverLetter) {
    }

    public record DecisionRequest(
            @NotNull(message = "Vui lòng chọn quyết định")
            ApplicationStatus status,

            LocalDateTime interviewAt,

            @Size(max = 500, message = "Địa điểm/phương thức phỏng vấn tối đa 500 ký tự")
            String interviewLocation,

            @Size(max = 2000, message = "Lời nhắn tối đa 2000 ký tự")
            String employerMessage) {
    }

    public record StudentInfo(Long id, String fullName, String email, String phone, String school,
                              String major, String bio) {
    }

    public record ApplicationResponse(Long id, Long jobId, String jobTitle, String companyName,
                                      ApplicationStatus status, String cvContent, String coverLetter,
                                      LocalDateTime appliedAt, LocalDateTime interviewAt,
                                      String interviewLocation, String employerMessage, String cvFileName,
                                      StudentInfo student) {
    }

    public record NotificationResponse(Long id, String title, String message, Long applicationId,
                                       Long jobId, String jobTitle, ApplicationStatus status,
                                       LocalDateTime interviewAt, String interviewLocation,
                                       LocalDateTime createdAt, boolean read) {
    }

    public record NotificationListResponse(long unreadCount, java.util.List<NotificationResponse> items) {
    }
}
