# Codebase Q&A — Complete UI Guide for Stitch

> This document is the single source of truth for generating all frontend pages. It uses **Untitled UI React** (Tailwind CSS v4.2 + React Aria) as the component library. The design is light, modern, spacious — inspired by Linear/Vercel aesthetics. Not generic, not corporate.

---

## 1. Tech Stack & Setup

### Core Dependencies

```
React 19 + TypeScript 5.9
Vite (build tool)
Tailwind CSS v4.2
Untitled UI React (copy-paste component library)
@untitledui/icons (icon set)
React Aria v1.16 (accessibility primitives)
TanStack Query (server state)
Zustand (client state)
React Router DOM (routing)
react-markdown + react-syntax-highlighter (markdown rendering)
```

### Untitled UI Installation (Vite)

```bash
npx untitledui@latest init untitled-ui --vite
# Select "brand" as the brand color (purple)
```

Or manual:
```bash
npm install @untitledui/icons react-aria-components tailwindcss-react-aria-components tailwind-merge tailwindcss-animate
```

### File Structure

```
src/
├── components/
│   ├── base/          ← Untitled UI base components (buttons, inputs, etc.)
│   ├── application/   ← Untitled UI application UI components
│   ├── chat/          ← Chat-specific components
│   ├── repo/          ← Repository management components
│   └── layout/        ← Layout shells
├── hooks/
├── stores/
├── api/
├── providers/
│   ├── route-provider.tsx
│   └── theme-provider.tsx
├── styles/
│   ├── globals.css
│   └── theme.css
├── types/
└── utils/
    └── cx.ts
```

---

## 2. Design Tokens & Color System

### Brand Color Override

Replace the default Untitled UI brand purple with a custom violet-indigo blend that feels more refined:

```css
/* In theme.css — override brand colors */
--color-brand-50: rgb(245 243 255);
--color-brand-100: rgb(237 233 254);
--color-brand-200: rgb(221 214 254);
--color-brand-300: rgb(196 181 253);
--color-brand-400: rgb(167 139 250);
--color-brand-500: rgb(139 92 246);
--color-brand-600: rgb(124 58 237);
--color-brand-700: rgb(109 40 217);
--color-brand-800: rgb(91 33 182);
--color-brand-900: rgb(76 29 149);
--color-brand-950: rgb(46 16 101);
```

### Semantic Color Mapping

| Role | Light Mode | Usage |
|------|-----------|-------|
| Page background | `bg-white` | Main canvas |
| Secondary background | `bg-neutral-50` | Sidebar, subtle sections |
| Card surface | `bg-white` | Cards, panels |
| Card hover | `bg-neutral-50` | Hover state on cards |
| Primary action | `bg-brand-600` | Buttons, active indicators |
| Primary hover | `bg-brand-700` | Button hover |
| Text primary | `text-neutral-900` | Headings, body |
| Text secondary | `text-neutral-600` | Descriptions, metadata |
| Text muted | `text-neutral-500` | Placeholders, timestamps |
| Border default | `border-neutral-200` | Cards, dividers |
| Border subtle | `border-neutral-100` | Inner dividers |
| Success | `text-green-600` / `bg-green-50` | Ready status |
| Warning | `text-yellow-600` / `bg-yellow-50` | Pending/indexing |
| Error | `text-red-600` / `bg-red-50` | Failed status |

### Typography

| Element | Class |
|---------|-------|
| Page title | `text-display-xs font-semibold text-neutral-900` |
| Section header | `text-lg font-semibold text-neutral-900` |
| Card title | `text-sm font-medium text-neutral-900` |
| Body | `text-sm text-neutral-600` |
| Caption/meta | `text-xs text-neutral-500` |
| Code | `font-mono text-sm` |

### Spacing & Layout

| Token | Value |
|-------|-------|
| Sidebar width | `w-72` (288px) |
| Content max-width | `max-w-[1100px]` |
| Reading max-width | `max-w-[780px]` |
| Section gap | `gap-6` (24px) |
| Card gap | `gap-4` (16px) |
| Card padding | `p-4` (16px) |
| Border radius (cards) | `rounded-xl` (12px) |
| Border radius (buttons) | `rounded-lg` (8px) |
| Border radius (tags) | `rounded-md` (6px) |

### Shadows

```
Cards: shadow-xs (subtle, barely visible)
Modals: shadow-xl
Dropdowns: shadow-lg
Hover lift: shadow-sm + translate-y-[-1px]
```

---

## 3. Untitled UI Components Used Per Feature

### Base Components (from Untitled UI)

| Component | Import Path | Used For |
|-----------|-------------|----------|
| `Button` | `@/components/base/buttons/button` | All actions |
| `Input` | `@/components/base/input/input` | Text fields |
| `Textarea` | `@/components/base/textarea/textarea` | Chat input |
| `Select` | `@/components/base/select/select` | Repo selector |
| `Badges` | `@/components/base/badges/badge` | Status pills, tags |
| `Avatars` | `@/components/base/avatars/avatar` | User avatars |
| `Toggles` | `@/components/base/toggles/toggle` | Settings switches |
| `Tooltips` | `@/components/base/tooltips/tooltip` | Icon hints |
| `Tags` | `@/components/base/tags/tag` | Repo labels, file types |
| `Dropdowns` | `@/components/base/dropdowns/dropdown` | Context menus |
| `Progress Indicators` | `@/components/base/progress-indicators` | Indexing progress |

### Application UI Components (from Untitled UI)

| Component | Import Path | Used For |
|-----------|-------------|----------|
| `Sidebar Navigations` | `@/components/application/sidebar-navigations` | Main app sidebar |
| `Messaging` | `@/components/application/messaging` | Chat messages |
| `Code Snippets` | `@/components/application/code-snippets` | Citation code blocks |
| `Empty States` | `@/components/application/empty-states` | No repos, no conversations |
| `Modals` | `@/components/application/modals` | Connect repo, confirmations |
| `Alerts` | `@/components/application/alerts` | Error/success notifications |
| `Loading Indicators` | `@/components/application/loading-indicators` | Streaming, fetching |
| `Command Menus` | `@/components/application/command-menus` | Quick search (Cmd+K) |
| `Card Headers` | `@/components/application/card-headers` | Section headers |
| `Tabs` | `@/components/application/tabs` | Settings sections |
| `Activity Feeds` | `@/components/application/activity-feeds` | Conversation history |
| `Metrics` | `@/components/application/metrics` | Repo stats |
| `Content Dividers` | `@/components/application/content-dividers` | Section separators |
| `Notifications` | `@/components/application/notifications` | Toast messages |

### Shared Page Examples (from Untitled UI)

| Page | Reference |
|------|-----------|
| Login | `Log in pages` — use the minimal centered variant |
| Sign up | Not needed (GitHub OAuth only) |
| 404 | `404 sections` — use the clean minimal variant |

### Icons (from @untitledui/icons)

```tsx
import {
  MessageSquare01, // Conversations
  FolderCode, // Repositories
  Home01, // Home/Dashboard
  Settings01, // Settings
  Search, // Search
  Plus, // Add new
  Send01, // Send message
  GitBranch01, // Branch
  File06, // File reference
  Code02, // Code block
  RefreshCw01, // Re-index
  Trash01, // Delete
  LogOut01, // Logout
  ChevronDown, // Expand
  ChevronRight, // Collapse
  Check, // Success
  AlertCircle, // Error
  Loader01, // Loading
  Copy01, // Copy code
  ExternalLink01, // Open in GitHub
  Clock, // Timestamp
  Hash01, // Line numbers
  Star01, // Favorite
} from "@untitledui/icons";
```


---

## 4. Global Layout

### App Shell

The app uses a sidebar + main content layout. No top navbar. Clean and spacious.

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│  ┌─────────┐  ┌──────────────────────────────────┐  │
│  │         │  │                                  │  │
│  │ Sidebar │  │         Main Content             │  │
│  │  (w-72) │  │      (flex-1, centered)          │  │
│  │         │  │                                  │  │
│  │         │  │                                  │  │
│  │         │  │                                  │  │
│  │         │  │                                  │  │
│  └─────────┘  └──────────────────────────────────┘  │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### Sidebar Structure

Use Untitled UI `Sidebar Navigations` component. Customize with:

```
┌─────────────────────────┐
│  Logo + App Name        │  ← "Codebase Q&A" text-sm font-bold
│  (p-4, border-b)        │
├─────────────────────────┤
│                         │
│  REPOSITORIES (label)   │  ← text-xs uppercase text-neutral-500
│  ┌───────────────────┐  │
│  │ ● repo/name       │  │  ← RepoCard (active = brand bg tint)
│  │ ○ repo/other      │  │
│  └───────────────────┘  │
│  [+ Connect Repo]       │  ← Button, size="sm", color="tertiary"
│                         │
├─────────────────────────┤
│                         │
│  CONVERSATIONS (label)  │  ← Only shows when a repo is selected
│  [+ New conversation]   │
│  ┌───────────────────┐  │
│  │ 💬 Conv title...  │  │  ← Truncated, active = brand tint
│  │ 💬 Conv title...  │  │
│  └───────────────────┘  │
│                         │
├─────────────────────────┤
│  ┌───────────────────┐  │
│  │ 👤 username       │  │  ← Avatar + name + logout button
│  │    [Logout]       │  │
│  └───────────────────┘  │
└─────────────────────────┘
```

### Sidebar Behavior
- Fixed height `h-screen`, `overflow-y-auto`
- Background: `bg-white` with `border-r border-neutral-200`
- Active repo: `bg-brand-50 border-l-2 border-brand-600`
- Active conversation: `bg-neutral-50 text-neutral-900 font-medium`
- Hover states: `hover:bg-neutral-50`
- Transition: `transition-colors duration-150`

### Responsive
- Desktop (>1024px): Sidebar always visible
- Tablet (768-1024px): Sidebar as slideout (use Untitled UI `Slideout Menus`)
- Mobile (<768px): Full-screen slideout with hamburger trigger

---

## 5. Routes & Pages

| Route | Page | Description |
|-------|------|-------------|
| `/login` | LoginPage | GitHub OAuth sign-in |
| `/auth/callback` | OAuthCallbackPage | Handles OAuth redirect |
| `/` | HomePage | Dashboard / welcome state |
| `/chat` | ChatPage | Main chat interface (default after repo selected) |
| `/chat/:conversationId` | ChatPage | Existing conversation |
| `/repos` | ReposPage | Repository management |
| `/settings` | SettingsPage | User preferences |
| `*` | NotFoundPage | 404 page |


---

## 6. Page Specifications

### 6.1 Login Page (`/login`)

**Reference:** Untitled UI `Log in pages` — minimal centered variant

**Layout:** Full-screen centered, `bg-white`

**Content:**
```
┌──────────────────────────────────────────────┐
│                                              │
│              [Brand Icon]                    │  ← FolderCode icon, brand-600, 48px
│                                              │
│           Codebase Q&A                       │  ← text-display-xs font-semibold
│                                              │
│    Ask questions about any codebase.         │  ← text-md text-neutral-600
│    No local clone required.                  │
│                                              │
│    ┌──────────────────────────────┐          │
│    │  [GitHub Icon]  Continue     │          │  ← Button, size="xl", full-width
│    │     with GitHub              │          │     color="primary" (brand-600)
│    └──────────────────────────────┘          │
│                                              │
│    By continuing, you agree to our           │  ← text-xs text-neutral-500
│    Terms of Service.                         │
│                                              │
└──────────────────────────────────────────────┘
```

**Design Notes:**
- No card/box around the content — just centered on white
- Subtle gradient dot pattern in background (CSS only, very faint neutral-100 dots)
- Button uses `@untitledui/icons` GitHub icon or a custom SVG
- Max-width of content area: `max-w-sm` (384px)
- Spacing between elements: `gap-6`

**Untitled UI Components:**
- `Button` (size="xl", full width, with `iconLeading`)
- `FeaturedIcon` (for the brand icon at top, style="light", color="brand")

---

### 6.2 OAuth Callback Page (`/auth/callback`)

**Layout:** Full-screen centered, `bg-white`

**Content:**
```
┌──────────────────────────────────────────────┐
│                                              │
│           [Loading Indicator]                │  ← Untitled UI Loading Indicator
│                                              │
│           Authenticating...                  │  ← text-sm text-neutral-500
│                                              │
└──────────────────────────────────────────────┘
```

**Untitled UI Components:**
- `Loading Indicators` — spinner variant, brand color

---

### 6.3 Home / Dashboard Page (`/`)

**When:** User is authenticated but no repo is selected.

**Layout:** Main content area (no sidebar active state)

**Content:**
```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  Welcome back, {username} 👋                                 │  ← text-display-xs
│                                                              │
│  ┌─────────────────────────────────────────────────────┐     │
│  │  ASK ANYTHING ABOUT YOUR CODEBASE                   │     │
│  │                                                     │     │
│  │  ┌─────────────────────────────────────────────┐    │     │
│  │  │ Select a repository...              [▼]     │    │     │  ← Select component
│  │  └─────────────────────────────────────────────┘    │     │
│  │                                                     │     │
│  │  ┌─────────────────────────────────────────────┐    │     │
│  │  │ Ask a question about the codebase...        │    │     │  ← Textarea
│  │  │                                             │    │     │
│  │  └─────────────────────────────────────────────┘    │     │
│  │                                                     │     │
│  │  Suggestions:                                       │     │
│  │  [How does auth work?] [Explain the DB schema]      │     │  ← Tag/pill buttons
│  │  [What patterns are used?] [Show API endpoints]     │     │
│  │                                                     │     │
│  │                              [Ask →]                │     │  ← Button primary
│  └─────────────────────────────────────────────────────┘     │
│                                                              │
│  ── Recent Conversations ──────────────────────────────      │
│                                                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                   │
│  │ Conv 1   │  │ Conv 2   │  │ Conv 3   │                   │  ← Card grid
│  │ repo/x   │  │ repo/y   │  │ repo/x   │                   │
│  │ 3 msgs   │  │ 8 msgs   │  │ 2 msgs   │                   │
│  │ 2h ago   │  │ 1d ago   │  │ 3d ago   │                   │
│  └──────────┘  └──────────┘  └──────────┘                   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Design Notes:**
- The "Ask" card has a very subtle `border border-neutral-200 rounded-xl shadow-xs`
- Top border accent: `border-t-2 border-brand-500` on the ask card
- Suggestion pills use Untitled UI `Tags` with `color="brand"` variant
- Recent conversations use a 3-column grid (`grid grid-cols-3 gap-4`)
- Each conversation card: `rounded-xl border border-neutral-200 p-4 hover:shadow-sm hover:border-brand-200 transition-all`

**Untitled UI Components:**
- `Select` (for repo picker)
- `Textarea` (for question input)
- `Button` (primary, with `iconTrailing={Send01}`)
- `Tags` (for suggestion pills, clickable)
- `Card Headers` (for "Recent Conversations" section)
- `Empty States` (when no conversations exist)

---

### 6.4 Chat Page (`/chat` and `/chat/:conversationId`)

**This is the core page.** Most time is spent here.

**Layout:** Full height, flex column

```
┌──────────────────────────────────────────────────────────────┐
│  HEADER BAR                                                  │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  [FolderCode] owner/repo-name  •  branch: main        │  │  ← Breadcrumb-style
│  │                                    [⟳ Re-index]        │  │
│  └────────────────────────────────────────────────────────┘  │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  MESSAGE AREA (flex-1, overflow-y-auto, p-6)                 │
│                                                              │
│  ┌─ User Message ────────────────────────────────────────┐   │
│  │  How does the authentication middleware work?         │   │
│  └───────────────────────────────────────────── 10:30am ─┘   │
│                                                              │
│  ┌─ Assistant Message ───────────────────────────────────┐   │
│  │                                                       │   │
│  │  ┌─ Sources ────────────────────────────────────┐     │   │
│  │  │  📄 src/auth/middleware.ts:15-42             │     │   │
│  │  │  📄 src/config/security.ts:8-24             │     │   │
│  │  └─────────────────────────────────────────────┘     │   │
│  │                                                       │   │
│  │  The authentication middleware in                     │   │
│  │  `src/auth/middleware.ts` uses JWT tokens to...       │   │
│  │                                                       │   │
│  │  ```typescript                                        │   │
│  │  export function authenticateUser(req, res) {         │   │
│  │    const token = req.headers.authorization;           │   │
│  │  }                                                    │   │
│  │  ```                                                  │   │
│  │                                                       │   │
│  └───────────────────────────────────────────── 10:31am ─┘   │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│  INPUT BAR (sticky bottom, bg-white, border-t, p-4)          │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  ┌──────────────────────────────────────────┐  [Send]  │  │
│  │  │  Ask a follow-up question...             │          │  │
│  │  └──────────────────────────────────────────┘          │  │
│  │  ⌘+Enter to send  •  Shift+Enter for new line         │  │
│  └────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────┘
```

**Message Bubbles — Design:**

User messages:
- Aligned right
- `bg-brand-600 text-white rounded-2xl rounded-br-md px-4 py-3`
- Max-width: `max-w-[70%]`
- Timestamp below: `text-xs text-neutral-400 mt-1 text-right`

Assistant messages:
- Aligned left
- `bg-white border border-neutral-200 rounded-2xl rounded-bl-md px-5 py-4`
- Max-width: `max-w-[80%]`
- Contains rendered markdown (prose styling)
- Timestamp below: `text-xs text-neutral-400 mt-1`

**Citations/Sources Block:**
- Appears at the top of assistant messages (before the text)
- Container: `bg-neutral-50 rounded-lg p-3 mb-3 border border-neutral-100`
- Each citation row: `flex items-center gap-2 py-1.5`
  - `File06` icon (16px, `text-brand-600`)
  - File path in `font-mono text-xs text-brand-700`
  - Line range: `text-xs text-neutral-500`
  - Expand chevron on the right
- Clicking a citation expands to show the code snippet using Untitled UI `Code Snippets`

**Streaming State:**
- Show a pulsing dot indicator (`●●●`) while waiting for first token
- Once tokens arrive, render them progressively
- Use Untitled UI `Loading Indicators` (typing dots variant)
- "Searching codebase..." label with spinner while citations load

**Empty State (new conversation):**
- Centered in message area
- Large `MessageSquare01` icon (48px, `text-neutral-300`)
- "Ask anything about this codebase" — `text-lg text-neutral-500`
- 3-4 suggestion pills below (same as home page)

**Input Bar:**
- Use Untitled UI `Textarea` (auto-resize, max 5 rows)
- Send button: `Button` with `Send01` icon, `color="primary"`, `size="md"`
- Disabled state when streaming or no repo selected
- Keyboard hint: `text-xs text-neutral-400`

**Untitled UI Components:**
- `Messaging` (adapt the message layout patterns)
- `Code Snippets` (for citation expansion, with Shiki syntax highlighting)
- `Loading Indicators` (typing dots for streaming)
- `Textarea` (auto-resize input)
- `Button` (send action)
- `Badges` (for source count indicator)
- `Tags` (for suggestion pills)
- `Empty States` (when no messages)
- `Tooltips` (on action buttons)


---

### 6.5 Repositories Page (`/repos`)

**Layout:** Main content area with page header

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  Repositories                          [+ Connect Repo]      │  ← Page header + Button
│  Manage your connected repositories                          │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  CONNECTED (3)                                          │ │
│  ├─────────────────────────────────────────────────────────┤ │
│  │                                                         │ │
│  │  ┌─────────────────────────────────────────────────┐    │ │
│  │  │  [FolderCode]  octocat/hello-world              │    │ │
│  │  │                                                 │    │ │
│  │  │  Branch: main  •  245 chunks  •  Ready ●        │    │ │
│  │  │  Last indexed: 2 hours ago                      │    │ │
│  │  │                                                 │    │ │
│  │  │  [Re-index]  [Open in GitHub ↗]  [Disconnect]   │    │ │
│  │  └─────────────────────────────────────────────────┘    │ │
│  │                                                         │ │
│  │  ┌─────────────────────────────────────────────────┐    │ │
│  │  │  [FolderCode]  octocat/spoon-knife              │    │ │
│  │  │                                                 │    │ │
│  │  │  Branch: main  •  Indexing... ◌                 │    │ │
│  │  │  ████████████░░░░░░░░  54/120 files (45%)       │    │ │
│  │  │                                                 │    │ │
│  │  └─────────────────────────────────────────────────┘    │ │
│  │                                                         │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Repo Card Design:**
- Container: `rounded-xl border border-neutral-200 p-5 hover:border-neutral-300 transition-colors`
- Status dot: colored circle (green=ready, yellow=indexing, red=failed)
- Stats row: `flex items-center gap-3 text-xs text-neutral-500`
- Action buttons: `Button` with `color="tertiary"`, `size="sm"`
- Disconnect button: `color="tertiary"` with red text on hover
- Progress bar (when indexing): Untitled UI `Progress Indicators`, `color="brand"`, height 4px

**Connect Repo Modal:**
Use Untitled UI `Modals` component:

```
┌──────────────────────────────────────────┐
│  Connect Repository                  [✕] │
├──────────────────────────────────────────┤
│                                          │
│  Repository                              │
│  ┌────────────────────────────────────┐  │
│  │  owner/repository-name             │  │  ← Input with placeholder
│  └────────────────────────────────────┘  │
│  Format: owner/repo-name                 │
│                                          │
│  Branch (optional)                       │
│  ┌────────────────────────────────────┐  │
│  │  main                              │  │  ← Input
│  └────────────────────────────────────┘  │
│  Leave empty for default branch          │
│                                          │
├──────────────────────────────────────────┤
│            [Cancel]  [Connect & Index]   │  ← Button group
└──────────────────────────────────────────┘
```

**Untitled UI Components:**
- `Page Headers` (title + description + action button)
- `Card Headers` (for section label "CONNECTED")
- `Progress Indicators` (indexing bar)
- `Badges` (status: Ready, Indexing, Failed)
- `Modals` (connect repo dialog)
- `Input` (repo name, branch fields)
- `Button` (actions)
- `Metrics` (chunk count, file count stats)
- `Empty States` (when no repos connected)

**Empty State (no repos):**
- Centered illustration (use Untitled UI `Illustrations` or `Empty States`)
- "No repositories connected"
- "Connect a GitHub repository to start asking questions about your codebase"
- [+ Connect Repository] button, primary

---

### 6.6 Settings Page (`/settings`)

**Reference:** Untitled UI `Settings pages` layout

**Layout:** Content area with vertical tabs or sections

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│  Settings                                                    │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  [Profile]  [Preferences]  [API Usage]               │    │  ← Tabs
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ── Profile ─────────────────────────────────────────────    │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  Avatar    [GitHub Avatar]                           │    │
│  │  Username  akashsoni                                 │    │
│  │  Email     akash@example.com                         │    │
│  │  Joined    January 10, 2024                          │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ── Preferences ─────────────────────────────────────────    │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  Theme              [Light ▼]                        │    │  ← Select
│  │  Code theme         [One Dark ▼]                     │    │
│  │  Show line numbers  [Toggle: ON]                     │    │  ← Toggle
│  │  Auto-expand citations  [Toggle: OFF]                │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ── API Usage ───────────────────────────────────────────    │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  Queries today     8 / 20                            │    │  ← Progress bar
│  │  Queries this hour 3 / 20                            │    │
│  │  Repos connected   3 / 10                            │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
│  ── Danger Zone ─────────────────────────────────────────    │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐    │
│  │  [Delete Account]                                    │    │  ← Button, destructive
│  │  This will disconnect all repos and delete data.     │    │
│  └──────────────────────────────────────────────────────┘    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Untitled UI Components:**
- `Tabs` (section navigation)
- `Avatars` (profile picture)
- `Select` (theme picker)
- `Toggles` (boolean preferences)
- `Progress Indicators` (usage bars)
- `Metrics` (usage stats)
- `Button` (destructive variant for danger zone)
- `Content Dividers` (between sections)
- `Alerts` (confirmation for destructive actions)

---

### 6.7 404 Page

**Reference:** Untitled UI `404 sections` — minimal variant

```
┌──────────────────────────────────────────────────────────────┐
│                                                              │
│                         404                                   │  ← text-display-lg, brand-600
│                                                              │
│              Page not found                                   │  ← text-xl text-neutral-900
│                                                              │
│     The page you're looking for doesn't exist                │  ← text-md text-neutral-500
│     or has been moved.                                       │
│                                                              │
│              [← Back to Home]                                │  ← Button, primary
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

**Untitled UI Components:**
- `404 Sections` (use the clean, minimal variant)
- `Button` (navigation back)


---

## 7. Component Specifications

### 7.1 Citation Card (Custom Component)

The expandable code citation shown in assistant messages.

**Collapsed State:**
```tsx
<div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-neutral-50 border border-neutral-100 hover:bg-neutral-100 cursor-pointer transition-colors">
  <File06 className="size-4 text-brand-600 shrink-0" />
  <span className="font-mono text-xs text-brand-700 truncate">
    src/auth/middleware.ts
  </span>
  <span className="text-xs text-neutral-400">:15-42</span>
  <Badge size="sm" color="brand">authenticateUser</Badge>
  <ChevronRight className="size-3.5 text-neutral-400 ml-auto" />
</div>
```

**Expanded State:**
- Chevron rotates to down
- Below the row, show Untitled UI `Code Snippets` component
- Code block with syntax highlighting (Shiki), line numbers starting at `startLine`
- Copy button in top-right of code block
- "Open in GitHub" link below code block

### 7.2 Streaming Indicator

While the AI is generating a response:

```tsx
<div className="flex items-start gap-3 max-w-[80%]">
  <div className="bg-white border border-neutral-200 rounded-2xl rounded-bl-md px-5 py-4">
    {/* Citations loading */}
    {showCitationsLoading && (
      <div className="flex items-center gap-2 text-xs text-neutral-500 mb-3">
        <Loader01 className="size-3.5 animate-spin text-brand-600" />
        <span>Searching codebase...</span>
      </div>
    )}
    
    {/* Streaming text */}
    <div className="prose prose-sm">
      {streamingContent}
    </div>
    
    {/* Typing indicator (before first token) */}
    {!streamingContent && (
      <div className="flex gap-1">
        <span className="size-2 rounded-full bg-brand-400 animate-bounce [animation-delay:0ms]" />
        <span className="size-2 rounded-full bg-brand-400 animate-bounce [animation-delay:150ms]" />
        <span className="size-2 rounded-full bg-brand-400 animate-bounce [animation-delay:300ms]" />
      </div>
    )}
  </div>
</div>
```

### 7.3 Repo Status Badge

Use Untitled UI `Badges` with semantic colors:

| Status | Badge Config |
|--------|-------------|
| READY | `color="success"`, dot indicator, text "Ready" |
| INDEXING | `color="warning"`, animated dot, text "Indexing..." |
| PENDING | `color="warning"`, text "Queued" |
| FAILED | `color="error"`, text "Failed" |

### 7.4 Command Menu (Cmd+K)

Use Untitled UI `Command Menus` for quick navigation:

**Trigger:** `Cmd+K` (Mac) / `Ctrl+K` (Windows)

**Sections:**
1. **Repositories** — list connected repos, click to select
2. **Conversations** — recent conversations, click to open
3. **Actions** — "Connect new repo", "New conversation", "Settings"

**Design:**
- Overlay with backdrop blur
- Search input at top with `Search` icon
- Results grouped by section with `text-xs uppercase text-neutral-500` labels
- Active item: `bg-neutral-50 rounded-lg`
- Keyboard navigation support (built into React Aria)

### 7.5 Toast Notifications

Use Untitled UI `Notifications` (toast variant):

| Event | Type | Message |
|-------|------|---------|
| Repo connected | success | "Repository connected. Indexing started." |
| Repo disconnected | info | "Repository disconnected." |
| Indexing complete | success | "Indexing complete. 245 chunks indexed." |
| Indexing failed | error | "Indexing failed: {error message}" |
| Rate limited | warning | "Rate limit reached. Try again in {n} seconds." |
| Network error | error | "Connection lost. Retrying..." |

**Position:** Top-right, stacked
**Duration:** 5 seconds (errors persist until dismissed)
**Animation:** Slide in from right, fade out

---

## 8. Interaction Patterns

### 8.1 Chat Flow

1. User selects a repo from sidebar → repo becomes active (brand highlight)
2. Conversation list loads for that repo
3. User types question in input bar → press Enter or click Send
4. User message appears immediately (optimistic)
5. Streaming indicator shows (bouncing dots)
6. Citations arrive first → rendered in a collapsible block
7. Tokens stream in → rendered progressively as markdown
8. "Done" event → message finalized, streaming indicator removed
9. Conversation ID set (if new) → sidebar updates

### 8.2 Repo Connection Flow

1. User clicks "+ Connect Repo" → Modal opens
2. User types `owner/repo-name` and optional branch
3. Click "Connect & Index" → Modal closes
4. Repo appears in sidebar with "Queued" badge
5. Status changes to "Indexing..." with progress bar
6. Progress updates via polling (every 3s)
7. On complete → badge changes to "Ready", toast notification
8. Repo is now selectable for chat

### 8.3 Hover & Focus States

| Element | Hover | Focus |
|---------|-------|-------|
| Buttons (primary) | `bg-brand-700` | `ring-2 ring-brand-500 ring-offset-2` |
| Buttons (tertiary) | `bg-neutral-50` | `ring-2 ring-brand-500 ring-offset-2` |
| Cards | `border-neutral-300 shadow-sm -translate-y-px` | — |
| Sidebar items | `bg-neutral-50` | `bg-neutral-100` |
| Links | `text-brand-700 underline` | `ring-2 ring-brand-500` |
| Input fields | — | `ring-2 ring-brand-500 border-brand-500` |

### 8.4 Loading States

| Context | Pattern |
|---------|---------|
| Page load | Skeleton placeholders (neutral-100 shimmer) |
| Chat streaming | Bouncing dots → progressive text |
| Repo list | Skeleton cards (3 placeholder cards) |
| Conversation list | Skeleton lines (4 placeholder items) |
| Button action | Button shows spinner, text changes to "Loading..." |

### 8.5 Error States

| Context | Pattern |
|---------|---------|
| Failed to load repos | Untitled UI `Alerts` (error variant) with retry button |
| Chat error mid-stream | Error message in red bubble with retry option |
| Network offline | Top banner (Untitled UI `Banners`) with "You're offline" |
| 401 Unauthorized | Redirect to `/login` |
| Rate limited | Toast notification + disabled input with countdown |


---

## 9. Animations & Micro-interactions

### Transitions (Global)
```css
/* Default transition for all interactive elements */
transition: all 150ms ease;
```

### Specific Animations

| Element | Animation |
|---------|-----------|
| Page transitions | Fade in (`opacity 0→1`, 200ms) |
| Modal open | Scale up from 95% + fade in (200ms, ease-out) |
| Modal close | Scale down to 95% + fade out (150ms, ease-in) |
| Toast enter | Slide from right (300ms, spring) |
| Toast exit | Fade out + slide right (200ms) |
| Sidebar item select | Background color transition (150ms) |
| Card hover lift | `transform: translateY(-1px)` (150ms) |
| Message appear | Fade in + slide up 8px (200ms) |
| Streaming dots | Staggered bounce (infinite, 600ms per cycle) |
| Progress bar | Width transition (300ms, ease-in-out) |
| Citation expand | Height auto with `grid-rows` trick (200ms) |
| Skeleton shimmer | Left-to-right gradient sweep (1.5s, infinite) |

### Scroll Behavior
- Chat messages: `scroll-behavior: smooth` on container
- Auto-scroll to bottom on new message (only if user is near bottom)
- If user has scrolled up, show "↓ New messages" pill at bottom

---

## 10. Accessibility Requirements

All components from Untitled UI are built on React Aria, which handles:
- Keyboard navigation (Tab, Arrow keys, Enter, Escape)
- Screen reader announcements (ARIA labels, live regions)
- Focus management (focus trapping in modals, focus restoration)
- Color contrast (WCAG AA minimum)

### Additional Requirements

| Feature | Implementation |
|---------|---------------|
| Skip to content | Hidden link at top of page, visible on focus |
| Chat messages | `role="log"` with `aria-live="polite"` for new messages |
| Streaming | `aria-busy="true"` on message container while streaming |
| Status changes | `aria-live="assertive"` for repo status updates |
| Modal focus trap | Handled by React Aria `Modal` component |
| Keyboard shortcuts | `Cmd+K` for command menu, `Cmd+Enter` to send |
| Reduced motion | Respect `prefers-reduced-motion` — disable animations |
| Dark mode | Respect `prefers-color-scheme` as default |

---

## 11. Dark Mode

Untitled UI handles dark mode via the `.dark-mode` class on `<html>`.

**Key overrides for our brand:**
- Page background: `neutral-950`
- Card surface: `neutral-900`
- Card border: `neutral-800`
- User message bubble: `bg-brand-700` (slightly muted)
- Assistant message: `bg-neutral-900 border-neutral-800`
- Code blocks: `bg-neutral-950` (near black)
- Text primary: `neutral-50`
- Text secondary: `neutral-400`

**Toggle:** Use the `ThemeProvider` from Untitled UI Vite integration. Expose toggle in Settings page and optionally in sidebar footer.

---

## 12. Design Principles (for Stitch)

These are the non-negotiable aesthetic rules:

1. **White space is a feature.** Don't fill every pixel. Let content breathe. Generous padding everywhere.

2. **Color is earned.** The page is predominantly white/neutral. Brand purple appears only for: primary buttons, active states, links, and accent borders. Never as large background fills.

3. **Borders over shadows.** Use `border-neutral-200` for card definition. Shadows are `shadow-xs` (barely visible) — only `shadow-lg` for elevated elements like modals/dropdowns.

4. **Typography hierarchy is king.** Size + weight differences create hierarchy, not color or decoration. Headings are `font-semibold`, body is `font-normal`, metadata is smaller + lighter color.

5. **Rounded but not bubbly.** `rounded-xl` for cards, `rounded-lg` for buttons/inputs, `rounded-2xl` for chat bubbles. Never `rounded-full` on containers.

6. **Subtle motion.** Transitions are 150ms. Nothing bounces or overshoots. Hover states are color shifts, not scale changes (except the -1px lift on cards).

7. **Monospace for code, always.** File paths, line numbers, code snippets — all in `font-mono`. Never render code in the body font.

8. **No decorative elements.** No gradients (except the faint dot pattern on login), no illustrations in the app shell, no emoji in UI labels. The content IS the decoration.

9. **Density varies by context.** Sidebar is compact (tight spacing). Chat is spacious (generous message gaps). Settings is medium density.

10. **Feels like a tool, not a toy.** Think Linear, Raycast, Vercel Dashboard. Professional, fast, purposeful. Every element has a job.

---

## 13. File-by-File Component Map

For Stitch to generate each file:

```
src/
├── App.tsx                          → Router setup, providers
├── main.tsx                         → Entry point with providers
├── styles/
│   ├── globals.css                  → Tailwind imports + custom utilities
│   └── theme.css                    → Untitled UI theme with brand overrides
├── providers/
│   ├── route-provider.tsx           → React Aria router integration
│   └── theme-provider.tsx           → Dark mode provider
├── components/
│   ├── layout/
│   │   ├── AppLayout.tsx            → Sidebar + main content shell
│   │   ├── Sidebar.tsx              → Full sidebar with repos + conversations
│   │   └── AuthGuard.tsx            → Protected route wrapper
│   ├── chat/
│   │   ├── ChatPage.tsx             → Full chat page (messages + input)
│   │   ├── MessageList.tsx          → Scrollable message container
│   │   ├── UserMessage.tsx          → User bubble (right-aligned, brand)
│   │   ├── AssistantMessage.tsx     → Assistant bubble (left, white, markdown)
│   │   ├── CitationBlock.tsx        → Expandable source citations
│   │   ├── CitationCard.tsx         → Single citation row
│   │   ├── StreamingIndicator.tsx   → Bouncing dots + progressive text
│   │   ├── ChatInput.tsx            → Textarea + send button
│   │   ├── ChatEmptyState.tsx       → No messages placeholder
│   │   └── SuggestionPills.tsx      → Clickable question suggestions
│   ├── repo/
│   │   ├── ReposPage.tsx            → Full repos management page
│   │   ├── RepoCard.tsx             → Single repo card with actions
│   │   ├── RepoStatusBadge.tsx      → Status badge (Ready/Indexing/Failed)
│   │   ├── ConnectRepoModal.tsx     → Modal for connecting new repo
│   │   ├── IndexingProgress.tsx     → Progress bar for indexing
│   │   └── RepoEmptyState.tsx       → No repos placeholder
│   ├── home/
│   │   ├── HomePage.tsx             → Dashboard with ask card + recent convos
│   │   ├── AskCard.tsx              → The main "ask a question" card
│   │   └── RecentConversations.tsx  → Grid of recent conversation cards
│   ├── auth/
│   │   ├── LoginPage.tsx            → GitHub OAuth login
│   │   └── OAuthCallbackPage.tsx    → Callback handler
│   ├── settings/
│   │   └── SettingsPage.tsx         → User settings with tabs
│   ├── shared/
│   │   ├── CommandMenu.tsx          → Cmd+K quick navigation
│   │   ├── NotFoundPage.tsx         → 404 page
│   │   └── SkeletonLoaders.tsx      → Reusable skeleton components
│   └── base/                        → Untitled UI base components (auto-added via CLI)
├── hooks/
│   ├── useAuth.ts                   → Auth state + OAuth callback
│   ├── useChat.ts                   → Chat logic + streaming
│   ├── useRepos.ts                  → Repo CRUD operations
│   └── useSSE.ts                    → Indexing progress polling
├── stores/
│   ├── authStore.ts                 → Zustand auth state
│   └── chatStore.ts                 → Zustand chat state
├── api/
│   ├── client.ts                    → Axios instance with interceptors
│   ├── auth.api.ts                  → Auth endpoints
│   ├── repo.api.ts                  → Repo endpoints
│   ├── query.api.ts                 → SSE streaming query
│   └── conversation.api.ts          → Conversation CRUD
├── types/
│   └── index.ts                     → All TypeScript interfaces
└── utils/
    ├── cx.ts                        → Tailwind merge utility
    └── markdown.ts                  → Markdown rendering config
```

---

## 14. API Integration Summary

| Page | API Calls | State |
|------|-----------|-------|
| LoginPage | `GET /api/auth/github` (redirect) | — |
| OAuthCallback | `GET /api/auth/github/callback?code=` | Sets auth store |
| HomePage | `GET /api/repos`, `GET /api/conversations` | TanStack Query |
| ChatPage | `POST /api/query` (SSE stream), `GET /api/conversations/:id` | Zustand + streaming |
| ReposPage | `GET /api/repos`, `POST /api/repos`, `DELETE /api/repos/:id`, `POST /api/repos/:id/reindex` | TanStack Query + mutations |
| SettingsPage | `GET /api/auth/me` | TanStack Query |
| Sidebar | `GET /api/repos`, `GET /api/conversations?repoId=` | TanStack Query |

---

## 15. Summary for Stitch

**Generate a Vite + React + TypeScript project using Untitled UI React with:**

1. Purple/violet brand color (`brand-600: #7C3AED`)
2. Light, white-dominant design with neutral grays
3. Sidebar navigation layout (no top navbar)
4. 7 pages: Login, OAuth Callback, Home, Chat, Repos, Settings, 404
5. Real-time chat with SSE streaming and markdown rendering
6. Expandable code citations with syntax highlighting
7. Repository management with indexing progress
8. Command menu (Cmd+K) for quick navigation
9. Toast notifications for async events
10. Dark mode support via Untitled UI theme system
11. Fully accessible (React Aria foundation)
12. Responsive (sidebar collapses on tablet/mobile)

**The result should feel like Linear meets ChatGPT — clean, fast, professional, with just enough personality from the purple accent to feel distinctive.**
