package com.marketplace.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO phản hồi số liệu thống kê tổng quan hệ thống dành cho Quản trị viên (FR-31).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsResponse {

    private long totalUsers;
    private long totalStudents;
    private long totalEmployers;
    private long totalAdmins;
    private long activeUsers;
    private long lockedUsers;

    private long totalJobs;
    private long openJobs;
    private long filledJobs;
    private long completedJobs;
    private long closedJobs;
    private long hiddenJobs;

    private long totalApplications;
    private long pendingApplications;
    private long acceptedApplications;
    private long rejectedApplications;

    private long totalReviews;
    private long visibleReviews;
    private long hiddenReviews;

    private long totalCategories;
    private long totalLocations;
}

