package com.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.dtos.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cấu hình bảo mật Spring Security 6 theo mô hình Stateless JWT và phân quyền RBAC 3 vai trò.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeSecurityError(request, response, HttpStatus.UNAUTHORIZED,
                                        "UNAUTHORIZED_ACCESS", "Vui lòng đăng nhập để thực hiện thao tác này"))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeSecurityError(request, response, HttpStatus.FORBIDDEN,
                                        "FORBIDDEN_RESOURCE", "Bạn không có quyền truy cập tài nguyên này"))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/locations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/mine").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs", "/api/v1/jobs/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/jobs/*").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/jobs/*/status").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/applications").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/mine").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/v1/applications/*").hasAnyRole("STUDENT", "EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/jobs/*/applications").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/applications/*/status").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/jobs/*/reviews").hasAnyRole("STUDENT", "EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*/reviews").authenticated()
                        .requestMatchers("/api/v1/notifications", "/api/v1/notifications/**").authenticated()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/employer/**").hasRole("EMPLOYER")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void writeSecurityError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String errorCode,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body = ApiResponse.error(
                status.value(),
                status.getReasonPhrase(),
                errorCode,
                message,
                request.getRequestURI()
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
