import { create } from 'zustand';

export const useAuthStore = create((set, get) => ({
  token: localStorage.getItem('jwt_token') || null,
  user: JSON.parse(localStorage.getItem('user_data')) || null,

  setAuth: (token, user) => {
    localStorage.setItem('jwt_token', token);
    localStorage.setItem('user_data', JSON.stringify(user));
    set({ token, user });
  },

  clearAuth: () => {
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user_data');
    set({ token: null, user: null });
  },

  isAuthenticated: () => !!get().token,
  isAdmin: () => get().user?.role === 'ADMIN',
}));
