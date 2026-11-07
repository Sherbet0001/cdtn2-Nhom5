package com.marketplace.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO tổng hợp danh sách đánh giá VISIBLE của một người dùng kèm điểm trung bình (FR-15, FR-24).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReviewsSummaryResponse {

    private Long userId;
    private Double averageRating;
    private Long totalReviews;
    private List<ReviewResponse> reviews;
}

