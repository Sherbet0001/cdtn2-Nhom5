package com.marketplace.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin tin tuyển dụng (FR-07, FR-08, FR-09, FR-10, FR-17, FR-18, FR-20).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobResponse {

    private Long id;
    private Long jobId;
    private Long employerId;
    private String employerEmail;
    private String employerFullName;
    private String companyName;
    private Long categoryId;
    private String categoryName;
    private Long locationId;
    private String locationName;
    private String title;
    private String description;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryUnit;
    private String workSchedule;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer slots;
    private Long applicantCount;
    private Long acceptedCount;
    private Boolean hasApplied;
    private String myApplicationStatus;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

