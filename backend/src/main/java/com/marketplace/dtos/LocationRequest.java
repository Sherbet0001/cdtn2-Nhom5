package com.marketplace.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu thêm mới hoặc cập nhật danh mục địa điểm làm việc (FR-29).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRequest {

    @NotBlank(message = "Tên Tỉnh/Thành phố không được để trống")
    @Size(min = 2, max = 100, message = "Tên Tỉnh/Thành phố phải từ 2 đến 100 ký tự")
    private String city;

    @NotBlank(message = "Tên Quận/Huyện không được để trống")
    @Size(min = 2, max = 100, message = "Tên Quận/Huyện phải từ 2 đến 100 ký tự")
    private String district;
}

