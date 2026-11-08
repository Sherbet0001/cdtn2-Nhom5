package com.nhom5.partjob.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_applications",
        uniqueConstraints = @UniqueConstraint(name = "uk_application_job_student", columnNames = {"job_id", "student_id"}),
        indexes = @Index(name = "idx_application_job_status", columnList = "job_id, status"))
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "cv_snapshot", columnDefinition = "MEDIUMTEXT")
    private String cvSnapshot;

    @Column(name = "cv_file_name", length = 255)
    private String cvFileName;

    @Column(name = "cv_file_content_type", length = 100)
    private String cvFileContentType;

    @Lob
    @Column(name = "cv_file_snapshot", columnDefinition = "MEDIUMBLOB")
    private byte[] cvFileSnapshot;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.NEW;

    @Column(name = "interview_at")
    private LocalDateTime interviewAt;

    @Column(name = "interview_location", length = 500)
    private String interviewLocation;

    @Column(name = "employer_message", length = 2000)
    private String employerMessage;

    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected JobApplication() {
    }

    public JobApplication(Job job, User student, String cvSnapshot, String cvFileName,
                          String cvFileContentType, byte[] cvFileSnapshot, String coverLetter) {
        this.job = job;
        this.student = student;
        this.cvSnapshot = cvSnapshot;
        this.cvFileName = cvFileName;
        this.cvFileContentType = cvFileContentType;
        this.cvFileSnapshot = cvFileSnapshot == null ? null : cvFileSnapshot.clone();
        this.coverLetter = coverLetter;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        appliedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Job getJob() { return job; }
    public User getStudent() { return student; }
    public String getCvSnapshot() { return cvSnapshot; }
    public String getCvFileName() { return cvFileName; }
    public String getCvFileContentType() { return cvFileContentType; }
    public byte[] getCvFileSnapshot() { return cvFileSnapshot == null ? null : cvFileSnapshot.clone(); }
    public String getCoverLetter() { return coverLetter; }
    public ApplicationStatus getStatus() { return status; }
    public LocalDateTime getInterviewAt() { return interviewAt; }
    public String getInterviewLocation() { return interviewLocation; }
    public String getEmployerMessage() { return employerMessage; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void scheduleInterview(LocalDateTime interviewAt, String interviewLocation, String employerMessage) {
        this.status = ApplicationStatus.INTERVIEW;
        this.interviewAt = interviewAt;
        this.interviewLocation = interviewLocation;
        this.employerMessage = employerMessage;
    }

    public void reject(String employerMessage) {
        this.status = ApplicationStatus.REJECTED;
        this.interviewAt = null;
        this.interviewLocation = null;
        this.employerMessage = employerMessage;
    }
}
