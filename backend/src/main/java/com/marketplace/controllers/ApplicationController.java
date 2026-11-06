package com.marketplace.controllers;

import com.marketplace.dtos.ApiResponse;
import com.marketplace.dtos.ApplicationResponse;
import com.marketplace.dtos.ApplyJobRequest;
import com.marketplace.dtos.UpdateApplicationStatusRequest;
import com.marketplace.security.UserPrincipal;
import com.marketplace.services.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller cung cấp các API Ứng tuyển việc làm cho Sinh viên và Duyệt hồ sơ cho Nhà tuyển dụng
 * (FR-12, FR-19, FR-21, FR-22).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyJob(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("jobId") Long jobId,
            @Valid @RequestBody(required = false) ApplyJobRequest request) {
        ApplicationResponse application = applicationService.applyJob(currentUser.getId(), jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nộp hồ sơ ứng tuyển thành công", application));
    }

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getJobApplications(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("jobId") Long jobId) {
        List<ApplicationResponse> applications = applicationService.getJobApplicationsForEmployer(
                currentUser.getId(),
                jobId
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ứng viên thành công", applications));
    }

    @PatchMapping("/applications/{id}/status")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        ApplicationResponse updated = applicationService.updateApplicationStatus(
                currentUser.getId(),
                id,
                request
        );
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái hồ sơ ứng tuyển thành công", updated));
    }
}

