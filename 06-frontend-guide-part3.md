# Frontend Implementation Guide — Part 3: Repo Components, Auth, Common

---

## 6.9 src/components/Repo/RepoList.tsx

```tsx
import { Repo } from '../../types';
import { RepoCard } from './RepoCard';

interface Props {
  repos: Repo[];
}

export function RepoList({ repos }: Props) {
  if (repos.length === 0) {
    return (
      <p className="text-sm text-surface-400 italic">
        No repositories connected. Click + to add one.
      </p>
    );
  }

  return (
    <div className="space-y-1">
      {repos.map((repo) => (
        <RepoCard key={repo.id} repo={repo} />
      ))}
    </div>
  );
}
```

### 6.10 src/components/Repo/RepoCard.tsx

```tsx
import { Repo } from '../../types';
import { useChatStore } from '../../stores/chatStore';
import { useDisconnectRepo, useReindexRepo } from '../../hooks/useRepos';
import { useIndexingProgress } from '../../hooks/useSSE';
import { GitBranch, Trash2, RefreshCw, CheckCircle, AlertCircle, Loader2 } from 'lucide-react';

interface Props {
  repo: Repo;
}

export function RepoCard({ repo }: Props) {
  const activeRepoId = useChatStore((s) => s.activeRepoId);
  const setActiveRepo = useChatStore((s) => s.setActiveRepo);
  const resetChat = useChatStore((s) => s.resetChat);
  const disconnectMutation = useDisconnectRepo();
  const reindexMutation = useReindexRepo();

  const isActive = activeRepoId === repo.id;
  const isIndexing = repo.status === 'INDEXING' || repo.status === 'PENDING';
  const indexingStatus = useIndexingProgress(repo.id, isIndexing);

  const handleSelect = () => {
    if (repo.status !== 'READY') return;
    resetChat();
    setActiveRepo(repo.id);
  };

  const statusIcon = {
    READY: <CheckCircle size={14} className="text-green-500" />,
    INDEXING: <Loader2 size={14} className="text-blue-500 animate-spin" />,
    PENDING: <Loader2 size={14} className="text-yellow-500 animate-spin" />,
    FAILED: <AlertCircle size={14} className="text-red-500" />,
  }[repo.status];

  return (
    <div
      onClick={handleSelect}
      className={`group p-2 rounded-md cursor-pointer transition-colors ${
        isActive ? 'bg-primary-50 border border-primary-200' : 'hover:bg-surface-100'
      } ${repo.status !== 'READY' ? 'opacity-70 cursor-not-allowed' : ''}`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 min-w-0">
          {statusIcon}
          <span className="text-sm font-medium truncate">{repo.fullName}</span>
        </div>

        <div className="hidden group-hover:flex items-center gap-1">
          {repo.status === 'READY' && (
            <button
              onClick={(e) => { e.stopPropagation(); reindexMutation.mutate(repo.id); }}
              className="p-1 rounded hover:bg-surface-200"
              title="Re-index"
            >
              <RefreshCw size={12} />
            </button>
          )}
          <button
            onClick={(e) => { e.stopPropagation(); disconnectMutation.mutate(repo.id); }}
            className="p-1 rounded hover:bg-red-100 text-red-500"
            title="Disconnect"
          >
            <Trash2 size={12} />
          </button>
        </div>
      </div>

      {/* Indexing progress bar */}
      {isIndexing && indexingStatus && (
        <div className="mt-2">
          <div className="flex justify-between text-xs text-surface-500 mb-1">
            <span>Indexing...</span>
            <span>{indexingStatus.progress}%</span>
          </div>
          <div className="w-full bg-surface-200 rounded-full h-1.5">
            <div
              className="bg-primary-500 h-1.5 rounded-full transition-all"
              style={{ width: `${indexingStatus.progress}%` }}
            />
          </div>
          <p className="text-xs text-surface-400 mt-1">
            {indexingStatus.processedFiles}/{indexingStatus.totalFiles} files
          </p>
        </div>
      )}

      {/* Metadata */}
      {repo.status === 'READY' && (
        <div className="flex items-center gap-2 mt-1 text-xs text-surface-400">
          <GitBranch size={10} />
          <span>{repo.branch}</span>
          <span>•</span>
          <span>{repo.totalChunks} chunks</span>
        </div>
      )}
    </div>
  );
}
```

### 6.11 src/components/Repo/ConnectRepoModal.tsx

```tsx
import { useState } from 'react';
import { useConnectRepo } from '../../hooks/useRepos';
import { X } from 'lucide-react';

interface Props {
  onClose: () => void;
}

export function ConnectRepoModal({ onClose }: Props) {
  const [repoName, setRepoName] = useState('');
  const [branch, setBranch] = useState('');
  const connectMutation = useConnectRepo();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    connectMutation.mutate(
      { repoFullName: repoName, branch: branch || undefined },
      { onSuccess: onClose }
    );
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Connect Repository</h2>
          <button onClick={onClose} className="p-1 rounded hover:bg-surface-100">
            <X size={20} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-1">Repository</label>
            <input
              type="text"
              value={repoName}
              onChange={(e) => setRepoName(e.target.value)}
              placeholder="owner/repository-name"
              className="w-full px-3 py-2 border border-surface-300 rounded-md
                         focus:outline-none focus:ring-2 focus:ring-primary-500"
              required
            />
            <p className="text-xs text-surface-400 mt-1">
              e.g., facebook/react or your-username/your-repo
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Branch (optional)</label>
            <input
              type="text"
              value={branch}
              onChange={(e) => setBranch(e.target.value)}
              placeholder="main (default)"
              className="w-full px-3 py-2 border border-surface-300 rounded-md
                         focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>

          {connectMutation.isError && (
            <p className="text-sm text-red-600">
              Failed to connect. Make sure you have access to this repository.
            </p>
          )}

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm rounded-md border border-surface-300 hover:bg-surface-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!repoName || connectMutation.isPending}
              className="px-4 py-2 text-sm rounded-md bg-primary-600 text-white
                         hover:bg-primary-700 disabled:opacity-50"
            >
              {connectMutation.isPending ? 'Connecting...' : 'Connect & Index'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

### 6.12 src/components/Sidebar/ConversationList.tsx

```tsx
import { useQuery } from '@tanstack/react-query';
import { conversationApi } from '../../api/conversation.api';
import { useChatStore } from '../../stores/chatStore';
import { MessageSquare, Plus } from 'lucide-react';

interface Props {
  repoId: string;
}

export function ConversationList({ repoId }: Props) {
  const { data } = useQuery({
    queryKey: ['conversations', repoId],
    queryFn: () => conversationApi.list(repoId),
  });

  const activeConversationId = useChatStore((s) => s.activeConversationId);
  const setActiveConversation = useChatStore((s) => s.setActiveConversation);
  const resetChat = useChatStore((s) => s.resetChat);

  return (
    <div className="space-y-1">
      {/* New conversation button */}
      <button
        onClick={resetChat}
        className="w-full flex items-center gap-2 p-2 rounded-md text-sm
                   text-primary-600 hover:bg-primary-50 transition-colors"
      >
        <Plus size={14} />
        <span>New conversation</span>
      </button>

      {/* Existing conversations */}
      {data?.conversations.map((conv) => (
        <button
          key={conv.id}
          onClick={() => setActiveConversation(conv.id)}
          className={`w-full flex items-center gap-2 p-2 rounded-md text-sm text-left
                     transition-colors ${
                       activeConversationId === conv.id
                         ? 'bg-primary-50 text-primary-700'
                         : 'hover:bg-surface-100 text-surface-700'
                     }`}
        >
          <MessageSquare size={14} className="shrink-0" />
          <span className="truncate">{conv.title}</span>
        </button>
      ))}
    </div>
  );
}
```

### 6.13 src/components/Auth/LoginPage.tsx

```tsx
import { Github } from 'lucide-react';
import { authApi } from '../../api/auth.api';

export function LoginPage() {
  const handleLogin = () => {
    window.location.href = authApi.getGithubAuthUrl();
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-surface-50">
      <div className="text-center">
        <h1 className="text-4xl font-bold mb-2">Codebase Q&A</h1>
        <p className="text-surface-500 mb-8">
          Ask questions about any GitHub repository using AI
        </p>

        <button
          onClick={handleLogin}
          className="inline-flex items-center gap-3 px-6 py-3 bg-surface-900
                     text-white rounded-lg hover:bg-surface-800 transition-colors"
        >
          <Github size={20} />
          <span>Sign in with GitHub</span>
        </button>
      </div>
    </div>
  );
}
```

### 6.14 src/components/Auth/OAuthCallbackPage.tsx

```tsx
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
```

---

## 7. Environment Variables

### frontend/.env.example

```env
# API URL (leave empty for same-origin in production)
VITE_API_URL=http://localhost:8080

# GitHub OAuth (for redirect — the actual OAuth is handled by backend)
VITE_GITHUB_CLIENT_ID=your_github_client_id
```

---

## 8. UI Design Specifications

### Color Palette
- **Primary:** Blue (#3b82f6) — buttons, links, active states
- **Surface:** Slate grays — backgrounds, borders, text
- **Success:** Green (#22c55e) — ready status
- **Warning:** Yellow (#eab308) — pending status
- **Error:** Red (#ef4444) — failed status, errors

### Layout
- **Sidebar:** 288px fixed width, white background, border-right
- **Main area:** Flex-grow, light gray background
- **Chat messages:** Max-width 80%, alternating alignment (user right, assistant left)
- **Input bar:** Fixed at bottom, white background, border-top

### Typography
- **Headings:** Inter/system font, semibold
- **Body:** 14px, regular
- **Code:** JetBrains Mono / monospace, 12-13px
- **Citations:** 12px, monospace for file paths

### Responsive Breakpoints
- **Desktop (>1024px):** Full sidebar + chat
- **Tablet (768-1024px):** Collapsible sidebar (hamburger menu)
- **Mobile (<768px):** Not primary target, but usable with stacked layout
