package com.marketplace.services;

import com.marketplace.dtos.NotificationResponse;
import com.marketplace.entities.Job;
import com.marketplace.entities.JobApplication;
import com.marketplace.entities.Notification;
import com.marketplace.entities.NotificationType;
import com.marketplace.entities.User;
import com.marketplace.exceptions.AppException;
import com.marketplace.repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dịch vụ xử lý nghiệp vụ Thông báo trong hệ thống (FR-06, QT-NOTIF).
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Khởi tạo và lưu một bản ghi thông báo mới vào tbl_notifications (QT-NOTIF, FR-06).
     *
     * @param recipient   Người dùng nhận thông báo
     * @param application Hồ sơ ứng tuyển liên quan (có thể null)
     * @param type        Loại thông báo (APPLICATION_NEW, APPLICATION_ACCEPTED, APPLICATION_REJECTED, JOB_COMPLETED)
     * @param message     Nội dung thông báo hiển thị
     * @return Bản ghi Notification đã lưu
     */
    @Transactional
    public Notification createNotification(
            User recipient,
            JobApplication application,
            NotificationType type,
            String message) {
        Notification notification = Notification.builder()
                .user(recipient)
                .application(application)
                .type(type)
                .message(message)
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    /**
     * Tạo thông báo JOB_COMPLETED gửi đến toàn bộ Sinh viên đã có hồ sơ ACCEPTED khi tin tuyển dụng chuyển sang COMPLETED (FR-23, FR-06).
     *
     * @param job                  Tin tuyển dụng vừa hoàn thành
     * @param acceptedApplications Danh sách hồ sơ ứng tuyển ở trạng thái ACCEPTED của tin đó
     */
    @Transactional
    public void notifyAcceptedStudentsOnJobCompleted(Job job, List<JobApplication> acceptedApplications) {
        if (acceptedApplications == null || acceptedApplications.isEmpty()) {
            return;
        }
        // QT-NOTIF (FR-06, FR-23): Khi công việc chuyển sang COMPLETED, gửi thông báo JOB_COMPLETED cho các SV đã ACCEPTED
        String message = "Công việc \"" + job.getTitle() + "\" đã hoàn thành (COMPLETED). Bạn có thể gửi đánh giá cho Nhà tuyển dụng.";
        for (JobApplication app : acceptedApplications) {
            createNotification(app.getStudent(), app, NotificationType.JOB_COMPLETED, message);
        }
    }

    /**
     * Lấy danh sách thông báo của người dùng đang đăng nhập, hỗ trợ lọc chỉ thông báo chưa đọc (FR-06).
     *
     * @param userId     ID người dùng đang đăng nhập
     * @param unreadOnly true nếu chỉ lấy thông báo chưa đọc (is_read = false)
     * @return Danh sách thông báo sắp xếp mới nhất trước
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId, boolean unreadOnly) {
        List<Notification> notifications = unreadOnly
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .map(this::mapToNotificationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Đếm số lượng thông báo chưa đọc của người dùng phục vụ chuông thông báo polling 30 giây (FR-06).
     *
     * @param userId ID người dùng đang đăng nhập
     * @return Bản đồ chứa unreadCount
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCount(Long userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return Map.of("unreadCount", count);
    }

    /**
     * Đánh dấu một thông báo cụ thể là đã đọc (FR-06).
     *
     * @param userId         ID người dùng đang đăng nhập
     * @param notificationId ID thông báo cần đánh dấu đã đọc
     * @return Thông tin thông báo sau khi cập nhật
     * @throws AppException Mã lỗi NOTIFICATION_NOT_FOUND (404) hoặc FORBIDDEN_RESOURCE (403) nếu không phải chủ thông báo
     */
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException("NOTIFICATION_NOT_FOUND", "Không tìm thấy thông báo"));

        // Kiểm tra quyền sở hữu: Người dùng chỉ được đánh dấu đã đọc thông báo của chính mình
        if (!isNotificationOwnedByUser(notification, userId)) {
            throw new AppException("FORBIDDEN_RESOURCE", "Bạn không có quyền truy cập hoặc chỉnh sửa thông báo của người dùng khác");
        }

        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification = notificationRepository.save(notification);
        }

        return mapToNotificationResponse(notification);
    }

    /**
     * Đánh dấu tất cả thông báo chưa đọc của người dùng hiện tại là đã đọc (FR-06).
     *
     * @param userId ID người dùng đang đăng nhập
     * @return Bản đồ chứa số lượng thông báo đã cập nhật (updatedCount) và unreadCount = 0
     */
    @Transactional
    public Map<String, Object> markAllAsRead(Long userId) {
        List<Notification> unreadList = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (Notification notification : unreadList) {
            notification.setIsRead(true);
        }
        if (!unreadList.isEmpty()) {
            notificationRepository.saveAll(unreadList);
        }
        return Map.of(
                "updatedCount", unreadList.size(),
                "unreadCount", 0L
        );
    }

    /**
     * Kiểm tra thông báo có thuộc về người dùng hay không.
     */
    public boolean isNotificationOwnedByUser(Notification notification, Long userId) {
        return notification != null
                && userId != null
                && notification.getUser() != null
                && notification.getUser().getId().equals(userId);
    }

    private NotificationResponse mapToNotificationResponse(Notification notification) {
        JobApplication app = notification.getApplication();
        Long applicationId = app != null ? app.getId() : null;
        Long jobId = (app != null && app.getJob() != null) ? app.getJob().getId() : null;
        String jobTitle = (app != null && app.getJob() != null) ? app.getJob().getTitle() : null;

        return NotificationResponse.builder()
                .id(notification.getId())
                .notificationId(notification.getId())
                .userId(notification.getUser().getId())
                .applicationId(applicationId)
                .jobId(jobId)
                .jobTitle(jobTitle)
                .type(notification.getType().name())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}

