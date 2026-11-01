package com.marketplace.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    private String fullName;

    @Pattern(regexp = "^(|0[0-9]{9,10})$", message = "Số điện thoại phải bắt đầu bằng số 0 và gồm 10-11 chữ số")
    private String phone;

    private Long locationId;

    @Size(max = 2000, message = "Giới thiệu bản thân / đơn vị không được vượt quá 2000 ký tự")
    private String bio;

    @Size(max = 100, message = "Tên trường học không được vượt quá 100 ký tự")
    private String school;

    @Size(max = 100, message = "Chuyên ngành không được vượt quá 100 ký tự")
    private String major;

    @Size(max = 5000, message = "Nội dung CV không được vượt quá 5000 ký tự")
    private String cvContent;

    @Size(max = 150, message = "Tên doanh nghiệp / cửa hàng không được vượt quá 150 ký tự")
    private String companyName;
}

