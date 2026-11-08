import axios from 'axios';

export const TOKEN_STORAGE_KEY = 'partjob.accessToken';
export const USER_STORAGE_KEY = 'partjob.user';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status ?? 0;
    const data = error.response?.data;
    const authRequest = ['/auth/login', '/auth/register'].some((path) =>
      error.config?.url?.includes(path),
    );

    if (status === 401 && !authRequest) {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
      window.dispatchEvent(new Event('partjob:session-expired'));
    }

    return Promise.reject({
      status,
      message: data?.message || (status === 0
        ? 'Không thể kết nối máy chủ. Hãy kiểm tra backend và thử lại.'
        : 'Yêu cầu không thành công. Vui lòng thử lại.'),
      errors: data?.errors || [],
    });
  },
);

export default axiosClient;
