import api from './axios';

export const dashboardApi = {
  getDashboard: () => api.get('/dashboard'),
  getBalances: () => api.get('/dashboard/balances'),
  getActivity: (page = 0, size = 20) => api.get(`/dashboard/activity?page=${page}&size=${size}`),
  getGroupActivity: (groupId) => api.get(`/dashboard/groups/${groupId}/activity`),
};
