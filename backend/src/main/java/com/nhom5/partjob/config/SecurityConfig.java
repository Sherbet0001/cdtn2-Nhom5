package com.nhom5.partjob.config;

import com.nhom5.partjob.repository.UserRepository;
import com.nhom5.partjob.security.JwtAuthFilter;
import com.nhom5.partjob.security.JwtService;
import com.nhom5.partjob.security.RestSecurityHandlers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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

import java.util.Arrays;
import java.util.List;

/**
 * Cau hinh bao mat: JWT stateless, phan quyen theo vai tro (FR-04).
 * Endpoint cong khai: dang ky, dang nhap, GET /categories, GET /locations, Swagger.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtService jwtService,
                                           UserRepository userRepository,
                                           RestSecurityHandlers handlers) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories", "/locations").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/jobs/mine").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/jobs", "/jobs/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/jobs").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PUT, "/jobs/*").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.PATCH, "/jobs/*/status").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.POST, "/jobs/*/applications").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/applications/mine").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/applications/employer/mine").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/jobs/*/applications").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/applications/*").hasAnyRole("STUDENT", "EMPLOYER")
                        .requestMatchers(HttpMethod.PATCH, "/applications/*/status").hasRole("EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/notifications/**").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PATCH, "/notifications/*/read").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/jobs/*/reviews").hasAnyRole("STUDENT", "EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/profiles/me").hasAnyRole("STUDENT", "EMPLOYER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/profiles/me/cv-file").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/profiles/me/cv-file").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.PUT, "/profiles/me").hasAnyRole("STUDENT", "EMPLOYER")
                        .requestMatchers(HttpMethod.GET, "/profiles/*").hasAnyRole("EMPLOYER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/applications/*/cv-file").hasAnyRole("STUDENT", "EMPLOYER")
                        .requestMatchers("/notifications/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
