package com.nhom5.partjob.controller;

import com.nhom5.partjob.dto.ApplicationDtos.NotificationListResponse;
import com.nhom5.partjob.dto.ApplicationDtos.NotificationResponse;
import com.nhom5.partjob.security.AuthUser;
import com.nhom5.partjob.service.NotificationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/mine")
    public NotificationListResponse getMine(@AuthenticationPrincipal AuthUser user) {
        return notificationService.getMine(user.id());
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@AuthenticationPrincipal AuthUser user,
                                         @PathVariable Long notificationId) {
        return notificationService.markRead(user.id(), notificationId);
    }
}
