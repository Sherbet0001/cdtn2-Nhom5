package com.nhom5.partjob.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Dinh dang loi thong nhat cho moi endpoint (API contract muc 4.1). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String timestamp,
                            int status,
                            String error,
                            String message,
                            String path,
                            List<FieldErrorItem> errors) {

    public record FieldErrorItem(String field, String message) {
    }

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    public static ErrorResponse of(HttpStatus status, String message, String path, List<FieldErrorItem> errors) {
        return new ErrorResponse(LocalDateTime.now().withNano(0).toString(),
                status.value(), status.name(), message, path, errors);
    }
}
