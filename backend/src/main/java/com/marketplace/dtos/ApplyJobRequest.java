package com.marketplace.dtos;

import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO nhận dữ liệu nộp hồ sơ ứng tuyển của Sinh viên (FR-12).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyJobRequest {

    @Size(max = 2000, message = "Thư giới thiệu (cover letter) không được vượt quá 2000 ký tự")
    private String coverLetter;
}

