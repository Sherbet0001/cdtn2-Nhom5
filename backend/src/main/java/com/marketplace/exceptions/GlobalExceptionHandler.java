package com.marketplace.exceptions;

import com.marketplace.dtos.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
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
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex, HttpServletRequest request) {
        HttpStatus status = mapErrorCodeToHttpStatus(ex.getErrorCode());
        ApiResponse<Void> body = ApiResponse.error(
                status.value(),
                status.getReasonPhrase(),
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
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

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiResponse<Void> body = ApiResponse.validationError(
                status.value(),
                status.getReasonPhrase(),
                firstErrorMessage,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadableException(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ApiResponse<Void> body = ApiResponse.error(
                status.value(),
                status.getReasonPhrase(),
                "BAD_REQUEST",
                "Định dạng dữ liệu JSON gửi lên không hợp lệ",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            Exception ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        ApiResponse<Void> body = ApiResponse.error(
                status.value(),
                status.getReasonPhrase(),
                "FORBIDDEN_RESOURCE",
                "Bạn không có quyền truy cập chức năng này",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {
        // Không trả về stack trace hay thông tin nhạy cảm của CSDL ra ngoài Client
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiResponse<Void> body = ApiResponse.error(
                status.value(),
                status.getReasonPhrase(),
                "INTERNAL_SERVER_ERROR",
                "Đã xảy ra lỗi hệ thống ngoài ý muốn, vui lòng thử lại sau",
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
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
