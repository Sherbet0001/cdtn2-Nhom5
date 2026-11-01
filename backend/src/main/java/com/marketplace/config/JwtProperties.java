package com.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình thuộc tính JWT cho ứng dụng, ánh xạ tiền tố "app.jwt" từ application.properties.
 */
@ConfigurationProperties("app.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Khóa bí mật dùng để ký số HMAC-SHA256 cho JWT Token (tối thiểu 32 ký tự).
     */
    private String secret = "vlu-parttime-job-marketplace-secret-key-2026-sprint1-secure-256bit";

    /**
     * Thời hạn hiệu lực của JWT Token tính bằng giây (Mặc định 7200 giây = 2 giờ).
     */
    private long expirationSeconds = 7200;
}

