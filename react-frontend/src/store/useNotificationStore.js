import { create } from 'zustand';

// Simple global toast notification store
export const useNotificationStore = create((set) => ({
  toast: null, // { message, type: 'success' | 'error' | 'info' }
  
  showToast: (message, type = 'success') => {
    set({ toast: { message, type, id: Date.now() } });
    
    // Auto clear after 5s
    setTimeout(() => {
      set((state) => {
        // Only clear if it's the same toast
        if (state.toast?.id === Date.now()) {
            return { toast: null };
        }
        return {};
      });
    }, 5000);
  },
  
  clearToast: () => set({ toast: null })
}));
