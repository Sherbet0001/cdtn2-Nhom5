import axiosClient from './axiosClient';

const reviewService = {
  createReview(jobId, payload) {
    return axiosClient.post(`/jobs/${jobId}/reviews`, payload);
  },

  getUserReviews(userId) {
    return axiosClient.get(`/users/${userId}/reviews`);
  },
};

export default reviewService;

