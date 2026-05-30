---
inclusion: auto
---

# Frontend Coding Standards for CodebaseQA

This steering file ensures all frontend development follows production-grade best practices.

## Core Principles

1. **Feature-based structure** - Group by domain (chat/, repo/, auth/) not by type
2. **Single responsibility** - One component = one job
3. **Composition over props** - Prefer children and slots
4. **Container/Presentational split** - Separate data fetching from UI rendering
5. **TypeScript strict mode** - No `any`, use discriminated unions for state machines
6. **Server state in TanStack Query** - Never put API data in Zustand
7. **Client state in Zustand** - Only for UI state, use selectors everywhere
8. **URL state in React Router** - Enable deep linking
9. **Semantic HTML first** - Use proper HTML5 elements
10. **Tailwind only** - No CSS modules or inline styles

## State Management Rules

- **TanStack Query**: All API data, caching, background refetching
- **Zustand**: UI toggles, active selections, streaming buffers
- **React Router**: Current conversation ID, active repo, pagination
- **Derived state**: Compute, don't store

## Performance Requirements

- React.memo on message bubbles (chat lists can have 100+ messages)
- Virtualization for lists > 50 items
- Lazy loading with React.lazy + Suspense
- Dynamic imports for heavy dependencies
- Debounce search inputs (300ms)
- Bundle size < 200KB gzipped

## Error Handling

- Global error boundary at app root
- Per-feature error boundaries (chat, sidebar, repo list)
- API error normalization in axios interceptor
- User-facing error messages (no stack traces)
- Retry affordance on every error state

## Accessibility

- Semantic HTML (nav, main, aside, article, button)
- React Aria for complex interactions
- Live regions for dynamic content
- Focus management after modals
- Visible focus rings
- Keyboard-only navigation support

## Security

- JWT in memory (Zustand), not localStorage if possible
- Sanitize markdown with rehype-sanitize
- CSP headers configured
- No secrets in frontend code
- Input validation client-side
- Rate limit awareness

## Code Quality

- ESLint + Prettier (auto-format on save)
- No console.log in production
- Conventional commits (feat:, fix:, refactor:)
- Small PRs (one feature per PR)
- Explicit naming (isStreamingResponse not flag)

## Design System Integration

- Use Untitled UI React components
- Wrap Untitled UI in components/base/ - never modify source
- Use design tokens from theme.css
- Follow 12-ui-guide-stitch.md specifications
- Implement DESIGN.md color palette and typography

## File References

- Design Guide: #[[file:design/12-ui-guide-stitch.md]]
- Design System: #[[file:design/DESIGN.md]]
- Best Practices: #[[file:design/13-coding-best-practices.md]]
