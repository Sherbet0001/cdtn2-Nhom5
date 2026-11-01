import axiosClient from './axiosClient';

const authService = {
  register(payload) {
    return axiosClient.post('/auth/register', payload);
  },

  login(payload) {
    return axiosClient.post('/auth/login', payload);
  },

  logout() {
    return axiosClient.post('/auth/logout');
  },
};

export default authService;
