/**
 * WHY a centralized Axios instance?
 *
 * Every API call needs: base URL, auth headers, error handling.
 * Instead of repeating this in every component, we configure once here.
 *
 * WHY Axios over native fetch()?
 * - Automatic JSON parsing (fetch requires manual response.json())
 * - Request/response interceptors (add auth header globally, handle 401s)
 * - Better error handling (fetch doesn't reject on 4xx/5xx)
 * - Request cancellation is cleaner
 * - Alternative: fetch + wrapper library like ky — works but less ecosystem support
 *
 * INTERCEPTOR PATTERN:
 * Request interceptor: runs BEFORE every request → adds JWT token
 * Response interceptor: runs AFTER every response → handles auth errors globally
 */
import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = sessionStorage.getItem('refreshToken');
        if (refreshToken) {
          const response = await axios.post(`${API_BASE_URL}/auth/refresh`, {
            refreshToken,
          });

          const { accessToken, refreshToken: newRefreshToken } = response.data;
          sessionStorage.setItem('accessToken', accessToken);
          sessionStorage.setItem('refreshToken', newRefreshToken);

          originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshError) {
        sessionStorage.removeItem('accessToken');
        sessionStorage.removeItem('refreshToken');
        sessionStorage.removeItem('user');
        window.dispatchEvent(new CustomEvent('auth-expired'));
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
