import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { FolderCode } from 'lucide-react';
import { useAuthStore } from '../stores/authStore';

/**
 * Login Page - GitHub OAuth authentication
 * Minimal centered design following Untitled UI patterns
 */
export function LoginPage() {
  const navigate = useNavigate();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/');
    }
  }, [isAuthenticated, navigate]);

  const handleGitHubLogin = () => {
    // Redirect to backend OAuth endpoint
    const apiUrl = import.meta.env.VITE_API_URL || '';
    window.location.href = `${apiUrl}/api/auth/github`;
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-white relative overflow-hidden">
      {/* Subtle dot pattern background */}
      <div 
        className="absolute inset-0 opacity-30"
        style={{
          backgroundImage: 'radial-gradient(rgb(229 229 229) 1px, transparent 1px)',
          backgroundSize: '24px 24px',
        }}
      />

      {/* Subtle gradient glow */}
      <div className="fixed top-0 right-0 -z-10 w-[600px] h-[600px] bg-brand-50 rounded-full blur-[120px] opacity-40 translate-x-1/2 -translate-y-1/2 pointer-events-none" />

      {/* Login Card */}
      <main className="relative w-full max-w-sm px-6 py-12 flex flex-col items-center text-center">
        {/* Brand Icon */}
        <div className="w-16 h-16 mb-6 flex items-center justify-center rounded-xl bg-brand-50 border border-brand-200">
          <FolderCode className="w-10 h-10 text-brand-600" />
        </div>

        {/* Heading */}
        <h1 className="text-display-xs font-semibold text-neutral-900 mb-2">
          Codebase Q&A
        </h1>

        {/* Subtitle */}
        <p className="text-body-md text-neutral-600 mb-8">
          Ask questions about any codebase.
          <br />
          No local clone required.
        </p>

        {/* GitHub Login Button */}
        <button
          onClick={handleGitHubLogin}
          className="w-full flex items-center justify-center gap-3 bg-brand-600 text-white font-semibold px-6 py-3.5 rounded-lg hover:bg-brand-700 active:scale-[0.98] transition-all shadow-sm"
        >
          <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
          </svg>
          Continue with GitHub
        </button>

        {/* Terms */}
        <p className="mt-6 text-meta-xs text-neutral-500">
          By continuing, you agree to our Terms of Service.
        </p>
      </main>
    </div>
  );
}
