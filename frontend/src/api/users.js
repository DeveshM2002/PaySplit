import api from './axios';

export const userApi = {
  getMe: () => api.get('/users/me'),
  updateProfile: (data) => api.put('/users/me', data),
  changePassword: (data) => api.put('/users/me/password', data),
  searchUsers: (query) => api.get(`/users/search?query=${query}`),
  getUserById: (id) => api.get(`/users/${id}`),
  getAllUsers: () => api.get('/users'),
};
