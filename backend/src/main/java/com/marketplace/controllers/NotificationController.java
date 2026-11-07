package com.marketplace.controllers;

import com.marketplace.dtos.ApiResponse;
import com.marketplace.dtos.NotificationResponse;
import com.marketplace.security.UserPrincipal;
import com.marketplace.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cung cấp các API quản lý thông báo trong hệ thống (FR-06).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        List<NotificationResponse> notifications = notificationService.getUserNotifications(
                currentUser.getId(),
                unreadOnly
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Long> result = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công", result));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable("id") Long id) {
        NotificationResponse updated = notificationService.markAsRead(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu thông báo là đã đọc", updated));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Map<String, Object> result = notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo là đã đọc", result));
    }
}

