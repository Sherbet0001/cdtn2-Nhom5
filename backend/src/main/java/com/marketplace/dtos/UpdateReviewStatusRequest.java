package com.marketplace.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu ẩn hoặc hiển thị lại đánh giá từ Quản trị viên (FR-30).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReviewStatusRequest {

    @NotBlank(message = "Trạng thái đánh giá không được để trống")
    @Pattern(
            regexp = "^(VISIBLE|HIDDEN)$",
            message = "Trạng thái đánh giá chỉ chấp nhận VISIBLE hoặc HIDDEN"
    )
    private String status;
}

