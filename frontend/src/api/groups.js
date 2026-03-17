import api from './axios';

export const groupApi = {
  create: (data) => api.post('/groups', data),
  getAll: () => api.get('/groups'),
  getById: (id) => api.get(`/groups/${id}`),
  update: (id, data) => api.put(`/groups/${id}`, data),
  delete: (id) => api.delete(`/groups/${id}`),
  addMember: (groupId, userId) => api.post(`/groups/${groupId}/members/${userId}`),
  addMemberByName: (groupId, name) => api.post(`/groups/${groupId}/members/by-name`, { name }),
  removeMember: (groupId, userId) => api.delete(`/groups/${groupId}/members/${userId}`),
  getBalances: (id) => api.get(`/groups/${id}/balances`),
  getSimplifiedDebts: (id) => api.get(`/groups/${id}/simplified-debts`),
  getMemberBalances: (id) => api.get(`/groups/${id}/member-balances`),
};
