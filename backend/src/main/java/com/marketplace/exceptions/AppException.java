package com.marketplace.exceptions;

import lombok.Getter;

/**
 * Ngoại lệ nghiệp vụ chuẩn của hệ thống.
 * Chỉ lưu trữ errorCode và message, tuyệt đối không chứa HttpStatus để đảm bảo tách biệt tầng Service và tầng Web.
 */
@Getter
public class AppException extends RuntimeException {

    private final String errorCode;

    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

