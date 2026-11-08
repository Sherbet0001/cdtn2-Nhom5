import axiosClient from './axiosClient.js';

const authService = {
  login: (credentials) => axiosClient.post('/auth/login', credentials),
  register: (payload) => axiosClient.post('/auth/register', payload),
  logout: () => axiosClient.post('/auth/logout'),
};

export default authService;
