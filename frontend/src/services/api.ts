import axios from 'axios';
import type { AxiosError } from 'axios';
import toast from 'react-hot-toast';
import type { ApiError } from '@/types';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('access_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    const status = error.response?.status;
    const message = error.response?.data?.detail ?? 'An unexpected error occurred';

    if (status === 401) {
      localStorage.removeItem('access_token');
      window.location.href = '/login';
    } else if (status === 403) {
      toast.error('You do not have permission to perform this action');
    } else if (status && status >= 500) {
      toast.error('Server error — please try again later');
    }

    return Promise.reject({ status, message });
  },
);

export default api;
