import axios from 'axios';

const TOKEN_STORAGE_KEY = 'vlu_parttime_token';
const USER_STORAGE_KEY = 'vlu_parttime_user';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Request Interceptor: Tự động đính kèm Authorization: Bearer <token>
axiosClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Bắt lỗi tập trung 401 (Unauthorized) và 403 (Forbidden)
axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const status = error.response?.status;
    const responseData = error.response?.data;

    if (status === 401) {
      const isAuthEndpoint =
        error.config?.url?.includes('/auth/login') ||
        error.config?.url?.includes('/auth/register');

      if (!isAuthEndpoint) {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(USER_STORAGE_KEY);
        window.dispatchEvent(
          new CustomEvent('auth:unauthorized', {
            detail: responseData?.message || 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại',
          })
        );
      }
    }

    if (status === 403) {
      window.dispatchEvent(
        new CustomEvent('auth:forbidden', {
          detail: responseData?.message || 'Bạn không có quyền truy cập chức năng này',
        })
      );
    }

    const standardizedError = {
      status: status || 0,
      errorCode: responseData?.errorCode || 'NETWORK_ERROR',
      message:
        responseData?.message ||
        'Không thể kết nối đến máy chủ Backend. Vui lòng kiểm tra kết nối và thử lại.',
      errors: responseData?.errors || [],
      requestId: responseData?.requestId || 'N/A',
      timestamp: responseData?.timestamp || new Date().toISOString(),
    };

    return Promise.reject(standardizedError);
  }
);

export { TOKEN_STORAGE_KEY, USER_STORAGE_KEY };
export default axiosClient;

