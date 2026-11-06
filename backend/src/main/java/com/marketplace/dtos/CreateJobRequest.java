package com.marketplace.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO nhận dữ liệu đăng tin tuyển dụng mới (FR-17) và cập nhật tin tuyển dụng (FR-18).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateJobRequest {

    @NotBlank(message = "Tiêu đề công việc không được để trống")
    @Size(max = 200, message = "Tiêu đề công việc không được vượt quá 200 ký tự")
    private String title;

    @NotBlank(message = "Mô tả công việc không được để trống")
    private String description;

    @NotNull(message = "Danh mục ngành nghề không được để trống")
    private Long categoryId;

    @NotNull(message = "Địa điểm làm việc không được để trống")
    private Long locationId;

    @NotNull(message = "Mức lương tối thiểu không được để trống")
    @DecimalMin(value = "0", message = "Mức lương tối thiểu không được nhỏ hơn 0")
    private BigDecimal salaryMin;

    @NotNull(message = "Mức lương tối đa không được để trống")
    @DecimalMin(value = "0", message = "Mức lương tối đa không được nhỏ hơn 0")
    private BigDecimal salaryMax;

    @NotBlank(message = "Đơn vị tính lương không được để trống")
    @Pattern(regexp = "^(HOUR|DAY|MONTH|TASK)$", message = "Đơn vị tính lương chỉ chấp nhận HOUR, DAY, MONTH hoặc TASK")
    private String salaryUnit;

    @NotBlank(message = "Lịch làm việc không được để trống")
    @Size(max = 200, message = "Lịch làm việc không được vượt quá 200 ký tự")
    private String workSchedule;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @NotNull(message = "Số lượng tuyển (slots) không được để trống")
    @Min(value = 1, message = "Số lượng tuyển (slots) phải từ 1 trở lên")
    private Integer slots;
}

