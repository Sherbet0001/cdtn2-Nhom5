package com.nhom5.partjob.controller;

import com.nhom5.partjob.dto.ProfileDtos.*;
import com.nhom5.partjob.security.AuthUser;
import com.nhom5.partjob.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/profiles")
@Tag(name = "Hồ sơ")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @Operation(summary = "Xem hồ sơ của chính mình (FR-05, FR-16)")
    @GetMapping("/me")
    public ProfileResponse getMine(@AuthenticationPrincipal AuthUser user) {
        return profileService.getMine(user.id());
    }

    @Operation(summary = "Cập nhật hồ sơ, CV và giới thiệu bản thân (FR-05, FR-11, FR-16)")
    @PutMapping("/me")
    public ProfileResponse updateMine(@AuthenticationPrincipal AuthUser user,
                                      @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateMine(user.id(), request);
    }

    @Operation(summary = "Tải tệp CV PDF/DOC/DOCX lên hồ sơ sinh viên")
    @PutMapping(value = "/me/cv-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse uploadCvFile(@AuthenticationPrincipal AuthUser user,
                                        @RequestParam("file") MultipartFile file) {
        return profileService.uploadCvFile(user.id(), file);
    }

    @Operation(summary = "Tải tệp CV của chính sinh viên")
    @GetMapping("/me/cv-file")
    public ResponseEntity<byte[]> downloadMyCv(@AuthenticationPrincipal AuthUser user) {
        var document = profileService.getCvFile(user.id());
        return fileResponse(document.fileName(), document.contentType(), document.content());
    }

    private static ResponseEntity<byte[]> fileResponse(String fileName, String contentType, byte[] content) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(content.length)
                .body(content);
    }
}
