package com.marketplace.controllers;

import com.marketplace.dtos.ApiResponse;
import com.marketplace.dtos.ProfileResponse;
import com.marketplace.dtos.UpdateProfileRequest;
import com.marketplace.security.UserPrincipal;
import com.marketplace.services.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller quản lý Hồ sơ cá nhân của người dùng đang đăng nhập (FR-05, FR-11, FR-16).
 */
@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        ProfileResponse profile = profileService.getMyProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ cá nhân thành công", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse updatedProfile = profileService.updateMyProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ cá nhân thành công", updatedProfile));
    }
}

