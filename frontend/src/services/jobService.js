import axiosClient from './axiosClient.js';

const jobService = {
  search: (params) => axiosClient.get('/jobs', { params }),
  get: (jobId) => axiosClient.get(`/jobs/${jobId}`),
  getMine: () => axiosClient.get('/jobs/mine'),
  create: (job) => axiosClient.post('/jobs', job),
  update: (jobId, job) => axiosClient.put(`/jobs/${jobId}`, job),
  updateStatus: (jobId, status) => axiosClient.patch(`/jobs/${jobId}/status`, { status }),
};

export default jobService;
