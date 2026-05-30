import { useState } from 'react';
import { Plus, FolderCode, RefreshCw, ExternalLink, Trash2, Loader2 } from 'lucide-react';

/**
 * Repositories Page
 * Manage connected repositories and their indexing status
 */
export function ReposPage() {
  const [showConnectModal, setShowConnectModal] = useState(false);
  const [repoName, setRepoName] = useState('');
  const [branch, setBranch] = useState('main');

  // Mock data - will be replaced with actual API calls
  const repos = [
    {
      id: '1',
      fullName: 'octocat/hello-world',
      branch: 'main',
      status: 'READY' as const,
      totalChunks: 245,
      lastIndexedAt: '2 hours ago',
    },
    {
      id: '2',
      fullName: 'octocat/spoon-knife',
      branch: 'main',
      status: 'INDEXING' as const,
      totalChunks: 54,
      progress: 45,
      processedFiles: 54,
      totalFiles: 120,
    },
    {
      id: '3',
      fullName: 'octocat/test-repo',
      branch: 'main',
      status: 'READY' as const,
      totalChunks: 152,
      lastIndexedAt: '1 day ago',
    },
  ];

  const handleConnect = () => {
    if (!repoName.trim()) return;
    // TODO: Connect repository
    console.log('Connect:', repoName, branch);
    setShowConnectModal(false);
    setRepoName('');
    setBranch('main');
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'READY':
        return (
          <div className="flex items-center gap-1.5 px-2 py-1 bg-green-50 text-green-600 rounded-full">
            <span className="w-1.5 h-1.5 bg-green-600 rounded-full" />
            <span className="text-[10px] font-semibold uppercase tracking-wide">Ready</span>
          </div>
        );
      case 'INDEXING':
        return (
          <div className="flex items-center gap-1.5 px-2 py-1 bg-yellow-50 text-warning rounded-full">
            <span className="w-1.5 h-1.5 bg-warning rounded-full animate-pulse" />
            <span className="text-[10px] font-semibold uppercase tracking-wide">Indexing</span>
          </div>
        );
      case 'FAILED':
        return (
          <div className="flex items-center gap-1.5 px-2 py-1 bg-red-50 text-error rounded-full">
            <span className="w-1.5 h-1.5 bg-error rounded-full" />
            <span className="text-[10px] font-semibold uppercase tracking-wide">Failed</span>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-white">
      <div className="max-w-site mx-auto px-chat-padding py-8">
        {/* Page Header */}
        <div className="flex items-end justify-between mb-8">
          <div>
            <h1 className="text-display-xs font-semibold text-neutral-900 mb-1">
              Repositories
            </h1>
            <p className="text-body-md text-neutral-600">
              Manage connected codebases and their indexing status for AI analysis.
            </p>
          </div>
          <button
            onClick={() => setShowConnectModal(true)}
            className="flex items-center gap-2 bg-brand-600 text-white font-semibold px-6 py-2.5 rounded-lg hover:bg-brand-700 active:scale-[0.98] transition-all shadow-sm"
          >
            <Plus className="w-5 h-5" />
            Connect Repo
          </button>
        </div>

        {/* Stats Dashboard */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-10">
          <div className="bg-white p-4 rounded-xl border border-neutral-200 shadow-xs">
            <p className="text-label-caps uppercase text-neutral-500 mb-1">Total Size</p>
            <p className="text-[20px] font-semibold text-neutral-900">1.2 GB</p>
          </div>
          <div className="bg-white p-4 rounded-xl border border-neutral-200 shadow-xs">
            <p className="text-label-caps uppercase text-neutral-500 mb-1">Ready</p>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 bg-success rounded-full" />
              <p className="text-[20px] font-semibold text-neutral-900">
                {repos.filter((r) => r.status === 'READY').length}
              </p>
            </div>
          </div>
          <div className="bg-white p-4 rounded-xl border border-neutral-200 shadow-xs">
            <p className="text-label-caps uppercase text-neutral-500 mb-1">Indexing</p>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 bg-warning rounded-full animate-pulse" />
              <p className="text-[20px] font-semibold text-neutral-900">
                {repos.filter((r) => r.status === 'INDEXING').length}
              </p>
            </div>
          </div>
          <div className="bg-white p-4 rounded-xl border border-neutral-200 shadow-xs">
            <p className="text-label-caps uppercase text-neutral-500 mb-1">Last Sync</p>
            <p className="text-[20px] font-semibold text-neutral-900">14m ago</p>
          </div>
        </div>

        {/* Repositories Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
          {repos.map((repo) => (
            <div
              key={repo.id}
              className="group bg-white border border-neutral-200 rounded-xl p-5 shadow-xs hover:shadow-sm hover:-translate-y-1 transition-all duration-200"
            >
              {/* Header */}
              <div className="flex justify-between items-start mb-4">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-brand-50 rounded-lg flex items-center justify-center border border-brand-200">
                    <FolderCode className="w-6 h-6 text-brand-600" />
                  </div>
                  <div>
                    <h3 className="text-card-title font-medium text-neutral-900">
                      {repo.fullName.split('/')[1]}
                    </h3>
                    <p className="text-meta-xs text-neutral-500">{repo.fullName}</p>
                  </div>
                </div>
                {getStatusBadge(repo.status)}
              </div>

              {/* Stats */}
              <div className="space-y-3 mb-6">
                {repo.status === 'INDEXING' && repo.progress !== undefined ? (
                  <>
                    <div className="flex justify-between text-meta-xs text-neutral-600 mb-1">
                      <span>Processing vectors...</span>
                      <span>{repo.progress}%</span>
                    </div>
                    <div className="w-full bg-neutral-100 h-1.5 rounded-full overflow-hidden">
                      <div
                        className="bg-warning h-full rounded-full transition-all duration-1000"
                        style={{ width: `${repo.progress}%` }}
                      />
                    </div>
                  </>
                ) : (
                  <div className="flex justify-between text-meta-xs text-neutral-600">
                    <span>{repo.totalChunks} files indexed</span>
                    <span>Updated {repo.lastIndexedAt}</span>
                  </div>
                )}
              </div>

              {/* Actions */}
              <div className="flex items-center gap-2 pt-4 border-t border-neutral-200">
                <button className="flex-1 py-1.5 text-body-sm font-semibold text-neutral-600 hover:bg-neutral-50 rounded transition-colors flex items-center justify-center gap-1.5">
                  <ExternalLink className="w-4 h-4" />
                  GitHub
                </button>
                <button
                  disabled={repo.status === 'INDEXING'}
                  className="flex-1 py-1.5 text-body-sm font-semibold text-neutral-600 hover:bg-neutral-50 rounded transition-colors flex items-center justify-center gap-1.5 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <RefreshCw className="w-4 h-4" />
                  Re-index
                </button>
                <button className="p-1.5 text-neutral-600 hover:text-error hover:bg-red-50 rounded transition-colors">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}

          {/* Connect Placeholder */}
          <button
            onClick={() => setShowConnectModal(true)}
            className="group relative flex flex-col items-center justify-center p-8 rounded-xl border-2 border-dashed border-neutral-300 bg-neutral-50 hover:bg-neutral-100 hover:border-brand-400 transition-all duration-200 min-h-[220px]"
          >
            <div className="w-12 h-12 rounded-full bg-neutral-200 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
              <Plus className="w-6 h-6 text-neutral-600" />
            </div>
            <span className="text-card-title font-medium text-neutral-900">
              Connect Repository
            </span>
            <span className="text-meta-xs text-neutral-500 mt-1">
              Import from GitHub or GitLab
            </span>
          </button>
        </div>
      </div>

      {/* Connect Modal */}
      {showConnectModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full">
            {/* Modal Header */}
            <div className="flex items-center justify-between p-6 border-b border-neutral-200">
              <h2 className="text-headline-sm font-semibold text-neutral-900">
                Connect Repository
              </h2>
              <button
                onClick={() => setShowConnectModal(false)}
                className="text-neutral-500 hover:text-neutral-700"
              >
                ✕
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-body-sm font-medium text-neutral-700 mb-2">
                  Repository
                </label>
                <input
                  type="text"
                  value={repoName}
                  onChange={(e) => setRepoName(e.target.value)}
                  placeholder="owner/repository-name"
                  className="w-full px-4 py-3 border border-neutral-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                />
                <p className="text-meta-xs text-neutral-500 mt-1">
                  Format: owner/repo-name
                </p>
              </div>

              <div>
                <label className="block text-body-sm font-medium text-neutral-700 mb-2">
                  Branch (optional)
                </label>
                <input
                  type="text"
                  value={branch}
                  onChange={(e) => setBranch(e.target.value)}
                  placeholder="main"
                  className="w-full px-4 py-3 border border-neutral-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-brand-500 focus:border-brand-500"
                />
                <p className="text-meta-xs text-neutral-500 mt-1">
                  Leave empty for default branch
                </p>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="flex items-center justify-end gap-3 p-6 border-t border-neutral-200">
              <button
                onClick={() => setShowConnectModal(false)}
                className="px-4 py-2 text-body-sm font-medium text-neutral-700 hover:bg-neutral-50 rounded-lg transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleConnect}
                disabled={!repoName.trim()}
                className="px-4 py-2 text-body-sm font-semibold bg-brand-600 text-white rounded-lg hover:bg-brand-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              >
                Connect & Index
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
