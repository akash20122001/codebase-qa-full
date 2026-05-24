# Frontend Implementation Guide — Part 2: Hooks, Components

---

## 5. Custom Hooks

### 5.1 src/hooks/useAuth.ts

```typescript
import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { authApi } from '../api/auth.api';

export function useAuth() {
  const { user, isAuthenticated, setAuth, logout, loadFromStorage } = useAuthStore();

  useEffect(() => {
    loadFromStorage();
  }, []);

  return { user, isAuthenticated, setAuth, logout };
}

/**
 * Hook for the OAuth callback page.
 * Extracts the code from URL, exchanges it for a token, and redirects.
 */
export function useOAuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();

  useEffect(() => {
    const code = searchParams.get('code');
    if (!code) {
      navigate('/login');
      return;
    }

    authApi.handleCallback(code)
      .then(({ token, user }) => {
        setAuth(token, user);
        navigate('/');
      })
      .catch(() => {
        navigate('/login?error=auth_failed');
      });
  }, [searchParams]);
}
```

### 5.2 src/hooks/useRepos.ts

```typescript
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { repoApi } from '../api/repo.api';

export function useRepos() {
  return useQuery({
    queryKey: ['repos'],
    queryFn: repoApi.list,
  });
}

export function useConnectRepo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ repoFullName, branch }: { repoFullName: string; branch?: string }) =>
      repoApi.connect(repoFullName, branch),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repos'] });
    },
  });
}

export function useDisconnectRepo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (repoId: string) => repoApi.disconnect(repoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repos'] });
    },
  });
}

export function useReindexRepo() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (repoId: string) => repoApi.reindex(repoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repos'] });
    },
  });
}
```

### 5.3 src/hooks/useChat.ts

```typescript
import { useCallback } from 'react';
import { useChatStore } from '../stores/chatStore';
import { streamQuestion } from '../api/query.api';
import { Message } from '../types';

export function useChat() {
  const {
    activeRepoId,
    activeConversationId,
    messages,
    isStreaming,
    streamingContent,
    citations,
    setStreaming,
    appendStreamContent,
    setCitations,
    addMessage,
    setActiveConversation,
    resetChat,
  } = useChatStore();

  const sendMessage = useCallback(async (question: string) => {
    if (!activeRepoId || isStreaming) return;

    // Add user message to UI immediately
    const userMessage: Message = {
      id: crypto.randomUUID(),
      role: 'user',
      content: question,
      citations: null,
      createdAt: new Date().toISOString(),
    };
    addMessage(userMessage);

    // Reset streaming state
    useChatStore.setState({ streamingContent: '', citations: [] });
    setStreaming(true);

    try {
      await streamQuestion(
        activeRepoId,
        question,
        activeConversationId,
        {
          onCitations: (cits) => {
            setCitations(cits);
          },
          onToken: (token) => {
            appendStreamContent(token);
          },
          onDone: (event) => {
            // Finalize: move streaming content to a proper message
            const assistantMessage: Message = {
              id: event.messageId || crypto.randomUUID(),
              role: 'assistant',
              content: useChatStore.getState().streamingContent,
              citations: useChatStore.getState().citations,
              createdAt: new Date().toISOString(),
            };
            addMessage(assistantMessage);
            useChatStore.setState({ streamingContent: '', citations: [] });

            // Set conversation ID if this was a new conversation
            if (event.conversationId && !activeConversationId) {
              setActiveConversation(event.conversationId);
            }
          },
          onError: (error) => {
            const errorMessage: Message = {
              id: crypto.randomUUID(),
              role: 'assistant',
              content: `⚠️ Error: ${error}`,
              citations: null,
              createdAt: new Date().toISOString(),
            };
            addMessage(errorMessage);
          },
        }
      );
    } finally {
      setStreaming(false);
    }
  }, [activeRepoId, activeConversationId, isStreaming]);

  return {
    messages,
    isStreaming,
    streamingContent,
    citations,
    sendMessage,
    resetChat,
  };
}
```

### 5.4 src/hooks/useSSE.ts (Indexing Progress)

```typescript
import { useEffect, useRef, useState } from 'react';
import { IndexingStatus } from '../types';

/**
 * Hook to poll indexing status for a repo.
 * Uses polling instead of SSE for simplicity (SSE GET endpoint alternative).
 */
export function useIndexingProgress(repoId: string | null, isIndexing: boolean) {
  const [status, setStatus] = useState<IndexingStatus | null>(null);
  const intervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    if (!repoId || !isIndexing) {
      setStatus(null);
      return;
    }

    const poll = async () => {
      try {
        const token = localStorage.getItem('token');
        const res = await fetch(`/api/repos/${repoId}/indexing-status`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (res.ok) {
          const data = await res.json();
          setStatus(data.data);

          // Stop polling if done
          if (data.data.status === 'COMPLETED' || data.data.status === 'FAILED') {
            if (intervalRef.current) clearInterval(intervalRef.current);
          }
        }
      } catch {
        // Ignore polling errors
      }
    };

    poll(); // Initial fetch
    intervalRef.current = setInterval(poll, 3000); // Poll every 3s

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [repoId, isIndexing]);

  return status;
}
```

---

## 6. Components

### 6.1 src/App.tsx

```tsx
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAuth } from './hooks/useAuth';
import { AppLayout } from './components/Layout/AppLayout';
import { LoginPage } from './components/Auth/LoginPage';
import { OAuthCallbackPage } from './components/Auth/OAuthCallbackPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 30_000, retry: 1 },
  },
});

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" />;
  return <>{children}</>;
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/auth/callback" element={<OAuthCallbackPage />} />
          <Route
            path="/*"
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
```

### 6.2 src/components/Layout/AppLayout.tsx

```tsx
import { Sidebar } from '../Sidebar/Sidebar';
import { ChatWindow } from '../Chat/ChatWindow';
import { useChatStore } from '../../stores/chatStore';

export function AppLayout() {
  const activeRepoId = useChatStore((s) => s.activeRepoId);

  return (
    <div className="flex h-screen bg-surface-50">
      {/* Sidebar — repo list + conversation list */}
      <Sidebar />

      {/* Main content area */}
      <main className="flex-1 flex flex-col">
        {activeRepoId ? (
          <ChatWindow />
        ) : (
          <div className="flex-1 flex items-center justify-center text-surface-500">
            <div className="text-center">
              <h2 className="text-2xl font-semibold mb-2">Codebase Q&A</h2>
              <p>Select a repository from the sidebar to start asking questions</p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
```

### 6.3 src/components/Sidebar/Sidebar.tsx

```tsx
import { useState } from 'react';
import { useRepos } from '../../hooks/useRepos';
import { useChatStore } from '../../stores/chatStore';
import { useAuthStore } from '../../stores/authStore';
import { RepoList } from '../Repo/RepoList';
import { ConversationList } from './ConversationList';
import { ConnectRepoModal } from '../Repo/ConnectRepoModal';
import { Plus, LogOut } from 'lucide-react';

export function Sidebar() {
  const [showConnectModal, setShowConnectModal] = useState(false);
  const { data: repos } = useRepos();
  const { user, logout } = useAuthStore();
  const activeRepoId = useChatStore((s) => s.activeRepoId);

  return (
    <aside className="w-72 border-r border-surface-200 flex flex-col bg-white">
      {/* Header */}
      <div className="p-4 border-b border-surface-200">
        <div className="flex items-center justify-between">
          <h1 className="font-bold text-lg">Codebase Q&A</h1>
          <button
            onClick={() => setShowConnectModal(true)}
            className="p-1.5 rounded-md hover:bg-surface-100 text-primary-600"
            title="Connect repository"
          >
            <Plus size={20} />
          </button>
        </div>
      </div>

      {/* Repos */}
      <div className="flex-1 overflow-y-auto">
        <div className="p-3">
          <h3 className="text-xs font-semibold text-surface-500 uppercase mb-2">
            Repositories
          </h3>
          <RepoList repos={repos || []} />
        </div>

        {/* Conversations for active repo */}
        {activeRepoId && (
          <div className="p-3 border-t border-surface-200">
            <h3 className="text-xs font-semibold text-surface-500 uppercase mb-2">
              Conversations
            </h3>
            <ConversationList repoId={activeRepoId} />
          </div>
        )}
      </div>

      {/* User footer */}
      <div className="p-3 border-t border-surface-200 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <img
            src={user?.avatarUrl}
            alt={user?.username}
            className="w-7 h-7 rounded-full"
          />
          <span className="text-sm font-medium">{user?.username}</span>
        </div>
        <button onClick={logout} className="p-1.5 rounded hover:bg-surface-100">
          <LogOut size={16} />
        </button>
      </div>

      {showConnectModal && (
        <ConnectRepoModal onClose={() => setShowConnectModal(false)} />
      )}
    </aside>
  );
}
```

### 6.4 src/components/Chat/ChatWindow.tsx

```tsx
import { useRef, useEffect } from 'react';
import { useChat } from '../../hooks/useChat';
import { MessageBubble } from './MessageBubble';
import { StreamingMessage } from './StreamingMessage';
import { InputBar } from './InputBar';
import { useChatStore } from '../../stores/chatStore';

export function ChatWindow() {
  const { messages, isStreaming, streamingContent, citations, sendMessage } = useChat();
  const activeRepoId = useChatStore((s) => s.activeRepoId);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamingContent]);

  return (
    <div className="flex-1 flex flex-col">
      {/* Messages area */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4">
        {messages.length === 0 && !isStreaming && (
          <div className="text-center text-surface-400 mt-20">
            <p className="text-lg">Ask a question about this codebase</p>
            <p className="text-sm mt-2">
              Try: "How does the authentication work?" or "Explain the database schema"
            </p>
          </div>
        )}

        {messages.map((msg) => (
          <MessageBubble key={msg.id} message={msg} />
        ))}

        {isStreaming && streamingContent && (
          <StreamingMessage content={streamingContent} citations={citations} />
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <InputBar onSend={sendMessage} disabled={isStreaming || !activeRepoId} />
    </div>
  );
}
```

### 6.5 src/components/Chat/MessageBubble.tsx

```tsx
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Message } from '../../types';
import { CodeCitation } from './CodeCitation';

interface Props {
  message: Message;
}

export function MessageBubble({ message }: Props) {
  const isUser = message.role === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[80%] rounded-lg px-4 py-3 ${
          isUser
            ? 'bg-primary-600 text-white'
            : 'bg-white border border-surface-200'
        }`}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <div className="prose prose-sm max-w-none">
            <ReactMarkdown
              components={{
                code({ node, className, children, ...props }) {
                  const match = /language-(\w+)/.exec(className || '');
                  const inline = !match;
                  return inline ? (
                    <code className="bg-surface-100 px-1 py-0.5 rounded text-sm" {...props}>
                      {children}
                    </code>
                  ) : (
                    <SyntaxHighlighter
                      style={oneDark}
                      language={match[1]}
                      PreTag="div"
                      className="rounded-md text-sm"
                    >
                      {String(children).replace(/\n$/, '')}
                    </SyntaxHighlighter>
                  );
                },
              }}
            >
              {message.content}
            </ReactMarkdown>
          </div>
        )}

        {/* Citations */}
        {message.citations && message.citations.length > 0 && (
          <div className="mt-3 pt-3 border-t border-surface-200">
            <p className="text-xs font-semibold text-surface-500 mb-2">Sources:</p>
            <div className="space-y-1">
              {message.citations.map((citation, idx) => (
                <CodeCitation key={idx} citation={citation} />
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
```

### 6.6 src/components/Chat/CodeCitation.tsx

```tsx
import { useState } from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneDark } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { Citation } from '../../types';
import { FileCode, ChevronDown, ChevronUp } from 'lucide-react';

interface Props {
  citation: Citation;
}

export function CodeCitation({ citation }: Props) {
  const [expanded, setExpanded] = useState(false);

  // Detect language from file extension
  const ext = citation.filePath.split('.').pop() || '';
  const language = { ts: 'typescript', js: 'javascript', py: 'python', java: 'java',
    go: 'go', rs: 'rust', rb: 'ruby' }[ext] || ext;

  return (
    <div className="border border-surface-200 rounded-md overflow-hidden">
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full flex items-center gap-2 px-3 py-2 text-xs hover:bg-surface-50 transition-colors"
      >
        <FileCode size={14} className="text-primary-600" />
        <span className="font-mono text-primary-700 flex-1 text-left">
          {citation.filePath}:{citation.startLine}-{citation.endLine}
        </span>
        {citation.chunkName && (
          <span className="text-surface-500">{citation.chunkName}</span>
        )}
        {expanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
      </button>

      {expanded && (
        <div className="border-t border-surface-200">
          <SyntaxHighlighter
            style={oneDark}
            language={language}
            showLineNumbers
            startingLineNumber={citation.startLine}
            customStyle={{ margin: 0, fontSize: '12px' }}
          >
            {citation.snippet}
          </SyntaxHighlighter>
        </div>
      )}
    </div>
  );
}
```

### 6.7 src/components/Chat/InputBar.tsx

```tsx
import { useState, useRef, KeyboardEvent } from 'react';
import { Send } from 'lucide-react';

interface Props {
  onSend: (message: string) => void;
  disabled: boolean;
}

export function InputBar({ onSend, disabled }: Props) {
  const [input, setInput] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setInput('');
    // Reset textarea height
    if (textareaRef.current) textareaRef.current.style.height = 'auto';
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  // Auto-resize textarea
  const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    const el = e.target;
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 150) + 'px';
  };

  return (
    <div className="border-t border-surface-200 p-4 bg-white">
      <div className="flex items-end gap-3 max-w-4xl mx-auto">
        <textarea
          ref={textareaRef}
          value={input}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          placeholder="Ask a question about the codebase..."
          disabled={disabled}
          rows={1}
          className="flex-1 resize-none rounded-lg border border-surface-300 px-4 py-3
                     focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent
                     disabled:opacity-50 disabled:cursor-not-allowed"
        />
        <button
          onClick={handleSend}
          disabled={disabled || !input.trim()}
          className="p-3 rounded-lg bg-primary-600 text-white hover:bg-primary-700
                     disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        >
          <Send size={20} />
        </button>
      </div>
    </div>
  );
}
```

### 6.8 src/components/Chat/StreamingMessage.tsx

```tsx
import ReactMarkdown from 'react-markdown';
import { Citation } from '../../types';
import { CodeCitation } from './CodeCitation';
import { Loader2 } from 'lucide-react';

interface Props {
  content: string;
  citations: Citation[];
}

export function StreamingMessage({ content, citations }: Props) {
  return (
    <div className="flex justify-start">
      <div className="max-w-[80%] rounded-lg px-4 py-3 bg-white border border-surface-200">
        {/* Citations shown at top while streaming */}
        {citations.length > 0 && (
          <div className="mb-3 pb-3 border-b border-surface-200">
            <p className="text-xs font-semibold text-surface-500 mb-2">
              Searching codebase...
            </p>
            <div className="space-y-1">
              {citations.slice(0, 3).map((c, i) => (
                <CodeCitation key={i} citation={c} />
              ))}
            </div>
          </div>
        )}

        {/* Streaming content */}
        <div className="prose prose-sm max-w-none">
          <ReactMarkdown>{content}</ReactMarkdown>
        </div>

        {/* Streaming indicator */}
        <div className="flex items-center gap-2 mt-2 text-primary-600">
          <Loader2 size={14} className="animate-spin" />
          <span className="text-xs">Generating...</span>
        </div>
      </div>
    </div>
  );
}
```
