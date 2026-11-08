import axiosClient from './axiosClient.js';

const notificationService = {
  getMine: () => axiosClient.get('/notifications/mine'),
  markRead: (notificationId) => axiosClient.patch(`/notifications/${notificationId}/read`),
};

export default notificationService;
