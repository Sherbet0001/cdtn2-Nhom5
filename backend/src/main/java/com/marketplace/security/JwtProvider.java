package com.marketplace.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.config.JwtProperties;
import com.marketplace.exceptions.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thành phần cung cấp và xác thực JWT chuẩn RFC 7519 (HMAC-SHA256).
 * QT-AUTH: Thời hạn hiệu lực của JWT Token mặc định là 2 giờ (7200 giây).
 */
@Component
public class JwtProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final byte[] secretBytes;
    private final long expirationSeconds;

    @Autowired
    public JwtProvider(JwtProperties jwtProperties) {
        this(jwtProperties.getSecret(), jwtProperties.getExpirationSeconds());
    }

    public JwtProvider(String secret, long expirationSeconds) {
        if (secret == null || secret.trim().length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("JWT_SECRET phải có độ dài tối thiểu 32 ký tự (256-bit) cho thuật toán HMAC-SHA256");
        }
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    /**
     * Khởi tạo chuỗi JWT Bearer Token chứa thông tin định danh và vai trò người dùng.
     *
     * @param userId ID tài khoản trong bảng tbl_users
     * @param email  Email đăng nhập
     * @param role   Vai trò RBAC (STUDENT, EMPLOYER, ADMIN)
     * @return Chuỗi JWT chuẩn gồm 3 phần Header.Payload.Signature
     */
    public String generateToken(Long userId, String email, String role) {
        try {
            long nowSeconds = Instant.now().getEpochSecond();
            long expSeconds = nowSeconds + expirationSeconds;

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(userId));
            payload.put("userId", userId);
            payload.put("email", email);
            payload.put("role", role);
            payload.put("iat", nowSeconds);
            payload.put("exp", expSeconds);

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsBytes(payload));
            String signingInput = encodedHeader + "." + encodedPayload;
            String signature = signHmacSha256(signingInput);

            return signingInput + "." + signature;
        } catch (Exception ex) {
            throw new AppException("INTERNAL_SERVER_ERROR", "Không thể khởi tạo phiên xác thực JWT");
        }
    }

    /**
     * Kiểm tra chữ ký số và thời hạn của JWT Token, trả về thông tin người dùng nếu hợp lệ.
     *
     * @param token Chuỗi JWT từ Header Authorization
     * @return Đối tượng UserPrincipal chứa thông tin người dùng
     * @throws AppException Mã lỗi INVALID_TOKEN hoặc TOKEN_EXPIRED nếu token không hợp lệ
     */
    public UserPrincipal validateAndExtractPrincipal(String token) {
        if (token == null || token.isBlank()) {
            throw new AppException("INVALID_TOKEN", "Token xác thực không được để trống");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new AppException("INVALID_TOKEN", "Cấu trúc JWT Token không hợp lệ");
        }

        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = signHmacSha256(signingInput);

        // So sánh chữ ký theo thời gian hằng số (constant-time) để chống timing attack
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new AppException("INVALID_TOKEN", "Chữ ký xác thực JWT không hợp lệ");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payloadNode = objectMapper.readTree(payloadBytes);

            long exp = payloadNode.path("exp").asLong(0);
            long nowSeconds = Instant.now().getEpochSecond();
            if (exp <= nowSeconds) {
                throw new AppException("TOKEN_EXPIRED", "Phiên đăng nhập đã hết hạn sau 2 giờ, vui lòng đăng nhập lại");
            }

            long userId = payloadNode.path("userId").asLong(0);
            String email = payloadNode.path("email").asText(null);
            String role = payloadNode.path("role").asText(null);

            if (userId <= 0 || email == null || role == null) {
                throw new AppException("INVALID_TOKEN", "Thông tin định danh trong JWT Token bị thiếu");
            }

            return new UserPrincipal(userId, email, role);
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AppException("INVALID_TOKEN", "Không thể giải mã JWT Token");
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private String signHmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretBytes, HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(rawHmac);
        } catch (Exception ex) {
            throw new AppException("INTERNAL_SERVER_ERROR", "Lỗi ký số HMAC-SHA256 cho JWT");
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

