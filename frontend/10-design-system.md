# Design System — CodebaseQA

> Use this design system for ALL screens. Follow it exactly.

---

## Light Theme

| Token | Value |
|-------|-------|
| Background | `#FFFFFF` |
| Surface (cards) | `#FFFFFF` |
| Surface hover | `#F9FAFB` |
| Subtle background sections | `#F9FAFB` |
| Primary accent | `#1D4ED8` (dark blue) — buttons, active states, primary actions only |
| Secondary accent | `#3B82F6` (medium blue) — tags, pills, secondary highlights |
| Tertiary | `#059669` (muted green) — links, file references, success states |
| Text primary | `#111827` (near black) |
| Text secondary | `#6B7280` (medium gray) |
| Text muted | `#9CA3AF` (light gray) |
| Borders (cards) | `#F3F4F6` |
| Borders (dividers) | `#E5E7EB` |
| Input backgrounds | `#FFFFFF` with `#E5E7EB` border |
| Active/selected background | `#EFF6FF` (very pale blue) |
| Tag backgrounds | `#F3F4F6` (gray), `#EFF6FF` (blue), `#ECFDF5` (green) |
| Tag text | Matches accent color of the tag |
| Shadows | `rgba(0, 0, 0, 0.04) 0px 2px 16px` |
| Code block background | `#1F2937` |
| Inline code background | `#F3F4F6` |

---

## Dark Theme

| Token | Value |
|-------|-------|
| Background | `#09090B` |
| Surface (cards) | `#18181B` |
| Surface hover | `#1F1F23` |
| Subtle background sections | `#111113` |
| Primary accent | `#3B82F6` (soft blue) — buttons, active states, primary actions only |
| Secondary accent | `#60A5FA` (light blue) — tags, pills, secondary highlights |
| Tertiary | `#10B981` (muted green) — links, file references, success states |
| Text primary | `#F9FAFB` (off-white) |
| Text secondary | `#9CA3AF` (muted gray) |
| Text muted | `#6B7280` (dark muted gray) |
| Borders (cards) | `#27272A` |
| Borders (dividers) | `#3F3F46` |
| Input backgrounds | `#09090B` with `#3F3F46` border |
| Active/selected background | `#1E3A5F` (dark blue tint) |
| Tag backgrounds | `#27272A` (gray), `#1E3A5F` (blue), `#064E3B` (green) |
| Tag text | Matches accent color of the tag |
| Shadows | `rgba(0, 0, 0, 0.3) 0px 2px 16px` |
| Code block background | `#0A0A0F` |
| Inline code background | `#27272A` |

---

## Shared Tokens (Both Themes)

### Typography

| Token | Value |
|-------|-------|
| Font family (UI) | Inter |
| Font family (code) | JetBrains Mono |
| Page titles | 28px / 700 weight |
| Section headers | 18px / 600 weight |
| Card titles | 15px / 500 weight |
| Body text | 14px / 400 weight |
| Previews | 13px / 400 weight |
| Labels/metadata | 12px / 400 weight |
| Footnotes | 11px / 400 weight |
| Line height (UI) | 1.5 |
| Line height (reading) | 1.7 |

### Spacing

| Token | Value |
|-------|-------|
| Between sections | 24px |
| Between cards | 16px |
| Internal card padding | 12px |
| Between inline elements | 8px |

### Sizing

| Token | Value |
|-------|-------|
| Border radius (cards) | 12px |
| Border radius (buttons/inputs) | 8px |
| Border radius (tags/pills) | 6px |
| Border radius (inline code) | 4px |
| Button height (primary) | 40px |
| Button height (secondary) | 32px |
| Input height (standard) | 44px |
| Input height (textarea) | 100px |
| Icon size (navigation) | 24px |
| Icon size (inline) | 20px |
| Icon size (metadata) | 16px |

### Layout

| Token | Value |
|-------|-------|
| Left icon rail width | 56px |
| Main content max-width | 1100px |
| Reading content max-width | 780px |

### Navigation Rail Icons (top to bottom)

1. Home
2. Conversations
3. History
4. Repositories
5. Settings
6. (Bottom) User avatar

- Active icon: primary accent color + active/selected background circle
- Inactive icon: text secondary color

### Status Indicators

| Status | Color |
|--------|-------|
| Ready / Online | `#10B981` (green) |
| In Progress / Warning | `#F59E0B` (amber) |
| Error / Failed | `#EF4444` (red) |

### Interactions

| State | Effect |
|-------|--------|
| Card hover | Lift 2px + border changes to primary accent at 30% opacity |
| Active/selected | Left border 3px primary accent + active background tint |
| Transitions | `all 150ms ease` |

### Code Syntax Colors (used in code blocks in both themes)

| Token | Color |
|-------|-------|
| Keywords | `#93C5FD` (light blue) |
| Strings | `#6EE7B7` (mint green) |
| Comments | `#6B7280` (gray) |
| Functions | `#F9FAFB` (white) |
| Types | `#A5B4FC` (lavender) |
| Numbers | `#FCD34D` (amber) |
| Line numbers | `#4B5563` |

---

## Component Patterns

### Conversation Card
Icon (accent colored) + title (bold) + 2-line preview (secondary text) + bottom row with repo pill + message count + timestamp

### Repo Card
Folder icon (accent) + name (bold) + status dot + stats row + last indexed time

### Tag / Pill
Small rounded element with tinted background + matching text color

### Ask Card
Full width, primary accent top border (3px), contains repo selector + textarea + action links + submit button

### Source / Citation
File icon + monospace file path (tertiary color) + line range + relevance indicator

### Progress Bar
4px height, rounded, primary accent fill on dark track

---

## Design Tone

Clean, professional, spacious. Color used sparingly — mostly white/dark with blue accents. Not playful, not corporate. Feels like Linear, Notion, or Vercel's dashboard. Every screen should have real content and data — never empty or placeholder-feeling.
