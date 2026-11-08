import axiosClient from './axiosClient.js';

const applicationService = {
  apply: (jobId, coverLetter) => axiosClient.post(`/jobs/${jobId}/applications`, { coverLetter }),
  getMine: () => axiosClient.get('/applications/mine'),
  getForEmployer: () => axiosClient.get('/applications/employer/mine'),
  getForJob: (jobId) => axiosClient.get(`/jobs/${jobId}/applications`),
  downloadCvFile: (applicationId) => axiosClient.get(`/applications/${applicationId}/cv-file`, { responseType: 'blob' }),
  decide: (applicationId, decision) => axiosClient.patch(`/applications/${applicationId}/status`, decision),
};

export default applicationService;
