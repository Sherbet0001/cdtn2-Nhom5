package com.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình thuộc tính JWT cho ứng dụng (loại bỏ cảnh báo Unknown property trên IDE).
 */
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    /**
     * Khóa bí mật dùng để ký số HMAC-SHA256 cho JWT Token.
     */
    private String secret = "vlu-parttime-job-marketplace-secret-key-2026-sprint1-secure-256bit";

    /**
     * Thời hạn hiệu lực của JWT Token tính bằng giây (Mặc định 7200 giây = 2 giờ).
     */
    private long expirationSeconds = 7200;
}

