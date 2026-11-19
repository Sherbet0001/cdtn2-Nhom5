package com.vlu.freelance.controller;

import com.vlu.freelance.dto.AuthResponse;
import com.vlu.freelance.dto.LoginRequest;
import com.vlu.freelance.dto.RegisterRequest;
import com.vlu.freelance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth Module", description = "Xác thực và phân quyền tài khoản (FR-01 đến FR-04)")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống cho Quản trị viên, Sinh viên và Nhà tuyển dụng (FR-02, FR-04)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản Sinh viên (STUDENT) hoặc Nhà tuyển dụng (EMPLOYER) (FR-01)")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản (FR-03)")
    public ResponseEntity<Void> logout() {
        // Với JWT stateless, phía Frontend chỉ cần xóa token khỏi LocalStorage theo đặc tả Buổi 5
        return ResponseEntity.noContent().build();
    }
}
