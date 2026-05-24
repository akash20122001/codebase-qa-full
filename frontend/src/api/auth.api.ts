import apiClient from './client';
import type { User } from '../types';

export const authApi = {
  // Redirect to backend OAuth endpoint
  // Backend will handle GitHub OAuth and redirect back to frontend with token
  getGithubAuthUrl: () => {
    const backendUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
    const frontendCallback = `${window.location.origin}/auth/callback`;
    return `${backendUrl}/api/auth/github?redirect_uri=${encodeURIComponent(frontendCallback)}`;
  },

  getMe: async (): Promise<User> => {
    try {
      const { data } = await apiClient.get('/api/auth/me');
      console.log('[Auth API] getMe response:', data);
      
      // Backend returns the user object directly in the response
      // Not wrapped in a "data" property
      return data;
    } catch (error) {
      console.error('[Auth API] getMe failed:', error);
      throw error;
    }
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/api/auth/logout');
  },
};
