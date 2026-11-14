package com.vlu.freelance.controller;

import com.vlu.freelance.dto.JobCreateRequest;
import com.vlu.freelance.dto.JobResponse;
import com.vlu.freelance.dto.JobStatusUpdateRequest;
import com.vlu.freelance.dto.JobUpdateRequest;
import com.vlu.freelance.service.JobService;
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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Job Module", description = "Quản lý tin tuyển dụng (FR-07 đến FR-10, FR-17, FR-18, FR-23)")
public class JobController {

    @Autowired
    private JobService jobService;

    private Long getUserId(Principal principal, String headerUserId) {
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
        // Fallback default employer ID cho mục đích chạy thử & demo tại lớp
        return 2L;
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm & lọc danh sách tin tuyển dụng (FR-07, 08, 09)")
    public ResponseEntity<Page<JobResponse>> getJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) String salaryUnit,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<JobResponse> jobs = jobService.getJobs(
                keyword, categoryId, locationId, minSalary, salaryUnit,
                PageRequest.of(page, size, Sort.by("created_at").descending())
        );
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết một tin tuyển dụng (FR-10)")
    public ResponseEntity<JobResponse> getJobById(@PathVariable Long id) {
        JobResponse job = jobService.getJobById(id);
        return ResponseEntity.ok(job);
    }

    @PostMapping
    @Operation(summary = "Đăng tin tuyển dụng mới (FR-17) - Vai trò: NTD")
    public ResponseEntity<Map<String, Object>> createJob(
            @Valid @RequestBody JobCreateRequest request,
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Long employerId = getUserId(principal, headerUserId);
        Map<String, Object> response = jobService.createJob(request, employerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mine")
    @Operation(summary = "Danh sách tin tuyển dụng của NTD (FR-18) - Vai trò: NTD")
    public ResponseEntity<Page<JobResponse>> getMyJobs(
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long employerId = getUserId(principal, headerUserId);
        Page<JobResponse> jobs = jobService.getMyJobs(
                employerId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(jobs);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Chỉnh sửa nội dung tin tuyển dụng (FR-18) - Vai trò: NTD chủ tin")
    public ResponseEntity<JobResponse> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request,
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Long employerId = getUserId(principal, headerUserId);
        JobResponse response = jobService.updateJob(id, request, employerId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Đổi trạng thái tin tuyển dụng (FR-23) - Vai trò: NTD chủ tin")
    public ResponseEntity<Map<String, Object>> updateJobStatus(
            @PathVariable Long id,
            @Valid @RequestBody JobStatusUpdateRequest request,
            Principal principal,
            @RequestHeader(value = "X-User-Id", required = false) String headerUserId
    ) {
        Long employerId = getUserId(principal, headerUserId);
        Map<String, Object> response = jobService.updateJobStatus(id, request.getStatus(), employerId);
        return ResponseEntity.ok(response);
    }
}
