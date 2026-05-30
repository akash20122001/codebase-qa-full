# Installation Instructions - Phase 1

## Prerequisites
- Node.js 18+ installed
- Backend server running on port 8080 (or update VITE_API_URL)

## Step 1: Install Missing Dependencies

Run this command in the frontend directory:

```bash
npm install clsx tailwind-merge
```

## Step 2: Verify Environment Variables

Check your `.env` file has:

```env
VITE_API_URL=http://localhost:8080
```

## Step 3: Start Development Server

```bash
npm run dev
```

The app should open at `http://localhost:5173`

## Step 4: Test the Application

1. **Login Page** - Navigate to `/login`
   - Should see centered login card with GitHub button
   - Subtle dot pattern background
   - Brand violet color scheme

2. **OAuth Flow** - Click "Continue with GitHub"
   - Should redirect to GitHub OAuth
   - After auth, redirects to `/auth/callback`
   - Then redirects to home page

3. **Home Page** - After login
   - Should see welcome message with username
   - Ask card with repo selector and question input
   - Suggestion pills
   - Empty state for recent conversations

4. **Repositories Page** - Click "Repositories" in sidebar
   - Should see stats dashboard (4 cards)
   - Repository grid with 3 mock repos
   - Status badges (Ready, Indexing)
   - Connect placeholder card
   - Click "Connect Repo" to open modal

5. **Sidebar Navigation**
   - Should see active state on current page
   - Hover states work
   - User profile at bottom with avatar
   - Logout button works

## Troubleshooting

### Issue: "Cannot find module 'clsx'"
**Solution**: Run `npm install clsx tailwind-merge`

### Issue: "Failed to fetch" on login
**Solution**: Ensure backend is running and VITE_API_URL is correct

### Issue: Styles not loading
**Solution**: 
1. Stop dev server
2. Delete `node_modules/.vite` cache
3. Run `npm run dev` again

### Issue: TypeScript errors
**Solution**: Run `npm run build` to see all type errors

## Next Steps

Once Phase 1 is working:
1. Test all pages and navigation
2. Verify responsive design (resize browser)
3. Check console for errors
4. Proceed to Phase 2 (Chat page implementation)

## Quick Commands

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint

# Type check
npx tsc --noEmit
```
