import { api } from './api';

export const adminService = {
  getAnalyticsSummary: async () => {
    const response = await api.get('/admin/analytics/summary');
    return response.data;
  },

  getAllPlatformSessions: async () => {
    const response = await api.get('/admin/sessions');
    return response.data;
  },

  terminateAnySession: async (sessionId) => {
    await api.delete(`/admin/sessions/${sessionId}`);
  }
};
