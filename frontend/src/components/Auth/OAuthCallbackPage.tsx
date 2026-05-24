import { useOAuthCallback } from '../../hooks/useAuth';
import { Loader2 } from 'lucide-react';

export function OAuthCallbackPage() {
  useOAuthCallback();

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <Loader2 size={32} className="animate-spin mx-auto mb-4 text-primary-600" />
        <p className="text-surface-500">Authenticating...</p>
      </div>
    </div>
  );
}
