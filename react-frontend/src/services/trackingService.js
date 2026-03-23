import { api } from './api';

export const trackingService = {
  trackEvent: async (payload) => {
    // payload: { actionType, resourcePath, resourceId, metadata }
    await api.post('/track', payload);
  }
};
