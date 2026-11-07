package com.vlu.freelance.controller;

import com.vlu.freelance.dto.ApplicationRequest;
import com.vlu.freelance.dto.ApplicationResponse;
import com.vlu.freelance.dto.ApplicationStatusRequest;
import com.vlu.freelance.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Application Module", description = "Quản lý hồ sơ ứng tuyển (FR-12, FR-13, FR-14, FR-19, FR-21, FR-22)")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    private Long getUserId(Principal principal, String headerUserId, Long defaultId) {
        if (principal != null && principal.getName() != null) {
            try {
                return Long.parseLong(principal.getName());
            } catch (NumberFormatException ignored) {}
        }
        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            try {
                return Long.parseLong(headerUserId.trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultId;
    }

    @PostMapping("/jobs/{jobId}/applications")
    @Operation(summary = "Sinh viên ứng tuyển công việc (FR-12)")
    public ResponseEntity<Map<String, Object>> applyJob(
            @PathVariable Long jobId,
            @RequestBody(required = false) ApplicationRequest request,
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Long studentId = getUserId(principal, headerUserId, 5L); // Fallback ID sinh viên mẫu
        String coverLetter = (request != null) ? request.getCoverLetter() : "";
        Map<String, Object> response = applicationService.applyJob(jobId, studentId, coverLetter);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/applications/mine")
    @Operation(summary = "Sinh viên xem lịch sử hồ sơ ứng tuyển của mình (FR-13, FR-14)")
    public ResponseEntity<Page<ApplicationResponse>> getMyApplications(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long studentId = getUserId(principal, headerUserId, 5L);
        Page<ApplicationResponse> response = applicationService.getMyApplications(
                studentId,
                PageRequest.of(page, size, Sort.by("appliedAt").descending())
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/applications/{id}")
    @Operation(summary = "Xem chi tiết một hồ sơ ứng tuyển (FR-13)")
    public ResponseEntity<ApplicationResponse> getApplicationById(
            @PathVariable Long id,
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Long currentUserId = getUserId(principal, headerUserId, 2L);
        ApplicationResponse response = applicationService.getApplicationById(id, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/{jobId}/applications")
    @Operation(summary = "Nhà tuyển dụng xem danh sách sinh viên ứng tuyển tin của mình (FR-19)")
    public ResponseEntity<Page<ApplicationResponse>> getJobApplications(
            @PathVariable Long jobId,
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long employerId = getUserId(principal, headerUserId, 2L);
        Page<ApplicationResponse> response = applicationService.getJobApplications(
                jobId, employerId,
                PageRequest.of(page, size, Sort.by("appliedAt").descending())
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/applications/{id}/status")
    @Operation(summary = "Nhà tuyển dụng duyệt (ACCEPTED) hoặc từ chối (REJECTED) hồ sơ (FR-21, FR-22)")
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusRequest request,
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Long employerId = getUserId(principal, headerUserId, 2L);
        Map<String, Object> response = applicationService.updateApplicationStatus(id, request.getStatus(), employerId);
        return ResponseEntity.ok(response);
    }
}
