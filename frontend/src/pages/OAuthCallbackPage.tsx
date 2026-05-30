import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';
import axios from 'axios';

/**
 * OAuth Callback Page - Handles GitHub OAuth redirect
 * Shows loading state while processing authentication
 */
export function OAuthCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const setAuth = useAuthStore((s) => s.setAuth);

  useEffect(() => {
    const handleCallback = async () => {
      const token = searchParams.get('token');
      const error = searchParams.get('error');

      if (error) {
        console.error('OAuth error:', error);
        const message = searchParams.get('message') || 'Authentication failed';
        console.error('Error message:', message);
        navigate('/login?error=oauth_failed');
        return;
      }

      if (!token) {
        console.error('No token received in callback');
        navigate('/login?error=no_token');
        return;
      }

      try {
        // Fetch user info with the token
        const apiUrl = import.meta.env.VITE_API_URL || '';
        const response = await axios.get(`${apiUrl}/api/auth/me`, {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Accept': 'application/json',
          },
        });
        
        const user = response.data;
        setAuth(token, user);
        navigate('/');
      } catch (err: any) {
        console.error('Failed to fetch user info:', err);
        const errorMessage = err.response?.data?.message || 'Failed to fetch user info';
        console.error('Error details:', errorMessage);
        navigate('/login?error=auth_failed');
      }
    };

    handleCallback();
  }, [searchParams, setAuth, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-white">
      <div className="flex flex-col items-center gap-4">
        <Loader2 className="w-8 h-8 text-brand-600 animate-spin" />
        <p className="text-body-sm text-neutral-500">Authenticating...</p>
        <p className="text-meta-xs text-neutral-400">Please wait while we complete the login process</p>
      </div>
    </div>
  );
}
