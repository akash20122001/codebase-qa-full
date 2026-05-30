import { NavLink } from 'react-router-dom';
import { 
  FolderCode, 
  MessageSquare, 
  Clock, 
  Settings, 
  FileText, 
  MessageCircle,
  Plus,
  LogOut
} from 'lucide-react';
import { useAuthStore } from '../../stores/authStore';

/**
 * Sidebar Navigation Component
 * Fixed-width sidebar with navigation links and user profile
 */
export function Sidebar() {
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  const navItems = [
    { to: '/repos', icon: FolderCode, label: 'Repositories' },
    { to: '/', icon: MessageSquare, label: 'Conversations' },
    { to: '/history', icon: Clock, label: 'History' },
    { to: '/settings', icon: Settings, label: 'Settings' },
  ];

  return (
    <aside className="fixed h-full w-sidebar-width left-0 top-0 bg-white border-r border-neutral-200 flex flex-col py-4 space-y-2 z-50">
      {/* Header */}
      <div className="px-6 mb-8">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-brand-50 border border-brand-200 flex items-center justify-center">
            <FolderCode className="w-5 h-5 text-brand-600" />
          </div>
          <div>
            <h1 className="text-headline-sm font-bold text-neutral-900">Codebase Q&A</h1>
            <p className="text-[10px] text-neutral-500 uppercase tracking-wider">v1.0.0</p>
          </div>
        </div>
      </div>

      {/* New Chat Button */}
      <div className="px-4 mb-4">
        <button className="w-full flex items-center justify-center gap-2 bg-brand-600 text-white font-body-md py-2.5 px-4 rounded-lg hover:bg-brand-700 transition-all active:scale-[0.98] shadow-sm">
          <Plus className="w-5 h-5" />
          New Chat
        </button>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-2 space-y-1 overflow-y-auto custom-scrollbar">
        <div className="text-label-caps uppercase text-neutral-500 px-4 py-2 mb-1">
          Main Menu
        </div>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex items-center px-4 py-3 rounded-lg transition-colors duration-150 ${
                isActive
                  ? 'text-neutral-900 font-bold border-l-2 border-brand-600 bg-neutral-50'
                  : 'text-neutral-600 hover:bg-neutral-50'
              }`
            }
          >
            <item.icon className="w-5 h-5 mr-3" />
            <span className="text-body-md">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Footer */}
      <div className="mt-auto border-t border-neutral-200 pt-4 px-2">
        <a
          href="#"
          className="flex items-center px-4 py-3 rounded-lg text-neutral-600 hover:bg-neutral-50 transition-colors duration-150"
        >
          <FileText className="w-5 h-5 mr-3" />
          <span className="text-body-md">Documentation</span>
        </a>
        <a
          href="#"
          className="flex items-center px-4 py-3 rounded-lg text-neutral-600 hover:bg-neutral-50 transition-colors duration-150"
        >
          <MessageCircle className="w-5 h-5 mr-3" />
          <span className="text-body-md">Feedback</span>
        </a>

        {/* User Profile */}
        {user && (
          <div className="mt-4 px-4 flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-brand-50 border border-brand-400 flex items-center justify-center overflow-hidden">
              {user.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.username}
                  className="w-full h-full object-cover"
                />
              ) : (
                <span className="text-brand-600 font-semibold text-sm">
                  {user.username.charAt(0).toUpperCase()}
                </span>
              )}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-body-sm font-semibold truncate text-neutral-900">
                {user.username}
              </p>
              <p className="text-meta-xs text-neutral-500 truncate">Standard Plan</p>
            </div>
            <button
              onClick={logout}
              className="p-2 text-neutral-500 hover:text-error hover:bg-error/10 rounded-lg transition-colors"
              title="Logout"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </aside>
  );
}
