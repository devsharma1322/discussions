# Repo 4 — `discussions-ui` Tickets

User-facing app. **React 18 + Vite + TypeScript + React Router v6 + Apollo Client + Tailwind CSS.** Talks only to the GraphQL BFF.

---

## Project Foundation

### Ticket: Bootstrap React 18 + Vite + Tailwind project
**Description:** `npm create vite@latest discussions-ui -- --template react-ts`. Install + configure Tailwind CSS (`tailwind.config.js`, `postcss.config.js`, base directives in `src/index.css`). Install `react-router-dom@6`, `@apollo/client`, `graphql`. Configure strict `tsconfig.json`. Add `.env.example` documenting `VITE_GRAPHQL_URL`. Set up `src/` layout (`pages`, `components`, `lib`, `hooks`, `types`, `graphql`).
**Link:** `[Link to be added]()`

### Ticket: Set up Apollo Client with auth/error/http links
**Description:** Build `src/lib/apolloClient.ts` composing three links in order: `authLink` (reads `engageAuth` from localStorage → `Authorization: Bearer <token>`), `errorLink` (catches `UNAUTHENTICATED` GraphQL errors or HTTP 401 → clears token + redirects to `/login?next=...`), `httpLink` (BFF URL from `import.meta.env.VITE_GRAPHQL_URL`). Wrap `<App>` in `<ApolloProvider>` in `main.tsx`.
**Link:** `[Link to be added]()`

### Ticket: Configure React Router v6 with route layout
**Description:** Define routes in `src/App.tsx` using `createBrowserRouter`: public (`/login`, `/register`, `/verify-email`, `/forgot-password`, `/reset-password`); protected wrapped by `<AuthGuard>` (`/`, `/discover`, `/my`, `/circles/:id`, `/threads/:id`). Root layout renders `<Nav>` + `<Outlet>` + `<ToastHost>`.
**Link:** `[Link to be added]()`

### Ticket: Generate TypeScript types from the GraphQL schema
**Description:** Install `@graphql-codegen/cli` + `@graphql-codegen/client-preset`. Configure `codegen.ts` to introspect the BFF schema and emit typed documents + hooks into `src/graphql/__generated__/`. Add `npm run codegen` and a `prebuild` hook.
**Link:** `[Link to be added]()`

---

## Token & Auth Plumbing

### Ticket: Implement token storage helpers
**Description:** Build `src/lib/auth.ts` exporting `getToken()`, `setToken(t)`, `clearToken()` against `localStorage`. Export `useAuth()` hook running the `me` query and exposing `{ user, loading, error, logout }`. Document the httpOnly-cookie upgrade path in comments.
**Link:** `[Link to be added]()`

### Ticket: Build `<AuthGuard>` wrapper component
**Description:** Client component that runs `useAuth()`. While loading, render a skeleton. On `UNAUTHENTICATED` or missing token, `navigate('/login?next=' + encodeURIComponent(location.pathname), { replace: true })`. Otherwise render `<Outlet />`. Used as a layout route element for all protected pages.
**Link:** `[Link to be added]()`

---

## Auth Pages

### Ticket: Build `/register` page
**Description:** Form with email + password inputs, inline validation. Calls the `register` mutation. On success, render a "Check your inbox" confirmation. Generic error toast on failure (never reveal whether email exists).
**Link:** `[Link to be added]()`

### Ticket: Build `/verify-email` page
**Description:** Reads `?token=...` via `useSearchParams`. Calls `verifyEmail` mutation on mount via `useEffect`. Renders success state ("Email verified — log in") with CTA to `/login`, or failure state with explanation + resend option.
**Link:** `[Link to be added]()`

### Ticket: Build `/login` page
**Description:** Form calling `login` mutation. On success, `setToken(engageAuth)` and `navigate(searchParams.get('next') ?? '/')`. Generic error message on failure. Links to `/register` and `/forgot-password`.
**Link:** `[Link to be added]()`

### Ticket: Build `/forgot-password` page
**Description:** Email input → `forgotPassword` mutation. Always shows "If your email exists, we sent a reset link" regardless of response.
**Link:** `[Link to be added]()`

### Ticket: Build `/reset-password` page
**Description:** Reads `?token=...`. New-password form + confirm field. Calls `resetPassword` mutation. On success, redirect to `/login` with a success toast.
**Link:** `[Link to be added]()`

---

## Navigation & Layout

### Ticket: Build `<Nav>` with tabs and user menu
**Description:** Three tabs: All Discussions (`/`), Discover (`/discover`), My Discussions (`/my`). Active tab highlighted using `useLocation`. Right side: user-menu dropdown showing logged-in email and a Logout button (calls `clearToken()` and navigates to `/login`).
**Link:** `[Link to be added]()`

---

## Discovery Feeds

### Ticket: Build reusable `<CircleFeed scope />` component
**Description:** Accepts `scope: CircleScope`. Runs `useCirclesQuery(scope, sort, search, page, limit)` with defaults `sort=POPULAR, page=1, limit=10` (fixed page size, cannot be raised by user). Renders responsive grid of `<CircleCard>`. Includes a 300ms-debounced `<SearchBar>` (shared) and a Popular/Newest sort dropdown. Skeleton grid while loading. Scope-aware empty states. **Search behavior:** input is always visible on All / Discover / My; for `DISCOVER`, when `search` is non-empty the result set widens to include circles the user has already joined (this matches the BFF/REST contract). Show a small helper text under the Discover search input when `search` is non-empty: "Showing matches across all circles."
**Link:** `[Link to be added]()`

### Ticket: Build shared `<SearchBar>` component
**Description:** Controlled text input with 300ms debounce, leading magnifier icon, clear (✕) button. Emits `onChange(search: string)` to the parent feed. Persists the current search to the URL (`?q=...`) via `useSearchParams` so the back button works and the URL is shareable. Max length 80 to mirror the server constraint.
**Link:** `[Link to be added]()`

### Ticket: Build `<Pagination>` controls (page size locked at 10)
**Description:** Render Prev / Next buttons and a "Page X of Y" indicator based on `{ total, page, limit }` from the query. Page size is fixed at 10 — no user-facing limit selector. Disabled state on Prev when `page === 1`, on Next when `page * limit >= total`. Reflect the current page in the URL (`?page=2`) via `useSearchParams`.
**Link:** `[Link to be added]()`

### Ticket: Build `/` (All Discussions) page
**Description:** Renders `<CircleFeed scope="ALL" />` inside the protected layout (already wrapped by `<AuthGuard>` via the route definition).
**Link:** `[Link to be added]()`

### Ticket: Build `/discover` page
**Description:** Renders `<CircleFeed scope="DISCOVER" />`. Empty state copy: "You've joined everything! 🎉".
**Link:** `[Link to be added]()`

### Ticket: Build `/my` page
**Description:** Renders `<CircleFeed scope="MINE" />`. Empty state copy: "Be the first to start one!" with a CTA that opens the Create Circle modal.
**Link:** `[Link to be added]()`

### Ticket: Build `<CircleCard>` with Admin badge + optimistic Join/Leave
**Description:** Shows topic, description preview, member count, "Admin" pill when `isAdmin`. Renders Join button when `!isMember && !isAdmin`, Leave when `isMember && !isAdmin`, no button when `isAdmin`. Uses `useMutation` with `optimisticResponse` to flip `isMember` and adjust `memberCount` instantly. Reverts + toast on error.
**Link:** `[Link to be added]()`

---

## Circle Detail & Thread Pages

### Ticket: Build `/circles/:id` page
**Description:** Reads `id` via `useParams`. Header with topic, description, member count, and admin Edit/Delete buttons visible only when `isAdmin`. List of threads via `useThreadsQuery` (page size 10) with `<Pagination>` controls and a "New Thread" button. Loading skeleton + empty state.
**Link:** `[Link to be added]()`

### Ticket: Build `/threads/:id` page
**Description:** Message list via `useMessagesQuery` (page size 10) with `<Pagination>` (or "Load older" button). Each message shows the author handle + timestamp. Highlights messages where `author === viewer.handle` for this circle. Input box at bottom calling `postMessage` mutation with optimistic append.
**Link:** `[Link to be added]()`

### Ticket: "New Thread" form/modal
**Description:** Triggered from the circle detail page. Title input (3–120 chars). On submit calls `createThread`, updates Apollo cache (or refetches the threads list), and navigates to the new thread.
**Link:** `[Link to be added]()`

---

## Create / Edit Circle

### Ticket: Build `<CreateCircleModal>` with AI-assisted description
**Description:** Modal with topic input + description textarea. "✨ Generate description" button with mode toggle (from topic only / from topic + my draft). On click, calls the `generateDescription` GraphQL operation and streams the result into the textarea. On submit calls `createCircle`, closes the modal, refetches the active feed.
**Link:** `[Link to be added]()`

### Ticket: Build `<EditCircleModal>` (admin-only, description-only)
**Description:** Opens from the circle detail page Edit button (only when `isAdmin`). Topic field rendered read-only with lock icon and helper text "Topic cannot be changed after creation." Description textarea + the same "✨ Generate description" button. On submit calls `updateCircleDescription`. In dev, warn if opened by non-admin.
**Link:** `[Link to be added]()`

### Ticket: Wire DELETE circle confirmation
**Description:** Admin Delete button on the detail page → confirm dialog → calls `deleteCircle` mutation → navigates back to `/my`. Toast on success/error.
**Link:** `[Link to be added]()`

---

## GenAI UX

### Ticket: Stream `generateDescription` into textarea
**Description:** In Create/Edit modals, consume the streaming GraphQL operation (Apollo subscription or incremental delivery client matching the BFF implementation) and append tokens to the textarea in real time. Disable submit while streaming. Show a small spinner near the textarea label.
**Link:** `[Link to be added]()`

### Ticket: Handle `DescriptionUnavailable` branch
**Description:** When the union resolves to `DescriptionUnavailable`, surface an inline message under the textarea ("AI is unavailable right now — type your own description"). Do not block submit. Use TypeScript discriminated union (`__typename`) so the unavailable branch is enforced at compile time.
**Link:** `[Link to be added]()`

---

## Polish

### Ticket: Tailwind loading skeletons + scope-aware empty states
**Description:** Build `<SkeletonCircleCard>` (animate-pulse) and an `<EmptyState>` component with scope-specific copy and CTAs. Use them across every feed and detail page.
**Link:** `[Link to be added]()`

### Ticket: Global toast system for errors and confirmations
**Description:** Lightweight toast provider (`<ToastHost>` + `useToast()` hook backed by React context) for success/error confirmations on join/leave, post message, generate description, etc. Errors from Apollo `errorLink` (non-401) also surface here.
**Link:** `[Link to be added]()`
