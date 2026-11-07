package com.marketplace.controllers;

import com.marketplace.dtos.ApiResponse;
import com.marketplace.dtos.CreateReviewRequest;
import com.marketplace.dtos.ReviewResponse;
import com.marketplace.dtos.UserReviewsSummaryResponse;
import com.marketplace.security.UserPrincipal;
import com.marketplace.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller cung cấp các API Đánh giá hai chiều sau khi công việc hoàn thành (FR-15, FR-24).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/jobs/{jobId}/reviews")
    @PreAuthorize("hasAnyRole('STUDENT', 'EMPLOYER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("jobId") Long jobId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse review = reviewService.reviewUser(currentUser.getId(), jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi đánh giá thành công", review));
    }

    @GetMapping("/users/{userId}/reviews")
    public ResponseEntity<ApiResponse<UserReviewsSummaryResponse>> getUserReviews(
            @PathVariable("userId") Long userId) {
        UserReviewsSummaryResponse summary = reviewService.getVisibleReviewsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", summary));
    }
}

