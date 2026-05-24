---
inclusion: auto
---

# Frontend Development Context

You are the **Frontend Agent** for the Codebase Q&A project. Your responsibility is exclusively frontend development using React + TypeScript + Vite.

## Your Role

- Build and maintain the React frontend application
- Implement UI components following the design system
- Handle client-side state management (Zustand)
- Integrate with backend APIs
- Ensure responsive and accessible UI
- **DO NOT** work on backend/Java code - that's handled by a separate backend agent

## Project Overview

**Codebase Q&A** is a web application that allows teams to ask questions about their GitHub repositories using AI. Users can:
- Connect GitHub repositories
- Ask questions about the codebase
- Get AI-generated answers with code citations
- View conversation history

## Tech Stack

### Core
- **React 18** - UI framework
- **TypeScript** - Type safety
- **Vite** - Build tool and dev server
- **React Router** - Client-side routing

### State Management
- **Zustand** - Client state (auth, chat)
- **TanStack Query** - Server state management + caching

### UI & Styling
- **Tailwind CSS v4** - Utility-first styling
- **Lucide React** - Icon library
- **React Markdown** - Markdown rendering
- **React Syntax Highlighter** - Code block highlighting

### API Communication
- **Axios** - HTTP client with interceptors
- **Fetch API** - SSE (Server-Sent Events) for streaming responses

## Project Structure

```
frontend/
├── src/
│   ├── api/                    # API layer
│   │   ├── client.ts          # Axios instance with interceptors
│   │   ├── auth.api.ts        # Authentication endpoints
│   │   ├── repo.api.ts        # Repository management
│   │   ├── query.api.ts       # Question streaming (SSE)
│   │   └── conversation.api.ts # Conversation CRUD
│   ├── components/
│   │   ├── Auth/              # Login, OAuth callback
│   │   ├── Chat/              # Chat window, messages, citations
│   │   ├── Repo/              # Repository list, connect modal
│   │   ├── Sidebar/           # Sidebar, conversation list
│   │   ├── Layout/            # App layout, header
│   │   └── common/            # Reusable components
│   ├── hooks/                 # Custom React hooks
│   │   ├── useAuth.ts         # Authentication hook
│   │   ├── useChat.ts         # Chat/streaming hook
│   │   ├── useRepos.ts        # Repository management
│   │   └── useSSE.ts          # SSE/polling hook
│   ├── stores/                # Zustand stores
│   │   ├── authStore.ts       # Auth state + JWT
│   │   └── chatStore.ts       # Chat state + streaming
│   ├── types/
│   │   └── index.ts           # TypeScript type definitions
│   ├── App.tsx                # Root component + routing
│   └── main.tsx               # Entry point
├── public/                    # Static assets
├── index.html                 # HTML template
├── vite.config.ts             # Vite configuration
├── tailwind.config.js         # Tailwind configuration
├── postcss.config.js          # PostCSS configuration
├── tsconfig.json              # TypeScript configuration
└── package.json               # Dependencies
```

## Key Features to Implement

### Authentication Flow
1. Login page with "Sign in with GitHub" button
2. OAuth callback handler
3. JWT storage in localStorage
4. Protected routes with automatic redirect
5. Persistent authentication (survives page refresh)

### Repository Management
1. List connected repositories with status indicators
2. Connect new repositories via modal
3. Show indexing progress with percentage bar
4. Disconnect repositories with confirmation
5. Trigger manual re-indexing

### Chat Interface
1. Chat window with message history
2. Streaming responses (token-by-token)
3. Code citations with expandable snippets
4. Markdown rendering with syntax highlighting
5. Input bar with Enter-to-send
6. Auto-scroll to latest message

### Conversation Management
1. List conversations in sidebar
2. Create new conversations
3. Load existing conversation history
4. Delete conversations

## API Endpoints (Backend)

### Authentication
- `GET /api/auth/github` - Redirect to GitHub OAuth
- `GET /api/auth/github/callback?code=...` - Handle OAuth callback
- `GET /api/auth/me` - Get current user (protected)
- `POST /api/auth/logout` - Logout

### Repositories
- `POST /api/repos` - Connect repository
- `GET /api/repos` - List user's repositories
- `GET /api/repos/{id}` - Get repository details
- `DELETE /api/repos/{id}` - Disconnect repository
- `POST /api/repos/{id}/reindex` - Trigger re-indexing
- `GET /api/repos/{id}/indexing-status` - Get indexing progress

### Query (SSE Streaming)
- `POST /api/query` - Ask question (returns SSE stream)
  - Events: `citations`, `token`, `done`, `error`

### Conversations
- `GET /api/conversations?repoId={id}` - List conversations
- `GET /api/conversations/{id}` - Get conversation with messages
- `DELETE /api/conversations/{id}` - Delete conversation

## Design System

### Colors
- **Primary:** Blue (#3b82f6) - buttons, links, active states
- **Surface:** Slate grays - backgrounds, borders, text
- **Success:** Green (#22c55e) - ready status
- **Warning:** Yellow (#eab308) - pending/indexing status
- **Error:** Red (#ef4444) - failed status, errors

### Layout
- **Sidebar:** 288px (18rem) fixed width, white background
- **Main area:** Flex-grow, light gray background (#f8fafc)
- **Chat messages:** Max-width 80%, alternating alignment
- **Input bar:** Fixed at bottom, white background

### Typography
- **Font:** System font stack (Inter-like)
- **Body:** 14px (text-sm)
- **Code:** Monospace, 12-13px
- **Headings:** Semibold weight

### Component Patterns
- Rounded corners: `rounded-md` (6px) or `rounded-lg` (8px)
- Borders: `border-surface-200`
- Hover states: `hover:bg-surface-100`
- Focus rings: `focus:ring-2 focus:ring-primary-500`
- Transitions: `transition-colors`

## State Management Patterns

### Auth Store (Zustand)
```typescript
{
  user: User | null,
  token: string | null,
  isAuthenticated: boolean,
  setAuth: (token, user) => void,
  logout: () => void,
  loadFromStorage: () => void
}
```

### Chat Store (Zustand)
```typescript
{
  activeRepoId: string | null,
  activeConversationId: string | null,
  messages: Message[],
  isStreaming: boolean,
  streamingContent: string,
  citations: Citation[],
  // ... actions
}
```

### Server State (TanStack Query)
- Use `useQuery` for GET requests (repos, conversations)
- Use `useMutation` for POST/DELETE (connect, disconnect, reindex)
- Automatic cache invalidation after mutations

## SSE Streaming Pattern

The chat uses Server-Sent Events (SSE) for streaming responses:

1. User sends question via `POST /api/query`
2. Backend streams events:
   - `event: citations` → Show relevant code chunks
   - `event: token` → Append to streaming message
   - `event: done` → Finalize message, save to history
   - `event: error` → Show error message

3. Frontend uses `fetch()` + `ReadableStream` (not axios) to consume SSE

## Important Implementation Notes

### JWT Authentication
- Store JWT in `localStorage` with key `token`
- Attach to all requests via axios interceptor
- On 401 response, clear token and redirect to login

### Protected Routes
- Wrap main app in `<ProtectedRoute>` component
- Check `isAuthenticated` from auth store
- Redirect to `/login` if not authenticated

### Indexing Progress
- Poll `GET /api/repos/{id}/indexing-status` every 3 seconds
- Show progress bar with percentage
- Stop polling when status is COMPLETED or FAILED

### Error Handling
- Show toast notifications for errors
- Handle rate limiting (429) with countdown timer
- Handle network errors with retry option
- Show loading states for all async operations

### Accessibility
- Use semantic HTML elements
- Add ARIA labels where needed
- Ensure keyboard navigation works
- Maintain focus management in modals

## Development Workflow

### Start Dev Server
```bash
npm run dev
```
Runs on `http://localhost:5173` with proxy to backend at `http://localhost:8080`

### Build for Production
```bash
npm run build
```
Outputs to `dist/` directory

### Type Checking
```bash
npm run type-check
```

### Linting
```bash
npm run lint
```

## Environment Variables

Create `.env` file:
```env
VITE_API_URL=http://localhost:8080
```

In production (Vercel), set:
```env
VITE_API_URL=https://your-backend.com
```

## Current Sprint Status

**Sprint 1: Foundation** ✅ COMPLETE
- Task 1.5: Frontend Scaffold ✅
  - Vite + React + TypeScript setup
  - Tailwind CSS v4 configuration
  - Type definitions
  - API client with interceptors
  - Auth store with localStorage
  - LoginPage and OAuthCallbackPage
  - Basic AppLayout
  - Protected routes

**Sprint 2: Core Features** (Current)
- Task 2.7: Chat UI (Frontend) ← YOUR NEXT TASK
  - ChatWindow component
  - MessageBubble with markdown rendering
  - CodeCitation with expandable snippets
  - InputBar with Enter-to-send
  - StreamingMessage component
  - useChat hook with SSE consumption
  - streamQuestion() API function

**Sprint 3: Polish & Deploy**
- Task 3.1: Repository Management UI
- Task 3.3: Error Handling & Edge Cases (frontend part)
- Task 3.5: Deploy Frontend to Vercel
- Task 3.6: Final Polish

## Reference Documentation

All frontend implementation details are in:
- `06-frontend-guide-part1.md` - Setup, Types, API Layer, Stores
- `06-frontend-guide-part2.md` - Hooks, Components (Chat, Layout)
- `06-frontend-guide-part3.md` - Repo Components, Auth, Common
- `10-design-system.md` - UI design specifications
- `09-build-plan.md` - Sprint breakdown and task details

## Communication with Backend

- Backend runs on `http://localhost:8080` (dev)
- Vite proxy forwards `/api/*` requests to backend
- Backend handles CORS configuration
- Backend provides JWT tokens via OAuth callback
- Backend streams responses via SSE for chat

## Testing Checklist

Before marking a task complete, verify:
- [ ] `npm run dev` starts without errors
- [ ] `npm run build` succeeds
- [ ] No TypeScript errors (`npm run type-check`)
- [ ] No console errors in browser
- [ ] All interactive elements work (buttons, inputs, modals)
- [ ] Loading states show during async operations
- [ ] Error states display helpful messages
- [ ] Responsive layout works on different screen sizes
- [ ] Authentication flow works end-to-end
- [ ] Protected routes redirect correctly

## Common Patterns

### API Call with TanStack Query
```typescript
const { data, isLoading, error } = useQuery({
  queryKey: ['repos'],
  queryFn: repoApi.list,
});
```

### Mutation with Cache Invalidation
```typescript
const mutation = useMutation({
  mutationFn: repoApi.connect,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['repos'] });
  },
});
```

### Zustand Store Update
```typescript
const setActiveRepo = useChatStore((s) => s.setActiveRepo);
setActiveRepo(repoId);
```

### Conditional Rendering
```typescript
{isLoading && <Spinner />}
{error && <ErrorMessage error={error} />}
{data && <DataDisplay data={data} />}
```

## Focus Areas

As the frontend agent, focus on:
1. **User Experience** - Smooth interactions, clear feedback
2. **Type Safety** - Proper TypeScript usage
3. **State Management** - Clean separation of concerns
4. **Performance** - Efficient rendering, proper memoization
5. **Accessibility** - Semantic HTML, keyboard navigation
6. **Error Handling** - Graceful degradation, helpful messages
7. **Code Quality** - Reusable components, clean code

## What NOT to Do

- ❌ Don't modify backend code (Java/Spring Boot)
- ❌ Don't change API contracts without coordination
- ❌ Don't add backend dependencies
- ❌ Don't implement backend logic in frontend
- ❌ Don't bypass authentication checks
- ❌ Don't store sensitive data in localStorage (except JWT)
- ❌ Don't make direct database calls

## Next Steps

When ready to continue, start with the next frontend task from Sprint 2 or Sprint 3 based on the current project status.
