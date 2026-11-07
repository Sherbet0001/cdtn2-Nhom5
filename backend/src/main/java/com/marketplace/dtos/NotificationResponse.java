package com.marketplace.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin thông báo trong hệ thống (FR-06).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private Long id;
    private Long notificationId;
    private Long userId;
    private Long applicationId;
    private Long jobId;
    private String jobTitle;
    private String type;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}

