import axiosClient from './axiosClient';

const jobService = {
  getJobs(params = {}) {
    return axiosClient.get('/jobs', { params });
  },

  getJobById(id) {
    return axiosClient.get(`/jobs/${id}`);
  },

  getMyJobs(params = {}) {
    return axiosClient.get('/jobs/mine', { params });
  },

  createJob(payload) {
    return axiosClient.post('/jobs', payload);
  },

  updateJob(id, payload) {
    return axiosClient.put(`/jobs/${id}`, payload);
  },

  applyJob(jobId, payload) {
    return axiosClient.post(`/jobs/${jobId}/applications`, payload);
  },

  getJobApplications(jobId) {
    return axiosClient.get(`/jobs/${jobId}/applications`);
  },

  updateApplicationStatus(applicationId, status) {
    return axiosClient.patch(`/applications/${applicationId}/status`, { status });
  },
};

export default jobService;

