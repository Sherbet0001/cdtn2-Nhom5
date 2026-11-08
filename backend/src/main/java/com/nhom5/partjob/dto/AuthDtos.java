package com.nhom5.partjob.dto;

import com.nhom5.partjob.domain.Role;
import com.nhom5.partjob.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** DTO cua module xac thuc (FR-01, 02, 03). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
            String fullName,

            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email sai định dạng")
            @Size(max = 150, message = "Email tối đa 150 ký tự")
            String email,

            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, max = 72, message = "Mật khẩu tối thiểu 8 ký tự")
            String password,

            @NotBlank(message = "Vai trò không được để trống")
            @Pattern(regexp = "STUDENT|EMPLOYER", message = "Vai trò phải là STUDENT hoặc EMPLOYER")
            String role) {
    }

    public record RegisterResponse(Long userId, String email, Role role, UserStatus status) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email sai định dạng")
            @Size(max = 150, message = "Email tối đa 150 ký tự")
            String email,

            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
            String password) {
    }

    public record UserInfo(Long id, String email, Role role, String fullName) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserInfo user) {
    }
}
