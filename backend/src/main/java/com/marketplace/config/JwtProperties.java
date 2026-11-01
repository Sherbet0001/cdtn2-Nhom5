package com.marketplace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cấu hình thuộc tính JWT cho ứng dụng, ánh xạ các thuộc tính "app.jwt.*" từ application.properties.
 */
@Component
public class JwtProperties {

    /**
     * Khóa bí mật dùng để ký số HMAC-SHA256 cho JWT Token (tối thiểu 32 ký tự).
     */
    @Value("${app.jwt.secret:vlu-parttime-job-marketplace-secret-key-2026-sprint1-secure-256bit}")
    private String secret = "vlu-parttime-job-marketplace-secret-key-2026-sprint1-secure-256bit";

    /**
     * Thời hạn hiệu lực của JWT Token tính bằng giây (Mặc định 7200 giây = 2 giờ).
     */
    @Value("${app.jwt.expiration-seconds:7200}")
    private long expirationSeconds = 7200;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public void setExpirationSeconds(long expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }
}
