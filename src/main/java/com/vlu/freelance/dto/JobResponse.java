package com.vlu.freelance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vlu.freelance.entity.Job;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class JobResponse {
    private Long id;
    private Long employerId;
    private String employerName;
    private Long categoryId;
    private String category;
    private Long locationId;
    private String location;
    private String title;
    private String description;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryUnit;
    private String workSchedule;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer slots;
    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public JobResponse() {}

    public static JobResponse fromEntity(Job job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setEmployerId(job.getEmployerId());
        response.setCategoryId(job.getCategoryId());
        response.setLocationId(job.getLocationId());
        response.setTitle(job.getTitle());
        response.setDescription(job.getDescription());
        response.setSalaryMin(job.getSalaryMin());
        response.setSalaryMax(job.getSalaryMax());
        response.setSalaryUnit(job.getSalaryUnit());
        response.setWorkSchedule(job.getWorkSchedule());
        response.setStartDate(job.getStartDate());
        response.setEndDate(job.getEndDate());
        response.setSlots(job.getSlots());
        response.setStatus(job.getStatus());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployerId() { return employerId; }
    public void setEmployerId(Long employerId) { this.employerId = employerId; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

