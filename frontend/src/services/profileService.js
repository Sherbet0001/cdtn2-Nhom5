import axiosClient from './axiosClient';

const profileService = {
  getMyProfile() {
    return axiosClient.get('/profiles/me');
  },

  updateMyProfile(payload) {
    return axiosClient.put('/profiles/me', payload);
  },

  getLocations() {
    return axiosClient.get('/locations');
  },

  getCategories() {
    return axiosClient.get('/categories');
  },
};

export default profileService;

