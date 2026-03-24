import axios from 'axios';
import { useAuthStore } from '../store/useAuthStore';

// Base API instance
export const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach JWT Token
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle 401 Unauthorized globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().clearAuth();
      // Optional: Redirect to login or show toast
      window.location.href = '/login';
    }
    // Extract backend error message if available
    const message = error.response?.data?.message || error.response?.data?.error || 'API Request Failed';
    return Promise.reject(new Error(message));
  }
);
