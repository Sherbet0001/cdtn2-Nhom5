package com.nhom5.partjob.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Ho so ca nhan va CV (tbl_profiles) - FR-05, 11, 16. Quan he 1-1 voi User. */
@Entity
@Table(name = "tbl_profiles")
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "school", length = 100)
    private String school;

    @Column(name = "major", length = 100)
    private String major;

    @Column(name = "cv_content", columnDefinition = "TEXT")
    private String cvContent;

    @Column(name = "cv_file_name", length = 255)
    private String cvFileName;

    @Column(name = "cv_file_content_type", length = 100)
    private String cvFileContentType;

    @Lob
    @Column(name = "cv_file_data", columnDefinition = "MEDIUMBLOB")
    private byte[] cvFileData;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Profile() {
    }

    public Profile(User user, String fullName) {
        this.user = user;
        this.fullName = fullName;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getCvContent() { return cvContent; }
    public void setCvContent(String cvContent) { this.cvContent = cvContent; }
    public String getCvFileName() { return cvFileName; }
    public String getCvFileContentType() { return cvFileContentType; }
    public byte[] getCvFileData() { return cvFileData; }
    public void setCvFile(String fileName, String contentType, byte[] data) {
        this.cvFileName = fileName;
        this.cvFileContentType = contentType;
        this.cvFileData = data;
    }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
