package com.marketplace.controllers;

import com.marketplace.dtos.AdminStatsResponse;
import com.marketplace.dtos.AdminUpdateJobStatusRequest;
import com.marketplace.dtos.AdminUserResponse;
import com.marketplace.dtos.ApiResponse;
import com.marketplace.dtos.CategoryRequest;
import com.marketplace.dtos.CategoryResponse;
import com.marketplace.dtos.JobResponse;
import com.marketplace.dtos.LocationRequest;
import com.marketplace.dtos.LocationResponse;
import com.marketplace.dtos.PageResponse;
import com.marketplace.dtos.ReviewResponse;
import com.marketplace.dtos.UpdateReviewStatusRequest;
import com.marketplace.dtos.UpdateUserStatusRequest;
import com.marketplace.security.UserPrincipal;
import com.marketplace.services.AdminService;
import com.marketplace.services.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller cung cấp các API Quản trị hệ thống dành cho vai trò ADMIN
 * (FR-25, FR-26, FR-27, FR-28, FR-29, FR-30, FR-31).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final CatalogService catalogService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getAdminUsers(
            @RequestParam(name = "role", required = false) String role,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<AdminUserResponse> users = adminService.getAdminUsers(role, status, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài khoản người dùng thành công", users));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        AdminUserResponse updated = adminService.updateUserStatus(currentUser.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái tài khoản người dùng thành công", updated));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<PageResponse<JobResponse>>> getAdminJobs(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<JobResponse> jobs = adminService.getAdminJobs(status, categoryId, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách toàn bộ tin tuyển dụng thành công", jobs));
    }

    @PatchMapping("/jobs/{id}/status")
    public ResponseEntity<ApiResponse<JobResponse>> updateAdminJobStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminUpdateJobStatusRequest request) {
        JobResponse updated = adminService.updateAdminJobStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái hiển thị tin tuyển dụng thành công", updated));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = catalogService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm mới danh mục ngành nghề thành công", created));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse updated = catalogService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục ngành nghề thành công", updated));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable("id") Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục ngành nghề thành công", null));
    }

    @PostMapping("/locations")
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(
            @Valid @RequestBody LocationRequest request) {
        LocationResponse created = catalogService.createLocation(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm mới địa điểm làm việc thành công", created));
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(
            @PathVariable("id") Long id,
            @Valid @RequestBody LocationRequest request) {
        LocationResponse updated = catalogService.updateLocation(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật địa điểm làm việc thành công", updated));
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(@PathVariable("id") Long id) {
        catalogService.deleteLocation(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa địa điểm làm việc thành công", null));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getAdminReviews(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageResponse<ReviewResponse> reviews = adminService.getAdminReviews(status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá hệ thống thành công", reviews));
    }

    @PatchMapping("/reviews/{id}/status")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReviewStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateReviewStatusRequest request) {
        ReviewResponse updated = adminService.updateReviewStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái hiển thị đánh giá thành công", updated));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getSystemStats() {
        AdminStatsResponse stats = adminService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success("Lấy số liệu thống kê tổng quan hệ thống thành công", stats));
    }
}

