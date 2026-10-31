package com.vlu.freelance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class JobCreateRequest {
    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Mô tả công việc không được để trống")
    private String description;

    @NotNull(message = "Mã ngành nghề không được để trống")
    private Long categoryId;

    @NotNull(message = "Mã địa điểm không được để trống")
    private Long locationId;

    @NotNull(message = "Lương tối thiểu không được để trống")
    @DecimalMin(value = "0.0", message = "Lương tối thiểu phải lớn hơn hoặc bằng 0")
    private BigDecimal salaryMin;

    @NotNull(message = "Lương tối đa không được để trống")
    @DecimalMin(value = "0.0", message = "Lương tối đa phải lớn hơn hoặc bằng 0")
    private BigDecimal salaryMax;

    @NotBlank(message = "Đơn vị lương không được để trống (HOUR, DAY, MONTH, TASK)")
    private String salaryUnit;

    @NotBlank(message = "Lịch làm việc không được để trống")
    private String workSchedule;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    @NotNull(message = "Số lượng tuyển không được để trống")
    @Min(value = 1, message = "Số lượng tuyển tối thiểu là 1")
    private Integer slots;

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public void setSalaryMin(BigDecimal salaryMin) { this.salaryMin = salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public void setSalaryMax(BigDecimal salaryMax) { this.salaryMax = salaryMax; }
    public String getSalaryUnit() { return salaryUnit; }
    public void setSalaryUnit(String salaryUnit) { this.salaryUnit = salaryUnit; }
    public String getWorkSchedule() { return workSchedule; }
    public void setWorkSchedule(String workSchedule) { this.workSchedule = workSchedule; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Integer getSlots() { return slots; }
    public void setSlots(Integer slots) { this.slots = slots; }
}
