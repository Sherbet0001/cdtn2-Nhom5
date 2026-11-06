package com.marketplace.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin hồ sơ ứng tuyển cho Sinh viên và Nhà tuyển dụng (FR-12, FR-19, FR-21, FR-22).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplicationResponse {

    private Long id;
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private Long studentId;
    private String studentFullName;
    private String studentEmail;
    private String studentPhone;
    private String studentSchool;
    private String studentMajor;
    private String studentCvContent;
    private String coverLetter;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}

