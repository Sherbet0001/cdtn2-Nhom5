package com.vlu.freelance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Trang giao diện tĩnh (index.html)
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/favicon.ico"
                ).permitAll()
                // Mở quyền truy cập tài liệu Swagger UI & API Docs
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml"
                ).permitAll()
                // Danh mục công khai
                .requestMatchers("/api/v1/categories/**", "/api/v1/locations/**").permitAll()
                // Phục vụ chạy thử nghiệm và demo Sprint 2 phân hệ Job & Application
                .requestMatchers("/api/v1/jobs/**", "/api/v1/applications/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}