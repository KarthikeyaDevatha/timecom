import { api } from './api';

export const sessionService = {
  getCurrentSession: async () => {
    const response = await api.get('/sessions/current');
    return response.data;
  },

  getAllUserSessions: async () => {
    const response = await api.get('/sessions');
    return response.data;
  },

  terminateSession: async (sessionId) => {
    await api.delete(`/sessions/${sessionId}`);
  }
};
