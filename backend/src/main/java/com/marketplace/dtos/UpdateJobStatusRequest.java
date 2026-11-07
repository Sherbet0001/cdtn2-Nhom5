package com.marketplace.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO nhận yêu cầu chuyển đổi trạng thái tin tuyển dụng từ Nhà tuyển dụng (FR-23).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateJobStatusRequest {

    @NotBlank(message = "Trạng thái tin tuyển dụng mới không được để trống")
    @Pattern(
            regexp = "(?i)^(OPEN|FILLED|COMPLETED|CLOSED)$",
            message = "Trạng thái tin tuyển dụng chỉ chấp nhận OPEN, FILLED, COMPLETED hoặc CLOSED"
    )
    private String status;
}

