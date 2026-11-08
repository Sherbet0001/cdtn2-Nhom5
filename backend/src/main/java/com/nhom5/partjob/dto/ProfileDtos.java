package com.nhom5.partjob.dto;

import com.nhom5.partjob.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** DTO cua module ho so (FR-05, 11, 16). */
public final class ProfileDtos {

    private ProfileDtos() {
    }

    public record LocationInfo(Long id, String city, String district) {
    }

    public record ProfileResponse(Long id,
                                  Long userId,
                                  String email,
                                  Role role,
                                  String fullName,
                                  String phone,
                                  LocationInfo location,
                                  String bio,
                                  String school,
                                  String major,
                                  String cvContent,
                                  String cvFileName,
                                  String companyName,
                                  LocalDateTime updatedAt) {
    }

    /**
     * PUT /profiles/me thay the cac truong chinh sua duoc cua ho so.
     * school, major, cvContent chi duoc ghi voi STUDENT; companyName chi duoc ghi voi EMPLOYER
     * (truong khong thuoc vai tro se bi bo qua).
     */
    public record ProfileUpdateRequest(
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
            String fullName,

            @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
            @Pattern(regexp = "^[0-9+\\-\\s]*$", message = "Số điện thoại chỉ gồm chữ số, khoảng trắng, + và -")
            String phone,

            Long locationId,

            @Size(max = 2000, message = "Giới thiệu tối đa 2000 ký tự")
            String bio,

            @Size(max = 100, message = "Tên trường tối đa 100 ký tự")
            String school,

            @Size(max = 100, message = "Chuyên ngành tối đa 100 ký tự")
            String major,

            @Size(max = 20000, message = "CV tối đa 20000 ký tự")
            String cvContent,

            @Size(max = 150, message = "Tên công ty tối đa 150 ký tự")
            String companyName) {
    }
}
