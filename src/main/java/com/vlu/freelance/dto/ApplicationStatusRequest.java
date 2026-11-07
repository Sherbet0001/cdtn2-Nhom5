package com.vlu.freelance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ApplicationStatusRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(ACCEPTED|REJECTED)$", message = "Trạng thái chỉ có thể là ACCEPTED hoặc REJECTED")
    private String status;

    public ApplicationStatusRequest() {}

    public ApplicationStatusRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
