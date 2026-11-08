package com.nhom5.partjob.service;

import com.nhom5.partjob.domain.ApplicationNotification;
import com.nhom5.partjob.dto.ApplicationDtos.NotificationListResponse;
import com.nhom5.partjob.dto.ApplicationDtos.NotificationResponse;
import com.nhom5.partjob.exception.ApiException;
import com.nhom5.partjob.repository.ApplicationNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final ApplicationNotificationRepository notificationRepository;

    public NotificationService(ApplicationNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getMine(Long userId) {
        List<NotificationResponse> items = notificationRepository.findAllByUserId(userId).stream()
                .map(NotificationService::toResponse).toList();
        return new NotificationListResponse(notificationRepository.countByUserIdAndReadAtIsNull(userId), items);
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        ApplicationNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thông báo"));
        notification.markRead();
        return toResponse(notificationRepository.save(notification));
    }

    private static NotificationResponse toResponse(ApplicationNotification notification) {
        var application = notification.getApplication();
        var job = application.getJob();
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(),
                application.getId(), job.getId(), job.getTitle(), application.getStatus(),
                application.getInterviewAt(), application.getInterviewLocation(), notification.getCreatedAt(),
                notification.isRead());
    }
}
