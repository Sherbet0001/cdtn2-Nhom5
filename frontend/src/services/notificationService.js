import axiosClient from './axiosClient';

const notificationService = {
  getNotifications(unreadOnly = false) {
    return axiosClient.get('/notifications', {
      params: { unreadOnly },
    });
  },

  getUnreadCount() {
    return axiosClient.get('/notifications/unread-count');
  },

  markAsRead(id) {
    return axiosClient.patch(`/notifications/${id}/read`);
  },

  markAllAsRead() {
    return axiosClient.patch('/notifications/read-all');
  },
};

export default notificationService;

