# Frontend Implementation Guide — Part 1: Setup, Types, API Layer, Stores

---

## 1. Project Setup

### 1.1 Create Vite + React + TypeScript Project

```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
```

### 1.2 Install Dependencies

```bash
# Core
npm install @tanstack/react-query zustand axios react-router-dom

# UI
npm install tailwindcss @tailwindcss/typography postcss autoprefixer
npm install react-markdown react-syntax-highlighter
npm install @radix-ui/react-dialog @radix-ui/react-toast
npm install lucide-react  # Icons

# Types
npm install -D @types/react-syntax-highlighter
```

### 1.3 Tailwind Setup

```js
// tailwind.config.js
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: { 50: '#eff6ff', 500: '#3b82f6', 600: '#2563eb', 700: '#1d4ed8' },
        surface: { 50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 800: '#1e293b', 900: '#0f172a' },
      },
    },
  },
  plugins: [require('@tailwindcss/typography')],
};
```

### 1.4 Vite Config

```ts
// vite.config.ts
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

---

## 2. TypeScript Types

### src/types/index.ts

```typescript
// ===== User =====
export interface User {
  id: string;
  username: string;
  email: string;
  avatarUrl: string;
}

// ===== Repository =====
export interface Repo {
  id: string;
  fullName: string;
  branch: string;
  status: 'PENDING' | 'INDEXING' | 'READY' | 'FAILED';
  totalChunks: number;
  lastIndexedAt: string | null;
  createdAt: string;
}

// ===== Conversation =====
export interface Conversation {
  id: string;
  repoId: string;
  repoFullName: string;
  title: string;
  messageCount?: number;
  createdAt: string;
  updatedAt: string;
}

// ===== Message =====
export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  citations: Citation[] | null;
  createdAt: string;
}

export interface Citation {
  filePath: string;
  startLine: number;
  endLine: number;
  chunkName: string | null;
  snippet: string;
  similarity?: number;
}

// ===== Indexing =====
export interface IndexingStatus {
  jobId: string;
  status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progress: number;
  processedFiles: number;
  totalFiles: number;
  errorMessage?: string;
}

// ===== API Responses =====
export interface ApiResponse<T> {
  data: T;
  timestamp?: string;
}

export interface ApiError {
  error: {
    code: string;
    message: string;
    retryAfter?: number;
  };
}

// ===== SSE Events =====
export interface SSETokenEvent {
  content: string;
}

export interface SSEDoneEvent {
  messageId: string;
  conversationId: string;
  tokenCount?: number;
  cached?: boolean;
}
```

---

## 3. API Layer

### 3.1 src/api/client.ts (Axios Instance)

```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor — attach JWT
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor — handle 401
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### 3.2 src/api/auth.api.ts

```typescript
import apiClient from './client';
import { User } from '../types';

export const authApi = {
  getGithubAuthUrl: () => {
    return `${import.meta.env.VITE_API_URL || ''}/api/auth/github`;
  },

  handleCallback: async (code: string): Promise<{ token: string; user: User }> => {
    const { data } = await apiClient.get(`/auth/github/callback?code=${code}`);
    return data.data;
  },

  getMe: async (): Promise<User> => {
    const { data } = await apiClient.get('/auth/me');
    return data.data;
  },
};
```

### 3.3 src/api/repo.api.ts

```typescript
import apiClient from './client';
import { Repo } from '../types';

export const repoApi = {
  connect: async (repoFullName: string, branch?: string): Promise<Repo> => {
    const { data } = await apiClient.post('/repos', { repoFullName, branch });
    return data.data;
  },

  list: async (): Promise<Repo[]> => {
    const { data } = await apiClient.get('/repos');
    return data.data;
  },

  disconnect: async (repoId: string): Promise<void> => {
    await apiClient.delete(`/repos/${repoId}`);
  },

  reindex: async (repoId: string): Promise<{ jobId: string }> => {
    const { data } = await apiClient.post(`/repos/${repoId}/reindex`);
    return data.data;
  },
};
```

### 3.4 src/api/query.api.ts (SSE Streaming)

```typescript
/**
 * This does NOT use axios — it uses fetch() with ReadableStream
 * because we need to consume SSE from a POST endpoint.
 */

import { Citation, SSEDoneEvent } from '../types';

interface StreamCallbacks {
  onCitations: (citations: Citation[]) => void;
  onToken: (token: string) => void;
  onDone: (event: SSEDoneEvent) => void;
  onError: (error: string) => void;
}

export async function streamQuestion(
  repoId: string,
  question: string,
  conversationId: string | null,
  callbacks: StreamCallbacks
): Promise<void> {
  const token = localStorage.getItem('token');
  const baseUrl = import.meta.env.VITE_API_URL || '';

  const response = await fetch(`${baseUrl}/api/query`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      Accept: 'text/event-stream',
    },
    body: JSON.stringify({ repoId, question, conversationId }),
  });

  if (!response.ok) {
    const error = await response.json();
    callbacks.onError(error.error?.message || 'Request failed');
    return;
  }

  const reader = response.body?.getReader();
  if (!reader) {
    callbacks.onError('No response body');
    return;
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || ''; // Keep incomplete line in buffer

    let currentEvent = '';

    for (const line of lines) {
      if (line.startsWith('event: ')) {
        currentEvent = line.slice(7);
      } else if (line.startsWith('data: ')) {
        const data = line.slice(6);
        try {
          const parsed = JSON.parse(data);

          switch (currentEvent) {
            case 'citations':
              callbacks.onCitations(parsed);
              break;
            case 'token':
              callbacks.onToken(parsed.content);
              break;
            case 'done':
              callbacks.onDone(parsed);
              break;
            case 'error':
              callbacks.onError(parsed.message);
              break;
          }
        } catch {
          // Non-JSON data line, ignore
        }
      }
    }
  }
}
```

### 3.5 src/api/conversation.api.ts

```typescript
import apiClient from './client';
import { Conversation, Message } from '../types';

export const conversationApi = {
  list: async (repoId?: string, page = 0, size = 20): Promise<{
    conversations: Conversation[];
    totalCount: number;
  }> => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (repoId) params.set('repoId', repoId);
    const { data } = await apiClient.get(`/conversations?${params}`);
    return data.data;
  },

  get: async (conversationId: string): Promise<{ conversation: Conversation; messages: Message[] }> => {
    const { data } = await apiClient.get(`/conversations/${conversationId}`);
    return data.data;
  },

  delete: async (conversationId: string): Promise<void> => {
    await apiClient.delete(`/conversations/${conversationId}`);
  },
};
```

---

## 4. State Management (Zustand Stores)

### 4.1 src/stores/authStore.ts

```typescript
import { create } from 'zustand';
import { User } from '../types';

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: User) => void;
  logout: () => void;
  loadFromStorage: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,

  setAuth: (token, user) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    set({ token, user, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    set({ token: null, user: null, isAuthenticated: false });
  },

  loadFromStorage: () => {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');
    if (token && userStr) {
      set({ token, user: JSON.parse(userStr), isAuthenticated: true });
    }
  },
}));
```

### 4.2 src/stores/chatStore.ts

```typescript
import { create } from 'zustand';
import { Citation, Message } from '../types';

interface ChatState {
  // Current active chat
  activeRepoId: string | null;
  activeConversationId: string | null;
  messages: Message[];
  isStreaming: boolean;
  streamingContent: string;
  citations: Citation[];

  // Actions
  setActiveRepo: (repoId: string) => void;
  setActiveConversation: (conversationId: string | null) => void;
  setMessages: (messages: Message[]) => void;
  addMessage: (message: Message) => void;
  setStreaming: (isStreaming: boolean) => void;
  appendStreamContent: (token: string) => void;
  setCitations: (citations: Citation[]) => void;
  resetChat: () => void;
}

export const useChatStore = create<ChatState>((set) => ({
  activeRepoId: null,
  activeConversationId: null,
  messages: [],
  isStreaming: false,
  streamingContent: '',
  citations: [],

  setActiveRepo: (repoId) => set({ activeRepoId: repoId }),

  setActiveConversation: (conversationId) => set({
    activeConversationId: conversationId,
    messages: [],
    streamingContent: '',
    citations: [],
  }),

  setMessages: (messages) => set({ messages }),

  addMessage: (message) => set((state) => ({
    messages: [...state.messages, message],
  })),

  setStreaming: (isStreaming) => set({ isStreaming }),

  appendStreamContent: (token) => set((state) => ({
    streamingContent: state.streamingContent + token,
  })),

  setCitations: (citations) => set({ citations }),

  resetChat: () => set({
    activeConversationId: null,
    messages: [],
    streamingContent: '',
    citations: [],
    isStreaming: false,
  }),
}));
```
