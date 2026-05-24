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
