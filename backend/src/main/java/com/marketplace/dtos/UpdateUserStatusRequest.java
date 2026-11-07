package com.marketplace.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu khóa hoặc mở khóa tài khoản người dùng từ Quản trị viên (FR-26).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusRequest {

    @NotBlank(message = "Trạng thái tài khoản không được để trống")
    @Pattern(
            regexp = "^(ACTIVE|LOCKED)$",
            message = "Trạng thái tài khoản chỉ chấp nhận ACTIVE hoặc LOCKED"
    )
    private String status;
}

