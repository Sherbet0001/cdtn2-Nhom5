package com.marketplace.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin hồ sơ ứng tuyển cho Sinh viên và Nhà tuyển dụng (FR-12, FR-13, FR-14, FR-19, FR-21, FR-22).
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
    private String jobStatus;
    private Long employerId;
    private String employerName;
    private String companyName;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryUnit;
    private String locationName;
    private Long studentId;
    private String studentFullName;
    private String studentEmail;
    private String studentPhone;
    private String studentSchool;
    private String studentMajor;
    private String studentCvContent;
    private String coverLetter;
    private String status;
    private Boolean hasReviewed;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}
