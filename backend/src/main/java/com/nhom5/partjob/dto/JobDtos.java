package com.nhom5.partjob.dto;

import com.nhom5.partjob.domain.JobStatus;
import com.nhom5.partjob.domain.JobType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class JobDtos {

    private JobDtos() {
    }

    public record JobRequest(
            @NotBlank(message = "Tên vị trí không được để trống")
            @Size(max = 150, message = "Tên vị trí tối đa 150 ký tự")
            String title,

            @NotBlank(message = "Mô tả công việc không được để trống")
            @Size(max = 20000, message = "Mô tả tối đa 20000 ký tự")
            String description,

            @Size(max = 10000, message = "Yêu cầu tối đa 10000 ký tự")
            String requirements,

            @Size(max = 10000, message = "Quyền lợi tối đa 10000 ký tự")
            String benefits,

            @NotNull(message = "Vui lòng chọn hình thức làm việc")
            JobType jobType,

            @DecimalMin(value = "0.0", message = "Mức lương không được âm")
            @Digits(integer = 12, fraction = 2, message = "Mức lương tối đa 12 chữ số nguyên và 2 chữ số thập phân")
            BigDecimal salaryMin,

            @DecimalMin(value = "0.0", message = "Mức lương không được âm")
            @Digits(integer = 12, fraction = 2, message = "Mức lương tối đa 12 chữ số nguyên và 2 chữ số thập phân")
            BigDecimal salaryMax,

            Long locationId,

            LocalDate expiresAt) {
    }

    public record StatusRequest(@NotNull(message = "Vui lòng chọn trạng thái") JobStatus status) {
    }

    public record JobResponse(
            Long id,
            String title,
            String description,
            String requirements,
            String benefits,
            JobType jobType,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            JobStatus status,
            LocalDate expiresAt,
            LocalDateTime postedAt,
            LocalDateTime updatedAt,
            Long employerId,
            String companyName,
            JobDtos.LocationResponse location) {
    }

    public record LocationResponse(Long id, String city, String district) {
    }
}
