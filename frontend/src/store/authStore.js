/**
 * WHY Zustand for state management?
 *
 * Zustand is ~1KB, has no boilerplate (unlike Redux), and works with React hooks natively.
 * 
 * Redux: Requires actions, reducers, dispatch, connect/useSelector — lots of ceremony.
 * Context API: Causes re-renders for ALL consumers when ANY value changes.
 * Zustand: Components only re-render when their specific slice of state changes.
 *
 * The store is just a hook. Call useAuthStore() in any component to access/modify state.
 */
import { create } from 'zustand';
import { authApi } from '../api/auth';
import { userApi } from '../api/users';

const useAuthStore = create((set) => ({
  user: null,
  isAuthenticated: false,
  isAuthLoading: true,
  isLoading: false,
  error: null,

  initAuth: async () => {
    const token = sessionStorage.getItem('accessToken');
    if (!token) {
      set({ user: null, isAuthenticated: false, isAuthLoading: false });
      return;
    }

    try {
      const response = await userApi.getMe();
      const user = response.data;
      sessionStorage.setItem('user', JSON.stringify(user));
      set({ user, isAuthenticated: true, isAuthLoading: false });
    } catch {
      sessionStorage.removeItem('accessToken');
      sessionStorage.removeItem('refreshToken');
      sessionStorage.removeItem('user');
      set({ user: null, isAuthenticated: false, isAuthLoading: false });
    }
  },

  signup: async (data) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authApi.signup(data);
      const { accessToken, refreshToken, user } = response.data;
      sessionStorage.setItem('accessToken', accessToken);
      sessionStorage.setItem('refreshToken', refreshToken);
      sessionStorage.setItem('user', JSON.stringify(user));
      set({ user, isAuthenticated: true, isLoading: false });
      return user;
    } catch (error) {
      const message = error.response?.data?.message || 'Signup failed';
      set({ error: message, isLoading: false });
      throw error;
    }
  },

  login: async (data) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authApi.login(data);
      const { accessToken, refreshToken, user } = response.data;
      sessionStorage.setItem('accessToken', accessToken);
      sessionStorage.setItem('refreshToken', refreshToken);
      sessionStorage.setItem('user', JSON.stringify(user));
      set({ user, isAuthenticated: true, isLoading: false });
      return user;
    } catch (error) {
      const message = error.response?.data?.message || 'Login failed';
      set({ error: message, isLoading: false });
      throw error;
    }
  },

  logout: () => {
    sessionStorage.removeItem('accessToken');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('user');
    set({ user: null, isAuthenticated: false });
  },

  updateUser: async (data) => {
    try {
      const response = await userApi.updateProfile(data);
      const user = response.data;
      sessionStorage.setItem('user', JSON.stringify(user));
      set({ user });
      return user;
    } catch (error) {
      throw error;
    }
  },

  clearError: () => set({ error: null }),
}));

export default useAuthStore;
