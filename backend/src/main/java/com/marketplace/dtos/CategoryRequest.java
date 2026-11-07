package com.marketplace.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO yêu cầu thêm mới hoặc cập nhật danh mục ngành nghề (FR-28).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục ngành nghề không được để trống")
    @Size(min = 2, max = 100, message = "Tên danh mục ngành nghề phải từ 2 đến 100 ký tự")
    private String name;
}

