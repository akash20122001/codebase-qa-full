export function AppLayout() {
  return (
    <div className="flex h-screen bg-surface-50">
      {/* Sidebar placeholder */}
      <aside className="w-72 border-r border-surface-200 bg-white p-4">
        <h2 className="text-lg font-semibold mb-4">Codebase Q&A</h2>
        <p className="text-sm text-surface-500">
          Repository list will appear here
        </p>
      </aside>

      {/* Main content area */}
      <main className="flex-1 flex flex-col">
        <div className="flex-1 flex items-center justify-center text-surface-500">
          <div className="text-center">
            <h2 className="text-2xl font-semibold mb-2">Welcome to Codebase Q&A</h2>
            <p>Select a repository from the sidebar to start asking questions</p>
          </div>
        </div>
      </main>
    </div>
  );
}
