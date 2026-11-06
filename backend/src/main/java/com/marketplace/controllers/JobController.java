package com.marketplace.controllers;

import com.marketplace.dtos.ApiResponse;
import com.marketplace.dtos.CreateJobRequest;
import com.marketplace.dtos.JobResponse;
import com.marketplace.dtos.PageResponse;
import com.marketplace.security.UserPrincipal;
import com.marketplace.services.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * REST Controller cung cấp các API quản lý và tìm kiếm tin tuyển dụng việc làm part-time
 * (FR-07, FR-08, FR-09, FR-10, FR-17, FR-18, FR-20).
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody CreateJobRequest request) {
        JobResponse createdJob = jobService.createJob(currentUser.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng tin tuyển dụng mới thành công", createdJob));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<PageResponse<JobResponse>>> getMyJobs(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<JobResponse> jobs = jobService.getEmployerJobs(currentUser.getId(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tin tuyển dụng của bạn thành công", jobs));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateJobRequest request) {
        JobResponse updatedJob = jobService.updateJob(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tin tuyển dụng thành công", updatedJob));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobResponse>>> searchOpenJobs(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "locationId", required = false) Long locationId,
            @RequestParam(name = "minSalary", required = false) BigDecimal minSalary,
            @RequestParam(name = "salaryUnit", required = false) String salaryUnit,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort) {
        PageResponse<JobResponse> pageResult = jobService.searchOpenJobs(
                keyword,
                categoryId,
                locationId,
                minSalary,
                salaryUnit,
                page,
                size,
                sort
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách việc làm thành công", pageResult));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJobDetail(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        JobResponse jobDetail = jobService.getJobDetail(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết tin tuyển dụng thành công", jobDetail));
    }
}

