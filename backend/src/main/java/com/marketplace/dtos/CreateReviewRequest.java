package com.marketplace.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO nhận dữ liệu khởi tạo đánh giá hai chiều sau khi công việc hoàn thành (FR-15, FR-24).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {

    @NotNull(message = "ID người được đánh giá (revieweeId) không được để trống")
    private Long revieweeId;

    @NotNull(message = "Điểm đánh giá (rating) không được để trống")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao")
    private Integer rating;

    @Size(max = 1000, message = "Nhận xét đánh giá không được vượt quá 1000 ký tự")
    private String comment;
}

