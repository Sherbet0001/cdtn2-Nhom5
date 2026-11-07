import axiosClient from './axiosClient';

const adminService = {
  getStats() {
    return axiosClient.get('/admin/stats');
  },

  getUsers(params = {}) {
    return axiosClient.get('/admin/users', { params });
  },

  updateUserStatus(userId, status) {
    return axiosClient.patch(`/admin/users/${userId}/status`, { status });
  },

  getJobs(params = {}) {
    return axiosClient.get('/admin/jobs', { params });
  },

  updateJobStatus(jobId, status) {
    return axiosClient.patch(`/admin/jobs/${jobId}/status`, { status });
  },

  getReviews(params = {}) {
    return axiosClient.get('/admin/reviews', { params });
  },

  updateReviewStatus(reviewId, status) {
    return axiosClient.patch(`/admin/reviews/${reviewId}/status`, { status });
  },

  getCategories() {
    return axiosClient.get('/categories');
  },

  createCategory(payload) {
    return axiosClient.post('/admin/categories', payload);
  },

  updateCategory(id, payload) {
    return axiosClient.put(`/admin/categories/${id}`, payload);
  },

  deleteCategory(id) {
    return axiosClient.delete(`/admin/categories/${id}`);
  },

  getLocations() {
    return axiosClient.get('/locations');
  },

  createLocation(payload) {
    return axiosClient.post('/admin/locations', payload);
  },

  updateLocation(id, payload) {
    return axiosClient.put(`/admin/locations/${id}`, payload);
  },

  deleteLocation(id) {
    return axiosClient.delete(`/admin/locations/${id}`);
  },
};

export default adminService;

