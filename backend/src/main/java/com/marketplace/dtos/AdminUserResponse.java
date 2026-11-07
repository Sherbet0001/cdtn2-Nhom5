package com.marketplace.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin tài khoản người dùng kèm hồ sơ cơ bản cho trang Quản trị viên (FR-25, FR-26).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserResponse {

    private Long id;
    private Long userId;
    private String email;
    private String role;
    private String status;
    private String fullName;
    private String phone;
    private String companyName;
    private String school;
    private String major;
    private String locationName;
    private LocalDateTime createdAt;
}

