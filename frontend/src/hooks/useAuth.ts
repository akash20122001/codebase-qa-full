import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { authApi } from '../api/auth.api';

export function useAuth() {
  const { user, isAuthenticated, setAuth, logout, loadFromStorage } = useAuthStore();

  useEffect(() => {
    loadFromStorage();
  }, [loadFromStorage]);

  return { user, isAuthenticated, setAuth, logout };
}

/**
 * Hook for the OAuth callback page.
 * Backend redirects here with token and user data in URL params.
 */
export function useOAuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();

  useEffect(() => {
    const token = searchParams.get('token');
    const error = searchParams.get('error');

    if (error) {
      console.error('[OAuth Callback] Error:', error);
      navigate('/login?error=auth_failed');
      return;
    }

    if (!token) {
      console.warn('[OAuth Callback] No token found in URL');
      navigate('/login');
      return;
    }

    console.log('[OAuth Callback] Token received, fetching user info...');

    // Fetch user info with the token
    const fetchUser = async () => {
      try {
        // Temporarily set token for the API call
        localStorage.setItem('token', token);
        
        const user = await authApi.getMe();
        console.log('[OAuth Callback] User fetched successfully:', user);
        
        setAuth(token, user);
        navigate('/');
      } catch (err) {
        console.error('[OAuth Callback] Failed to fetch user:', err);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/login?error=auth_failed');
      }
    };

    fetchUser();
  }, [searchParams, navigate, setAuth]);
}
