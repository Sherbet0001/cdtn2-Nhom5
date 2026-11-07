package com.marketplace.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin một bản ghi đánh giá hai chiều (FR-15, FR-24).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {

    private Long id;
    private Long reviewId;
    private Long jobId;
    private String jobTitle;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerRole;
    private Long revieweeId;
    private String revieweeName;
    private Integer rating;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
}

