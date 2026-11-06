package com.marketplace.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * DTO nhận trạng thái duyệt/từ chối hồ sơ ứng tuyển từ Nhà tuyển dụng (FR-21, FR-22).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateApplicationStatusRequest {

    @NotBlank(message = "Trạng thái duyệt hồ sơ không được để trống")
    @Pattern(regexp = "^(ACCEPTED|REJECTED)$", message = "Trạng thái duyệt hồ sơ chỉ chấp nhận ACCEPTED hoặc REJECTED")
    private String status;
}

