package com.marketplace.exceptions;

import com.marketplace.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Bộ xử lý lỗi tập trung duy nhất của toàn hệ thống.
 * Chịu trách nhiệm ánh xạ errorCode từ AppException sang HTTP Status tương ứng và đóng gói vào ApiResponse.
 */
@RestControllerAdvice
@SuppressWarnings("null")
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        HttpStatus status = mapErrorCodeToHttpStatus(ex.getErrorCode());
        ApiResponse<Void> body = ApiResponse.error(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiResponse.FieldValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> ApiResponse.FieldValidationError.builder()
                        .field(err.getField())
                        .message(err.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        String firstErrorMessage = fieldErrors.isEmpty()
                ? "Dữ liệu đầu vào không hợp lệ"
                : fieldErrors.get(0).getMessage();

        ApiResponse<Void> body = ApiResponse.validationError(firstErrorMessage, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadableException(HttpMessageNotReadableException ex) {
        ApiResponse<Void> body = ApiResponse.error("BAD_REQUEST", "Định dạng dữ liệu JSON gửi lên không hợp lệ");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        ApiResponse<Void> body = ApiResponse.error("FORBIDDEN_RESOURCE", "Bạn không có quyền truy cập chức năng này");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        // Không trả về stack trace hay thông tin nhạy cảm của CSDL ra ngoài Client
        ApiResponse<Void> body = ApiResponse.error("INTERNAL_SERVER_ERROR", "Đã xảy ra lỗi hệ thống ngoài ý muốn, vui lòng thử lại sau");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private HttpStatus mapErrorCodeToHttpStatus(String errorCode) {
        if (errorCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return switch (errorCode) {
            case "BAD_REQUEST", "INVALID_ROLE", "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
            case "UNAUTHORIZED_ACCESS", "INVALID_CREDENTIALS", "TOKEN_EXPIRED", "INVALID_TOKEN" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN_RESOURCE", "ACCOUNT_LOCKED", "REVIEW_NOT_ALLOWED" -> HttpStatus.FORBIDDEN;
            case "USER_NOT_FOUND", "PROFILE_NOT_FOUND", "JOB_NOT_FOUND", "LOCATION_NOT_FOUND", "CATEGORY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "EMAIL_ALREADY_EXISTS", "APPLICATION_CONFLICT", "INVALID_STATUS_TRANSITION" -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}

