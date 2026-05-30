import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';

/**
 * Main Layout Component
 * Provides the app shell with sidebar and main content area
 */
export function MainLayout() {
  return (
    <div className="flex min-h-screen bg-white">
      <Sidebar />
      <main className="flex-1 ml-sidebar-width">
        <Outlet />
      </main>
    </div>
  );
}
