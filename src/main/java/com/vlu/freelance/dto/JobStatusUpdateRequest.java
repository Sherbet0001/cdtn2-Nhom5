package com.vlu.freelance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class JobStatusUpdateRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(OPEN|FILLED|COMPLETED|CLOSED)$", message = "Trạng thái chỉ có thể là OPEN, FILLED, COMPLETED hoặc CLOSED")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

