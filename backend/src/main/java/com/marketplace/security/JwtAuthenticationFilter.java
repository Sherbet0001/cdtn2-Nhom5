package com.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.dtos.ApiResponse;
import com.marketplace.entities.User;
import com.marketplace.entities.UserStatus;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Bộ lọc xác thực JWT Bearer Token cho mọi HTTP Request.
 * QT-AUTH (FR-02, FR-04, FR-26): Kiểm tra tính hợp lệ của JWT, trạng thái khóa tài khoản và gán quyền RBAC.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @SuppressWarnings("null")
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        try {
            UserPrincipal principal = jwtProvider.validateAndExtractPrincipal(token);

            // QT-AUTH (FR-26): Kiểm tra tài khoản còn tồn tại và không bị khóa (LOCKED)
            User user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new AppException("UNAUTHORIZED_ACCESS", "Tài khoản người dùng không tồn tại"));

            if (user.getStatus() == UserStatus.LOCKED) {
                writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_LOCKED",
                        "Tài khoản của bạn đã bị khóa bởi Quản trị viên");
                return;
            }

            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
            UserPrincipal verifiedPrincipal = new UserPrincipal(user.getId(), user.getEmail(), user.getRole().name());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    verifiedPrincipal,
                    null,
                    Collections.singletonList(authority)
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (AppException ex) {
            SecurityContextHolder.clearContext();
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, ex.getErrorCode(), ex.getMessage());
        }
    }

    private void writeJsonError(HttpServletResponse response, int status, String errorCode, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> errorBody = ApiResponse.error(errorCode, message);
        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}

