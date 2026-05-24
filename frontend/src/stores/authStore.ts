import { create } from 'zustand';
import type { User } from '../types';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: User) => void;
  logout: () => void;
  loadFromStorage: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,

  setAuth: (token, user) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    set({ token, user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    set({ token: null, user: null, isAuthenticated: false });
  },

  loadFromStorage: () => {
    try {
      const token = localStorage.getItem('token');
      const userStr = localStorage.getItem('user');
      
      // Check if both token and userStr exist and are valid
      if (token && userStr && userStr !== 'undefined' && userStr !== 'null') {
        const user = JSON.parse(userStr);
        set({ token, user, isAuthenticated: true });
      } else {
        // Clear invalid data
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        set({ token: null, user: null, isAuthenticated: false });
      }
    } catch (error) {
      console.error('[Auth Store] Failed to load from storage:', error);
      // Clear corrupted data
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      set({ token: null, user: null, isAuthenticated: false });
    }
  },
}));
