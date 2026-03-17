import api from './axios';

export const settlementApi = {
  create: (data) => api.post('/settlements', data),
  getGroupSettlements: (groupId) => api.get(`/settlements/group/${groupId}`),
  getMySettlements: () => api.get('/settlements/me'),
  getSettlementsWithUser: (userId) => api.get(`/settlements/with/${userId}`),
};
