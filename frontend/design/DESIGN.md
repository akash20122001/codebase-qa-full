---
name: Modern Technical Utility
colors:
  surface: '#fbf9f9'
  surface-dim: '#dbdad9'
  surface-bright: '#fbf9f9'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3f3'
  surface-container: '#efeded'
  surface-container-high: '#e9e8e7'
  surface-container-highest: '#e3e2e2'
  on-surface: '#1b1c1c'
  on-surface-variant: '#4a4455'
  inverse-surface: '#303031'
  inverse-on-surface: '#f2f0f0'
  outline: '#7b7487'
  outline-variant: '#ccc3d8'
  surface-tint: '#732ee4'
  primary: '#630ed4'
  on-primary: '#ffffff'
  primary-container: '#7c3aed'
  on-primary-container: '#ede0ff'
  inverse-primary: '#d2bbff'
  secondary: '#674bb5'
  on-secondary: '#ffffff'
  secondary-container: '#ab8ffe'
  on-secondary-container: '#3f1e8c'
  tertiary: '#4e4e58'
  on-tertiary: '#ffffff'
  tertiary-container: '#666670'
  on-tertiary-container: '#e7e5f1'
  error: '#DC2626'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#eaddff'
  primary-fixed-dim: '#d2bbff'
  on-primary-fixed: '#25005a'
  on-primary-fixed-variant: '#5a00c6'
  secondary-fixed: '#e8ddff'
  secondary-fixed-dim: '#cebdff'
  on-secondary-fixed: '#21005e'
  on-secondary-fixed-variant: '#4f319c'
  tertiary-fixed: '#e3e1ed'
  tertiary-fixed-dim: '#c7c5d1'
  on-tertiary-fixed: '#1a1b23'
  on-tertiary-fixed-variant: '#46464f'
  background: '#fbf9f9'
  on-background: '#1b1c1c'
  surface-variant: '#e3e2e2'
  brand-50: '#F5F3FF'
  brand-400: '#A78BFA'
  brand-500: '#8B5CF6'
  brand-600: '#7C3AED'
  brand-700: '#6D28D9'
  brand-900: '#4C1D95'
  success: '#16A34A'
  warning: '#CA8A04'
  surface-bg: '#FFFFFF'
  surface-subtle: '#FAFAFA'
  border-default: '#E5E5E5'
  border-subtle: '#F5F5F5'
typography:
  display-xs:
    fontFamily: Geist
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.02em
  headline-sm:
    fontFamily: Geist
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  card-title:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
  body-md:
    fontFamily: Geist
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Geist
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
  label-caps:
    fontFamily: Geist
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.05em
  meta-xs:
    fontFamily: Geist
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  code-sm:
    fontFamily: JetBrains Mono
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  sidebar-width: 288px
  max-width-chat: 780px
  max-width-site: 1100px
  section-gap: 1.5rem
  component-gap: 1rem
  container-padding: 1rem
  chat-padding: 1.5rem
---

## Brand & Style

The design system is engineered for the modern developer, prioritizing speed, clarity, and technical precision. Drawing heavy inspiration from the **Minimalist** and **Corporate/Modern** movements (specifically the "Linear/Vercel" aesthetic), the system employs a "color is earned" philosophy. 

Whitespace is treated as a core functional element to reduce cognitive load during complex code analysis. The aesthetic is crisp and purposeful, utilizing sharp borders and subtle tonal shifts rather than heavy shadows or decorative flourishes. The interface should feel like a high-performance IDE: dependable, fast, and unobtrusive.

**Key Stylistic Principles:**
- **Clarity over Decoration:** Every element must serve a functional purpose.
- **Technical Sophistication:** Use of monospaced elements for data and metadata to reinforce the developer-first identity.
- **Intentional Brand Presence:** The violet-indigo brand color is reserved for primary actions, focus states, and user-originated content, acting as a visual beacon in a neutral environment.

## Colors

The palette is anchored by a high-energy **Violet-Indigo** primary and a sophisticated grayscale. 

### Brand Palette
The primary brand color (`brand-600`) is used for high-intent actions and user identity. Lighter tints like `brand-50` provide soft backgrounds for active states or highlighted repository items, ensuring the brand presence is felt without overwhelming the content.

### Neutral System
The neutrals are strictly balanced to provide structure. 
- **Neutral-900 (#171717)** is used for primary text and headings to ensure maximum contrast.
- **Neutral-600 (#525252)** is reserved for body copy and secondary instructions.
- **Neutral-200 (#E5E5E5)** serves as the standard border color, creating a "framed" look common in high-end technical tools.

### Functional Colors
Status colors (Success, Warning, Error) follow standard conventions but are paired with ultra-light background tints (5% opacity) for badges and status indicators to maintain the system's airy feel.

## Typography

This design system uses a dual-font approach to balance human-readable content with technical data.

- **Primary Typeface:** **Geist** (or Inter) is used for all UI elements, headings, and body text. It provides a clean, neutral, and highly legible foundation.
- **Technical Typeface:** **JetBrains Mono** is utilized for code snippets, file paths, line numbers, and specific metadata labels.

**Hierarchy Rules:**
- Headings always use **Semibold** weight to create a strong anchor in a minimalist layout.
- Body text is optimized for long-form reading in chat contexts at `14px` with a generous `24px` line height.
- Small labels and category headers should be set in **Uppercase** with increased letter-spacing to distinguish them from interactive labels.

## Layout & Spacing

The layout utilizes a **Fixed-Fluid Hybrid** model. Navigation and sidebars are fixed-width to maintain consistent utility access, while the primary workspace fluidly adjusts to the viewport before capping at specific max-widths to preserve readability.

**Breakpoints & Reflow:**
- **Desktop (1280px+):** Sidebar is persistent (`288px`). Chat is centered with a max-width of `780px` to maintain ideal line lengths.
- **Tablet (768px - 1024px):** Sidebar collapses into a drawer. Content margins decrease to `24px`.
- **Mobile (<768px):** Single column layout. `16px` horizontal padding. Cards become full-width.

**Grid & Rhythm:**
- A **12-column fluid grid** is used for the dashboard, typically arranging repository cards in 3 columns.
- Vertical spacing follows an **8px base unit**. Gaps between major sections are consistently `24px`, while internal component spacing is `16px`.

## Elevation & Depth

Depth is primarily communicated through **Tonal Layering** and **Low-contrast Outlines** rather than traditional shadows.

- **The Base Layer:** The application background is `white`.
- **The Secondary Layer:** Sidebars and secondary citation panels use `neutral-50` to create a subtle recessed effect.
- **Shadow Strategy:** 
    - **Shadow XS:** Used on all standard cards to give a slight "lift" from the background.
    - **Shadow LG:** Reserved for floating elements like command menus, tooltips, and dropdowns.
    - **Shadow XL:** Used exclusively for modal dialogs.
- **Interactive Depth:** When hovering over a card, it should lift slightly via a `-1px` Y-axis translation and a transition to **Shadow SM**.
- **Accents:** Active items (like the currently selected repository) use a `2px` left-aligned brand border instead of a background color change, maintaining a clean look.

## Shapes

The shape language is "Soft-Modern," using varying radii to distinguish between layout containers and interactive elements.

- **Main Containers:** Repository cards and citation panels use `rounded-xl` (12px) to feel approachable.
- **Interactive Elements:** Buttons and input fields use `rounded-lg` (8px) for a slightly more precise, "tool-like" appearance.
- **System Tags:** Badges and small tags use `rounded-md` (6px).
- **Special Case - Chat Bubbles:** Bubbles use `16px` rounding for three corners, with the corner closest to the sender's origin (bottom-right for user, bottom-left for assistant) sharpened to `4px` to indicate directionality.

## Components

### Buttons & Inputs
- **Primary Button:** Filled with `brand-600`, white text, `8px` radius. Hover state shifts to `brand-700`.
- **Input Fields:** `1px` border in `neutral-200`. On focus, the border shifts to `brand-500` with a `2px` brand-tinted outer glow.

### Chat Bubbles
- **User Bubble:** Background `brand-600`, text `white`. Aligned to the right.
- **Assistant Bubble:** Background `white`, border `neutral-200`, text `neutral-900`. Aligned to the left.
- **Streaming State:** Use three bouncing dots in `brand-400`.

### Citation Cards (Expandable)
- Use `neutral-50` background with a `neutral-200` border.
- The header should show the file path in `code-sm` (`neutral-700`).
- Content should be truncated by default, expanding with a `150ms` ease-in-out transition.

### Repository Status Badges
- Small, pill-shaped tags.
- **Indexing:** `yellow-50` background, `yellow-600` text, with a subtle pulsing animation on a leading icon.
- **Ready:** `green-50` background, `green-600` text.
- **Error:** `red-50` background, `red-600` text.

### Sidebar Navigation
- Vertical stack with `12px` padding between items.
- Active state: `neutral-900` text and a `2px` brand-violet left border.
- Hover state: `neutral-50` background.