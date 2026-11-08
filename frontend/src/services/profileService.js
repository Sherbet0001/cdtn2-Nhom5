import axiosClient from './axiosClient.js';

const profileService = {
  getMine: () => axiosClient.get('/profiles/me'),
  updateMine: (profile) => axiosClient.put('/profiles/me', profile),
  uploadCvFile: (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return axiosClient.put('/profiles/me/cv-file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  downloadCvFile: () => axiosClient.get('/profiles/me/cv-file', { responseType: 'blob' }),
  getLocations: () => axiosClient.get('/locations'),
};

export default profileService;
