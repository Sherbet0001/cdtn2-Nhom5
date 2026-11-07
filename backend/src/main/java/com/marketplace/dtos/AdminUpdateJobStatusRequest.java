package com.marketplace.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu ẩn hoặc khôi phục tin tuyển dụng từ Quản trị viên (FR-27).
 * Chấp nhận HIDDEN (ẩn tin) hoặc OPEN / FILLED / RESTORE (khôi phục tin tự động dựa trên acceptedCount < slots).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUpdateJobStatusRequest {

    @NotBlank(message = "Trạng thái tin tuyển dụng không được để trống")
    @Pattern(
            regexp = "^(HIDDEN|OPEN|FILLED|RESTORE)$",
            message = "Trạng thái cập nhật bởi Admin chỉ chấp nhận HIDDEN, OPEN, FILLED hoặc RESTORE"
    )
    private String status;
}

