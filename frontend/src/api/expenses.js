import api from './axios';

export const expenseApi = {
  create: (data) => api.post('/expenses', data),
  getById: (id) => api.get(`/expenses/${id}`),
  update: (id, data) => api.put(`/expenses/${id}`, data),
  delete: (id) => api.delete(`/expenses/${id}`),
  getGroupExpenses: (groupId) => api.get(`/expenses/group/${groupId}`),
  getMyExpenses: (page = 0, size = 20) => api.get(`/expenses/me?page=${page}&size=${size}`),
  getExpensesWithUser: (userId) => api.get(`/expenses/with/${userId}`),
  addComment: (expenseId, data) => api.post(`/expenses/${expenseId}/comments`, data),
  getComments: (expenseId) => api.get(`/expenses/${expenseId}/comments`),
};
