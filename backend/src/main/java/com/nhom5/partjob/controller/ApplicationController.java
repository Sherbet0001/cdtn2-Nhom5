package com.nhom5.partjob.controller;

import com.nhom5.partjob.dto.ApplicationDtos.*;
import com.nhom5.partjob.security.AuthUser;
import com.nhom5.partjob.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.nio.charset.StandardCharsets;

@RestController
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/jobs/{jobId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId,
                                     @Valid @RequestBody ApplyRequest request) {
        return applicationService.apply(user.id(), jobId, request);
    }

    @GetMapping("/applications/mine")
    public List<ApplicationResponse> getMine(@AuthenticationPrincipal AuthUser user) {
        return applicationService.getMine(user.id());
    }

    @GetMapping("/applications/employer/mine")
    public List<ApplicationResponse> getForEmployer(@AuthenticationPrincipal AuthUser user) {
        return applicationService.getForEmployer(user.id());
    }

    @GetMapping("/applications/{applicationId}/cv-file")
    public ResponseEntity<byte[]> downloadCv(@AuthenticationPrincipal AuthUser user,
                                             @PathVariable Long applicationId) {
        var document = applicationService.getCvFile(user.id(), user.role(), applicationId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(document.contentType()))
                .contentLength(document.content().length)
                .body(document.content());
    }

    @GetMapping("/jobs/{jobId}/applications")
    public List<ApplicationResponse> getForJob(@AuthenticationPrincipal AuthUser user, @PathVariable Long jobId) {
        return applicationService.getForJob(user.id(), jobId);
    }

    @PatchMapping("/applications/{applicationId}/status")
    public ApplicationResponse decide(@AuthenticationPrincipal AuthUser user, @PathVariable Long applicationId,
                                      @Valid @RequestBody DecisionRequest request) {
        return applicationService.decide(user.id(), applicationId, request);
    }
}
