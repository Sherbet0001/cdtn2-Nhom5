package com.marketplace.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Định dạng JSON phản hồi chuẩn cho toàn bộ REST API (Thành công & Lỗi).
 * Hỗ trợ đầy đủ cả các trường chuẩn { timestamp, status, error, message, path, errors }
 * lẫn { success, errorCode, requestId }.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private Integer status;
    private String error;
    private String errorCode;
    private String message;
    private String path;
    private T data;
    private List<FieldValidationError> errors;
    private String timestamp;
    private String requestId;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldValidationError {
        private String field;
        private String message;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now().toString())
                .requestId(generateRequestId())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String error, String errorCode, String message, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .error(error)
                .errorCode(errorCode)
                .message(message)
                .path(path)
                .timestamp(Instant.now().toString())
                .requestId(generateRequestId())
                .build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(Instant.now().toString())
                .requestId(generateRequestId())
                .build();
    }

    public static <T> ApiResponse<T> validationError(
            int status,
            String error,
            String message,
            String path,
            List<FieldValidationError> errors) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(status)
                .error(error)
                .errorCode("VALIDATION_ERROR")
                .message(message)
                .path(path)
                .errors(errors)
                .timestamp(Instant.now().toString())
                .requestId(generateRequestId())
                .build();
    }

    private static String generateRequestId() {
        return "req-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
