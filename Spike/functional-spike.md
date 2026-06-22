# AnonCircles — Functional Spike

This document captures the **business requirements, user flows, and expected behavior** for every API in the AnonCircles platform, and maps each functional requirement to the implementation ticket(s) that deliver it.

**Stack:** Java (Spring Boot) backend × 3 services + React (Vite + Apollo Client) UI.

- Repo 1 — `discussions-service` (Spring Boot REST + Postgres) → [repo-1-tickets.md](./repo-1-tickets.md)
- Repo 2 — `genai-service` (Spring Boot WebFlux + Gemini) → [repo-2-tickets.md](./repo-2-tickets.md)
- Repo 3 — `discussions-graph` (Spring for GraphQL BFF) → [repo-3-tickets.md](./repo-3-tickets.md)
- Repo 4 — `discussions-ui` (React + Vite + Apollo Client) → [repo-4-tickets.md](./repo-4-tickets.md)

---

## 1. Product Vision

AnonCircles lets anyone sign up with email/password, join topic-based **Discussion Circles**, and post **pseudonymously** — the system knows the user (for spam control and recovery), but other members in a circle only ever see a deterministic per-circle handle (e.g., `PurpleHeron-42`). Circles are sorted by popularity (member count), can be AI-described, and are administered solely by their creator.

### Identity Model
- **System identity:** email + password (PII, never exposed to other users).
- **Per-circle identity:** anonymous handle deterministically derived from `userId + circleId` — consistent within a circle, uncorrelatable across circles.

---

## 2. Personas & Top-Level User Flows

### 2.1 Visitor → New Member
1. Lands on `/login`, clicks "Register".
2. Submits email + password → receives verification email → clicks link → email verified.
3. Logs in → JWT `engage-auth` issued → redirected to **All Discussions** feed.

UI flow tickets:
- [Build `/register` page](./repo-4-tickets.md#ticket-build-register-page)
- [Build `/verify-email` page](./repo-4-tickets.md#ticket-build-verify-email-page)
- [Build `/login` page](./repo-4-tickets.md#ticket-build-login-page)
- [Build `/forgot-password` page](./repo-4-tickets.md#ticket-build-forgot-password-page)
- [Build `/reset-password` page](./repo-4-tickets.md#ticket-build-reset-password-page)

### 2.2 Authenticated User Browses Circles
- **All Discussions** (`/`) — every circle in the system, popularity-sorted.
- **Discover** (`/discover`) — circles the user has **not** joined (browse mode); **when searching, results widen to include circles the user has already joined** (search-overrides-scope).
- **My Discussions** (`/my`) — circles the user **created (admin)** or **joined**; Admin pill shown where applicable; **search stays scoped to the user's own circles**.
- Every feed has a **search bar** (matches `topic` or `description`) and a Popular | Newest sort.
- **Page size is fixed at 10 results per page** across every paginated list (circles, threads, messages).

UI flow tickets:
- [Build reusable `<CircleFeed scope />` component](./repo-4-tickets.md#ticket-build-reusable-circlefeed-scope--component)
- [Build `/` (All Discussions) page](./repo-4-tickets.md#ticket-build--all-discussions-page)
- [Build `/discover` page](./repo-4-tickets.md#ticket-build-discover-page)
- [Build `/my` page](./repo-4-tickets.md#ticket-build-my-page)
- [Build `<CircleCard>` with Admin badge + optimistic Join/Leave](./repo-4-tickets.md#ticket-build-circlecard-with-admin-badge--optimistic-joinleave)
- [Build `<Nav>` with tabs and user menu](./repo-4-tickets.md#ticket-build-nav-with-tabs-and-user-menu)

### 2.3 User Creates a Circle (Becomes Admin)
1. Clicks "Create Circle" → modal opens.
2. Types `topic`. Optionally clicks **✨ Generate description** (AI-assisted).
3. Submits → circle is created, user is admin and implicitly a member.

UI flow tickets:
- [Build `<CreateCircleModal>` with AI-assisted description](./repo-4-tickets.md#ticket-build-createcirclemodal-with-ai-assisted-description)
- [Stream `generateDescription` into textarea](./repo-4-tickets.md#ticket-stream-generatedescription-into-textarea)
- [Handle `DescriptionUnavailable` branch](./repo-4-tickets.md#ticket-handle-descriptionunavailable-branch)

### 2.4 User Joins or Leaves a Circle
- One-click Join/Leave on any card. Optimistic UI — cards "migrate" between feeds on next view.
- **Admins cannot leave their own circle** — they must delete it.

UI flow ticket:
- [Build `<CircleCard>` with Admin badge + optimistic Join/Leave](./repo-4-tickets.md#ticket-build-circlecard-with-admin-badge--optimistic-joinleave)

### 2.5 Admin Edits or Deletes Their Circle
- Edit modal lets admin change **description only** (topic is immutable).
- Delete removes the circle (and all threads/messages, by cascade).

UI flow tickets:
- [Build `<EditCircleModal>` (admin-only, description-only)](./repo-4-tickets.md#ticket-build-editcirclemodal-admin-only-description-only)
- [Wire DELETE circle confirmation](./repo-4-tickets.md#ticket-wire-delete-circle-confirmation)

### 2.6 Member Posts in a Thread
- Inside a circle, members see threads, open one, and post messages **as their per-circle handle**.
- Other users only see the handle, never the email.

UI flow tickets:
- [Build `/circles/:id` page](./repo-4-tickets.md#ticket-build-circlesid-page)
- [Build `/threads/:id` page](./repo-4-tickets.md#ticket-build-threadsid-page)
- ["New Thread" form/modal](./repo-4-tickets.md#ticket-new-thread-formmodal)

---

## 3. Functional Requirements — API by API

### 3.1 Auth APIs (Spring REST in `discussions-service`, proxied through GraphQL BFF)

| Functional Requirement | Backend Ticket(s) | GraphQL Ticket | UI Ticket |
|---|---|---|---|
| Register with email + password; never reveal whether email exists; send verification email | [Implement `POST /auth/register`](./repo-1-tickets.md#ticket-implement-post-authregister) | [`register(email, password)` mutation](./repo-3-tickets.md#ticket-registeremail-password-mutation) | [Build `/register` page](./repo-4-tickets.md#ticket-build-register-page) |
| Single-use email verification link → mark `email_verified_at` | [Implement `GET /auth/verify?token=...`](./repo-1-tickets.md#ticket-implement-get-authverifytoken) | [`verifyEmail(token)` mutation](./repo-3-tickets.md#ticket-verifyemailtoken-mutation) | [Build `/verify-email` page](./repo-4-tickets.md#ticket-build-verify-email-page) |
| Login with email + password; reject unverified; generic 401; constant-time compare; rate-limited 5/min | [Implement `POST /auth/login`](./repo-1-tickets.md#ticket-implement-post-authlogin), [Implement Bucket4j rate-limit filter for auth endpoints](./repo-1-tickets.md#ticket-implement-bucket4j-rate-limit-filter-for-auth-endpoints) | [`login(email, password): AuthResult` mutation](./repo-3-tickets.md#ticket-loginemail-password-authresult-mutation) | [Build `/login` page](./repo-4-tickets.md#ticket-build-login-page) |
| Forgot password — always returns 200 (no enumeration); emails reset link if user exists | [Implement `POST /auth/forgot-password`](./repo-1-tickets.md#ticket-implement-post-authforgot-password) | [`forgotPassword(email)` mutation](./repo-3-tickets.md#ticket-forgotpasswordemail-mutation) | [Build `/forgot-password` page](./repo-4-tickets.md#ticket-build-forgot-password-page) |
| Reset password using single-use token | [Implement `POST /auth/reset-password`](./repo-1-tickets.md#ticket-implement-post-authreset-password) | [`resetPassword(token, newPassword)` mutation](./repo-3-tickets.md#ticket-resetpasswordtoken-newpassword-mutation) | [Build `/reset-password` page](./repo-4-tickets.md#ticket-build-reset-password-page) |
| Get current user from token | [Implement `GET /auth/me`](./repo-1-tickets.md#ticket-implement-get-authme) | [`me: User` query](./repo-3-tickets.md#ticket-me-user-query) | [Build `<AuthGuard>` wrapper component](./repo-4-tickets.md#ticket-build-authguard-wrapper-component) |
| Logout (client clears token; optional server-side blacklist) | [Implement `POST /auth/logout`](./repo-1-tickets.md#ticket-implement-post-authlogout) | [`logout` mutation](./repo-3-tickets.md#ticket-logout-mutation) | [Build `<Nav>` with tabs and user menu](./repo-4-tickets.md#ticket-build-nav-with-tabs-and-user-menu) |
| Auto-logout + redirect on 401 anywhere | — | — | [Set up Apollo Client with auth/error/http links](./repo-4-tickets.md#ticket-set-up-apollo-client-with-autherrorhttp-links) |

### 3.2 Circles APIs

| Functional Requirement | Backend Ticket(s) | GraphQL Ticket | UI Ticket |
|---|---|---|---|
| List circles with three scopes (`ALL`, `MINE`, `DISCOVER`), sort (Popular / Newest), pagination (page size capped at 10); each row carries `isAdmin`/`isMember` for the viewer | [Implement `GET /circles` with scope + search + sort + pagination](./repo-1-tickets.md#ticket-implement-get-circles-with-scope--search--sort--pagination), [Build scope-aware SQL clause builder for circles](./repo-1-tickets.md#ticket-build-scope-aware-sql-clause-builder-for-circles) | [`circles(scope, sort, search, page, limit)` query](./repo-3-tickets.md#ticket-circlesscope-sort-search-page-limit-query), [DataLoader for per-circle membership/admin lookup](./repo-3-tickets.md#ticket-dataloader-for-per-circle-membershipadmin-lookup) | [Build reusable `<CircleFeed scope />` component](./repo-4-tickets.md#ticket-build-reusable-circlefeed-scope--component), [Build `<Pagination>` controls (page size locked at 10)](./repo-4-tickets.md#ticket-build-pagination-controls-page-size-locked-at-10) |
| **Search circles** by topic or description on every feed page; `ALL` and `DISCOVER` searches span all circles, `MINE` search stays within the user's circles (scope-override rule for Discover) | [Implement `GET /circles` with scope + search + sort + pagination](./repo-1-tickets.md#ticket-implement-get-circles-with-scope--search--sort--pagination), [Build scope-aware SQL clause builder for circles](./repo-1-tickets.md#ticket-build-scope-aware-sql-clause-builder-for-circles), [Add GIN index on `circles.description` for search](./repo-1-tickets.md#ticket-add-gin-index-on-circlesdescription-for-search) | [`circles(scope, sort, search, page, limit)` query](./repo-3-tickets.md#ticket-circlesscope-sort-search-page-limit-query) | [Build shared `<SearchBar>` component](./repo-4-tickets.md#ticket-build-shared-searchbar-component), [Build reusable `<CircleFeed scope />` component](./repo-4-tickets.md#ticket-build-reusable-circlefeed-scope--component) |
| Fetch a single circle with viewer-scoped flags | [Implement `GET /circles/{id}`](./repo-1-tickets.md#ticket-implement-get-circlesid) | [`circle(id): Circle` query](./repo-3-tickets.md#ticket-circleid-circle-query) | [Build `/circles/:id` page](./repo-4-tickets.md#ticket-build-circlesid-page) |
| Create a circle (creator becomes admin/host); rate-limited 5/hr | [Implement `POST /circles`](./repo-1-tickets.md#ticket-implement-post-circles), [Implement per-user mutation rate limits](./repo-1-tickets.md#ticket-implement-per-user-mutation-rate-limits) | [`createCircle(topic, description)` mutation](./repo-3-tickets.md#ticket-createcircletopic-description-mutation) | [Build `<CreateCircleModal>` with AI-assisted description](./repo-4-tickets.md#ticket-build-createcirclemodal-with-ai-assisted-description) |
| Edit circle — **admin only, description only**; reject any other field | [Implement `PATCH /circles/{id}` (admin-only, description-only)](./repo-1-tickets.md#ticket-implement-patch-circlesid-admin-only-description-only) | [`updateCircleDescription(id, description)` mutation](./repo-3-tickets.md#ticket-updatecircledescriptionid-description-mutation) | [Build `<EditCircleModal>` (admin-only, description-only)](./repo-4-tickets.md#ticket-build-editcirclemodal-admin-only-description-only) |
| Delete circle — admin only; cascades | [Implement `DELETE /circles/{id}`](./repo-1-tickets.md#ticket-implement-delete-circlesid) | [`deleteCircle(id)` mutation](./repo-3-tickets.md#ticket-deletecircleid-mutation) | [Wire DELETE circle confirmation](./repo-4-tickets.md#ticket-wire-delete-circle-confirmation) |
| Join a circle (idempotent); admins are implicitly members and get 409 | [Implement `POST /circles/{id}/join`](./repo-1-tickets.md#ticket-implement-post-circlesidjoin), [Build deterministic anon handle generator](./repo-1-tickets.md#ticket-build-deterministic-anon-handle-generator) | [`joinCircle(id)` mutation](./repo-3-tickets.md#ticket-joincircleid-mutation) | [Build `<CircleCard>` with Admin badge + optimistic Join/Leave](./repo-4-tickets.md#ticket-build-circlecard-with-admin-badge--optimistic-joinleave) |
| Leave a circle; admins cannot leave (409 — must delete) | [Implement `DELETE /circles/{id}/join`](./repo-1-tickets.md#ticket-implement-delete-circlesidjoin) | [`leaveCircle(id)` mutation](./repo-3-tickets.md#ticket-leavecircleid-mutation) | [Build `<CircleCard>` with Admin badge + optimistic Join/Leave](./repo-4-tickets.md#ticket-build-circlecard-with-admin-badge--optimistic-joinleave) |

### 3.3 Threads & Messages APIs

| Functional Requirement | Backend Ticket(s) | GraphQL Ticket | UI Ticket |
|---|---|---|---|
| List threads in a circle (paginated) | [Implement `GET /circles/{id}/threads`](./repo-1-tickets.md#ticket-implement-get-circlesidthreads) | [`threads(circleId)` query](./repo-3-tickets.md#ticket-threadscircleid-query) | [Build `/circles/:id` page](./repo-4-tickets.md#ticket-build-circlesid-page) |
| Create thread — member-only; `created_by` snapshot is the per-circle handle | [Implement `POST /circles/{id}/threads`](./repo-1-tickets.md#ticket-implement-post-circlesidthreads) | [`createThread(circleId, title)` mutation](./repo-3-tickets.md#ticket-createthreadcircleid-title-mutation) | ["New Thread" form/modal](./repo-4-tickets.md#ticket-new-thread-formmodal) |
| List messages in a thread (paginated) — member-only | [Implement `GET /threads/{id}/messages`](./repo-1-tickets.md#ticket-implement-get-threadsidmessages) | [`messages(threadId)` query](./repo-3-tickets.md#ticket-messagesthreadid-query) | [Build `/threads/:id` page](./repo-4-tickets.md#ticket-build-threadsid-page) |
| Post message — member-only, ≤2000 chars, rate-limited 30/min/user | [Implement `POST /threads/{id}/messages`](./repo-1-tickets.md#ticket-implement-post-threadsidmessages), [Implement per-user mutation rate limits](./repo-1-tickets.md#ticket-implement-per-user-mutation-rate-limits) | [`postMessage(threadId, body)` mutation](./repo-3-tickets.md#ticket-postmessagethreadid-body-mutation) | [Build `/threads/:id` page](./repo-4-tickets.md#ticket-build-threadsid-page) |

### 3.4 GenAI APIs

| Functional Requirement | Backend (genai-service) Ticket | GraphQL Ticket | UI Ticket |
|---|---|---|---|
| Generate a circle description from a topic (and optional draft), streaming the output | [Implement `POST /generate` with Gemini streaming](./repo-2-tickets.md#ticket-implement-post-generate-with-gemini-streaming), [Build pure `PromptBuilder`](./repo-2-tickets.md#ticket-build-pure-promptbuilder) | [`generateDescription(topic, description, mode)` mutation with union return](./repo-3-tickets.md#ticket-generatedescriptiontopic-description-mode-mutation-with-union-return), [Stream `generateDescription` output to the client](./repo-3-tickets.md#ticket-stream-generatedescription-output-to-the-client) | [Stream `generateDescription` into textarea](./repo-4-tickets.md#ticket-stream-generatedescription-into-textarea) |
| Gracefully degrade when AI is unavailable (quota / safety / network) | [Graceful UNAVAILABLE fallback for Gemini errors](./repo-2-tickets.md#ticket-graceful-unavailable-fallback-for-gemini-errors) | [`generateDescription(topic, description, mode)` mutation with union return](./repo-3-tickets.md#ticket-generatedescriptiontopic-description-mode-mutation-with-union-return) | [Handle `DescriptionUnavailable` branch](./repo-4-tickets.md#ticket-handle-descriptionunavailable-branch) |
| Prevent quota abuse — 10 generations/hour/user | — | [Per-user rate limit on `generateDescription` (10/hr)](./repo-3-tickets.md#ticket-per-user-rate-limit-on-generatedescription-10hr) | [Global toast system for errors and confirmations](./repo-4-tickets.md#ticket-global-toast-system-for-errors-and-confirmations) |

### 3.5 Operational / Cross-Cutting

| Functional Requirement | Tickets |
|---|---|
| Public health endpoints (no token) | [Expose `GET /actuator/health` liveness endpoint](./repo-2-tickets.md#ticket-expose-get-actuatorhealth-liveness-endpoint) |
| Public auth endpoints are still rate-limited; everything else requires `engage-auth` | [Implement `EngageAuthFilter` (JWT auth filter)](./repo-1-tickets.md#ticket-implement-engageauthfilter-jwt-auth-filter), [Implement Bucket4j rate-limit filter for auth endpoints](./repo-1-tickets.md#ticket-implement-bucket4j-rate-limit-filter-for-auth-endpoints) |
| Loading skeletons, empty states, toasts, optimistic UI everywhere | [Tailwind loading skeletons + scope-aware empty states](./repo-4-tickets.md#ticket-tailwind-loading-skeletons--scope-aware-empty-states), [Global toast system for errors and confirmations](./repo-4-tickets.md#ticket-global-toast-system-for-errors-and-confirmations) |

---

## 4. Behavior Contracts (Edge Cases)

| Scenario | Expected Behavior | Ticket |
|---|---|---|
| Login with wrong password | Generic 401 "Invalid email or password"; constant-time bcrypt run | [Implement `POST /auth/login`](./repo-1-tickets.md#ticket-implement-post-authlogin) |
| 6th login attempt within a minute | 429 with `Retry-After` | [Implement Bucket4j rate-limit filter for auth endpoints](./repo-1-tickets.md#ticket-implement-bucket4j-rate-limit-filter-for-auth-endpoints) |
| Unverified user logs in | 401 (treated like wrong password — no info leak) | [Implement `POST /auth/login`](./repo-1-tickets.md#ticket-implement-post-authlogin) |
| Verify-email token reused | 400 (single-use, marked `used_at`) | [Implement `GET /auth/verify?token=...`](./repo-1-tickets.md#ticket-implement-get-authverifytoken) |
| Non-admin attempts `PATCH /circles/{id}` | 403; UI hides Edit button when `!isAdmin` | [Implement `PATCH /circles/{id}` (admin-only, description-only)](./repo-1-tickets.md#ticket-implement-patch-circlesid-admin-only-description-only), [Build `<EditCircleModal>` (admin-only, description-only)](./repo-4-tickets.md#ticket-build-editcirclemodal-admin-only-description-only) |
| PATCH body includes `topic` | 400 — topic is immutable; field allowlist | [Implement `PATCH /circles/{id}` (admin-only, description-only)](./repo-1-tickets.md#ticket-implement-patch-circlesid-admin-only-description-only) |
| Admin attempts `DELETE /circles/{id}/join` (leave own circle) | 409 — must delete circle instead | [Implement `DELETE /circles/{id}/join`](./repo-1-tickets.md#ticket-implement-delete-circlesidjoin) |
| Non-member posts to a thread | 403 | [Implement `POST /threads/{id}/messages`](./repo-1-tickets.md#ticket-implement-post-threadsidmessages) |
| GenAI quota exhausted mid-stream | Stream a final `UNAVAILABLE` SSE event; UI shows inline fallback message | [Graceful UNAVAILABLE fallback for Gemini errors](./repo-2-tickets.md#ticket-graceful-unavailable-fallback-for-gemini-errors), [Handle `DescriptionUnavailable` branch](./repo-4-tickets.md#ticket-handle-descriptionunavailable-branch) |
| 11th `generateDescription` call in an hour | GraphQL error `RATE_LIMITED`; UI toast | [Per-user rate limit on `generateDescription` (10/hr)](./repo-3-tickets.md#ticket-per-user-rate-limit-on-generatedescription-10hr) |
| Any 401 from BFF | Apollo `errorLink` clears token and routes to `/login?next=...` | [Set up Apollo Client with auth/error/http links](./repo-4-tickets.md#ticket-set-up-apollo-client-with-autherrorhttp-links) |
| `?limit=50` on any paginated list | Server clamps to 10 (no error); BFF resolver also clamps before forwarding | [Implement `GET /circles` with scope + search + sort + pagination](./repo-1-tickets.md#ticket-implement-get-circles-with-scope--search--sort--pagination), [`circles(scope, sort, search, page, limit)` query](./repo-3-tickets.md#ticket-circlesscope-sort-search-page-limit-query) |
| Search "java" on **My Discussions** | Only matches within circles the user owns or joined | [Build scope-aware SQL clause builder for circles](./repo-1-tickets.md#ticket-build-scope-aware-sql-clause-builder-for-circles), [Build reusable `<CircleFeed scope />` component](./repo-4-tickets.md#ticket-build-reusable-circlefeed-scope--component) |
| Search "java" on **Discover Discussions** | Returns matches across **all** circles (including ones already joined); helper text confirms scope-widening | [Build scope-aware SQL clause builder for circles](./repo-1-tickets.md#ticket-build-scope-aware-sql-clause-builder-for-circles), [Build reusable `<CircleFeed scope />` component](./repo-4-tickets.md#ticket-build-reusable-circlefeed-scope--component) |
| Search "java" on **All Discussions** | Returns matches across all circles (no scope filter active) | [Implement `GET /circles` with scope + search + sort + pagination](./repo-1-tickets.md#ticket-implement-get-circles-with-scope--search--sort--pagination) |
| Empty search on Discover | Falls back to default Discover behavior (only circles not joined) | [Build scope-aware SQL clause builder for circles](./repo-1-tickets.md#ticket-build-scope-aware-sql-clause-builder-for-circles) |

---

## 5. Success Criteria

A new user can register, verify email, log in, browse all three feeds, create a circle (using AI to draft the description), join/leave other circles, post in a thread under a per-circle pseudonym, edit their own circle's description (but not topic), and delete it — all without ever seeing another user's email and with graceful UX when AI is unavailable. Coverage validated by:

- [Testcontainers integration tests — auth lifecycle](./repo-1-tickets.md#ticket-testcontainers-integration-tests--auth-lifecycle)
- [Testcontainers integration tests — circles + scopes](./repo-1-tickets.md#ticket-testcontainers-integration-tests--circles--scopes)
- [Testcontainers integration tests — admin authorization](./repo-1-tickets.md#ticket-testcontainers-integration-tests--admin-authorization)
- [Testcontainers integration tests — pagination cap and search semantics](./repo-1-tickets.md#ticket-testcontainers-integration-tests--pagination-cap-and-search-semantics)
- [Integration tests — `generateDescription` success + unavailable + rate-limit](./repo-3-tickets.md#ticket-integration-tests--generatedescription-success--unavailable--rate-limit)
