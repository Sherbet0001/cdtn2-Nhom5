package com.nhom5.partjob.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_jobs", indexes = {
        @Index(name = "idx_jobs_status_posted", columnList = "status, posted_at"),
        @Index(name = "idx_jobs_location", columnList = "location_id")
})
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private User employer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 20)
    private JobType jobType;

    @Column(name = "salary_min", precision = 14, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 14, scale = 2)
    private BigDecimal salaryMax;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.OPEN;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "posted_at", nullable = false, updatable = false)
    private LocalDateTime postedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Job() {
    }

    public Job(User employer, Location location, String title, String description, String requirements,
               String benefits, JobType jobType, BigDecimal salaryMin, BigDecimal salaryMax, LocalDate expiresAt) {
        this.employer = employer;
        this.location = location;
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.benefits = benefits;
        this.jobType = jobType;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        postedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getEmployer() { return employer; }
    public Location getLocation() { return location; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getRequirements() { return requirements; }
    public String getBenefits() { return benefits; }
    public JobType getJobType() { return jobType; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public JobStatus getStatus() { return status; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public LocalDateTime getPostedAt() { return postedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void update(Location location, String title, String description, String requirements, String benefits,
                       JobType jobType, BigDecimal salaryMin, BigDecimal salaryMax, LocalDate expiresAt) {
        this.location = location;
        this.title = title;
        this.description = description;
        this.requirements = requirements;
        this.benefits = benefits;
        this.jobType = jobType;
        this.salaryMin = salaryMin;
        this.salaryMax = salaryMax;
        this.expiresAt = expiresAt;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }
}
