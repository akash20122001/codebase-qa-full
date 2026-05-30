# Codebase Q&A — Frontend Coding Best Practices

> Production-grade patterns for building the UI. Brief hints, not full implementations.

---

## 1. Project Structure

- **Feature-based folders**, not type-based. Group by domain (`chat/`, `repo/`, `auth/`) not by role (`components/`, `hooks/`).
- Co-locate tests, types, and styles with their component.
- Barrel exports (`index.ts`) only at feature boundaries, not per-component — avoids circular deps and tree-shaking issues.
- Keep `components/base/` as a thin wrapper layer over Untitled UI — never modify Untitled UI source directly. Wrap and extend.

---

## 2. Component Patterns

- **Single responsibility.** One component = one job. If it has an `&&` in the name ("MessageListAndInput"), split it.
- **Composition over props.** Prefer `children` and slots over 15 boolean props. Use compound components for complex UI (e.g., `<Citation.Root>`, `<Citation.Code>`, `<Citation.Header>`).
- **Container/Presentational split.** Containers fetch data and manage state. Presentational components are pure — they receive props and render UI. Presentational components are trivially testable.
- **Forward refs** on any component that wraps a native element (inputs, buttons). Required for React Aria integration.
- **`displayName`** on all `forwardRef` and `memo` components — helps React DevTools debugging.
- Avoid `useEffect` for derived state. Use `useMemo` or compute inline. Effects are for side effects (fetching, subscriptions, DOM manipulation).

---

## 3. TypeScript

- **Strict mode on.** `"strict": true` in tsconfig. No `any` — use `unknown` + type guards.
- **Discriminated unions** for state machines (message status, repo status, indexing state). Never use separate booleans for mutually exclusive states.
  ```ts
  // Bad: isLoading, isError, isSuccess (can be true simultaneously)
  // Good:
  type QueryState = { status: 'idle' } | { status: 'loading' } | { status: 'error'; error: string } | { status: 'success'; data: T }
  ```
- **Zod or valibot** for runtime validation of API responses. Never trust the backend shape at runtime.
- **`satisfies`** keyword for type-safe object literals that still infer narrow types.
- **Const assertions** (`as const`) for static config objects (routes, event names, status maps).
- Export types separately from values — enables type-only imports and better tree-shaking.

---

## 4. State Management

- **Server state → TanStack Query.** Never put API data in Zustand. TanStack handles caching, deduplication, background refetching, optimistic updates.
- **Client state → Zustand.** Only for truly client-side state: active selections, UI toggles, streaming content buffer.
- **URL state → React Router.** Current conversation ID, active repo, page number — all in the URL. Enables deep linking and browser back/forward.
- **Derived state → compute, don't store.** If it can be calculated from other state, calculate it. Don't sync.
- **Zustand slices** — split stores by domain. Don't create one god store. Use `subscribeWithSelector` for granular re-renders.
- **Selectors everywhere.** `useChatStore(s => s.isStreaming)` not `useChatStore()`. Prevents unnecessary re-renders.

---

## 5. Data Fetching

- **Query keys are structured arrays.** `['repos', repoId, 'conversations']` — enables granular invalidation.
- **Stale time > 0.** Set `staleTime: 30_000` minimum. Don't refetch on every mount.
- **Optimistic updates** for mutations with immediate UI feedback (delete conversation, disconnect repo). Rollback on error.
- **Error boundaries** per data section, not per page. One failed query shouldn't crash the whole page.
- **Retry with backoff.** `retry: 2` with exponential backoff for transient failures. Don't retry 401s.
- **Prefetching.** Prefetch conversation messages on hover over conversation list item. Feels instant.

---

## 6. SSE / Streaming

- **AbortController** on every stream. Cancel on unmount, on new message send, on conversation switch.
- **Buffer management.** Accumulate tokens in a ref, flush to state on `requestAnimationFrame` — prevents 100+ re-renders per second.
- **Reconnection logic.** If stream drops mid-response, show error state with retry button. Don't silently fail.
- **Backpressure.** If tokens arrive faster than React can render, batch them. Never queue unbounded state updates.
- **Cleanup.** Always close the reader in a `finally` block. Leaked streams = memory leaks.

---

## 7. Performance

- **`React.memo`** on message bubbles. Chat lists can have 100+ messages — don't re-render all on every token.
- **Virtualization** for long conversation lists in sidebar. Use `@tanstack/react-virtual` if list exceeds ~50 items.
- **Lazy loading.** Code-split pages with `React.lazy` + `Suspense`. The chat page doesn't need the settings page bundle.
- **Dynamic imports** for heavy deps. `react-syntax-highlighter` is huge — import only when a code block is visible.
- **Image optimization.** GitHub avatars: add `?s=64` for sidebar thumbnails. Don't load full-res.
- **Debounce** search inputs (300ms). Don't fire API calls on every keystroke.
- **`useCallback`** only when passing callbacks to memoized children or as effect dependencies. Don't wrap everything.

---

## 8. Error Handling

- **Global error boundary** at app root — catches React render errors, shows fallback UI.
- **Per-feature error boundaries** — chat area, sidebar, repo list each have their own. Isolation.
- **API error normalization.** Transform all API errors into a consistent shape in the axios interceptor. Components never parse raw error responses.
- **User-facing messages.** Never show stack traces or raw error codes. Map error codes to human-readable messages in a central `errorMessages` map.
- **Retry affordance.** Every error state has a "Try again" button. Never dead-end the user.
- **Logging.** Console errors in dev, send to a service (Sentry) in prod. Include context: which repo, which conversation, what action.

---

## 9. Accessibility

- **Semantic HTML first.** `<nav>`, `<main>`, `<aside>`, `<article>`, `<button>` — not `<div onClick>`.
- **React Aria handles the hard parts.** Don't reinvent focus traps, keyboard navigation, or ARIA attributes. Use the primitives.
- **Live regions** for dynamic content. Chat messages: `aria-live="polite"`. Errors: `aria-live="assertive"`.
- **Focus management.** After modal close → return focus to trigger. After message send → focus stays on input.
- **Visible focus rings.** Never `outline: none` without a replacement. Untitled UI handles this via `focus-ring` tokens.
- **Skip links.** Hidden "Skip to main content" link, visible on Tab focus.
- **Test with keyboard only.** If you can't complete a flow without a mouse, it's broken.

---

## 10. Styling

- **Tailwind only.** No CSS modules, no styled-components, no inline styles. One styling paradigm.
- **`cx()` utility** (from Untitled UI) for conditional classes. Never string concatenation with ternaries.
- **Design tokens over raw values.** `text-neutral-600` not `text-[#6B7280]`. `rounded-xl` not `rounded-[12px]`. Tokens change, raw values don't.
- **Responsive: mobile-first.** Base styles are mobile, then `md:` and `lg:` for larger screens.
- **No `!important`.** If you need it, your specificity is wrong. Fix the cascade.
- **Extract repeated patterns** into Tailwind `@apply` only for truly atomic patterns (like `.prose-code`). Prefer component extraction over `@apply`.
- **Dark mode via Untitled UI tokens.** Don't write `dark:` variants manually — the CSS variables handle it.

---

## 11. Testing Strategy

- **Unit tests** (Vitest): Pure functions, utilities, store logic, API transformations.
- **Component tests** (Vitest + Testing Library): Render component, assert output. Test behavior, not implementation.
- **Integration tests** (Playwright): Critical flows — login, connect repo, ask question, view citation.
- **What to test:** User-visible behavior. "When I click Send, the message appears." Not "when I click Send, `addMessage` is called with these args."
- **Mock at the network boundary.** Use MSW (Mock Service Worker) to intercept API calls. Don't mock internal modules.
- **Snapshot tests: avoid.** They break on every UI change and test nothing meaningful.
- **Coverage target:** 80%+ on business logic (hooks, stores, API layer). Don't chase 100% on presentational components.

---

## 12. Security

- **JWT in memory, not localStorage.** Store token in Zustand (memory). Use `httpOnly` cookie if possible. localStorage is XSS-vulnerable.
  - If localStorage is the only option (current arch), sanitize all rendered content.
- **Sanitize markdown output.** `react-markdown` with `rehype-sanitize`. Never render raw HTML from API responses.
- **CSP headers.** Configure Content-Security-Policy to block inline scripts and unauthorized origins.
- **No secrets in frontend code.** `VITE_` env vars are public. Only non-sensitive config (API URL, OAuth client ID).
- **Input validation.** Validate repo name format client-side before sending. Max length on question input (1000 chars as per API spec).
- **Rate limit awareness.** Read `X-RateLimit-Remaining` headers. Disable input proactively when approaching limit.

---

## 13. Code Quality

- **ESLint + Prettier.** Non-negotiable. Auto-format on save. Lint on pre-commit (husky + lint-staged).
- **No console.log in production.** Use a logger utility that's a no-op in prod builds.
- **Consistent naming:**
  - Components: `PascalCase`
  - Hooks: `useCamelCase`
  - Utils: `camelCase`
  - Constants: `SCREAMING_SNAKE_CASE`
  - Files: match their default export
- **Small PRs.** One feature or fix per PR. Easier to review, easier to revert.
- **No dead code.** If it's commented out, delete it. Git has history.
- **Explicit over implicit.** Name things clearly. `isStreamingResponse` not `flag`. `handleSendMessage` not `handler`.

---

## 14. Git & CI

- **Conventional commits.** `feat:`, `fix:`, `refactor:`, `chore:`. Enables auto-changelogs.
- **Branch naming.** `feat/chat-streaming`, `fix/citation-expand-crash`.
- **Pre-commit hooks:** lint, format, type-check. Catch errors before they hit CI.
- **CI pipeline:** lint → type-check → test → build. All must pass before merge.
- **Bundle analysis.** Run `vite-bundle-visualizer` periodically. Catch unexpected bundle bloat.
- **Lighthouse CI.** Performance budget: LCP < 2s, CLS < 0.1, FID < 100ms.

---

## 15. Production Readiness Checklist

- [ ] Error boundaries on all route-level components
- [ ] Loading states for every async operation
- [ ] Empty states for every list that can be empty
- [ ] 404 page for unknown routes
- [ ] Favicon, meta tags, Open Graph tags
- [ ] `robots.txt` and `sitemap.xml` (if public)
- [ ] Environment-specific configs (dev/staging/prod)
- [ ] Source maps uploaded to error tracking (Sentry)
- [ ] Bundle size < 200KB gzipped (initial load)
- [ ] All images optimized and lazy-loaded
- [ ] HTTPS enforced, HSTS headers
- [ ] Rate limit handling in UI
- [ ] Graceful degradation when backend is down
- [ ] Analytics events on key actions (optional)
- [ ] Accessibility audit passes (axe-core, 0 violations)
