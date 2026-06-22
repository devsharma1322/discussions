# AnonCircles — Technical Spike

This document captures the **architecture, data models, integration points, and technical constraints** for every API, and maps each implementation step to its ticket in the four repos.

**Stack:** Java 21 + Spring Boot 3.x for all three backend services; React 18 + Vite + Apollo Client for the UI.

- Repo 1 — `discussions-service` (Spring Boot REST + JDBC + Flyway + Postgres) → [repo-1-tickets.md](./repo-1-tickets.md)
- Repo 2 — `genai-service` (Spring Boot WebFlux + Google Gen AI Java SDK) → [repo-2-tickets.md](./repo-2-tickets.md)
- Repo 3 — `discussions-graph` (Spring for GraphQL + WebClient + java-dataloader) → [repo-3-tickets.md](./repo-3-tickets.md)
- Repo 4 — `discussions-ui` (React 18 + Vite + Apollo Client + Tailwind) → [repo-4-tickets.md](./repo-4-tickets.md)

---

## 1. System Architecture

```
   discussions-ui (React 18 + Vite + Apollo Client)
            │
            │  GraphQL  +  Authorization: Bearer <engage-auth>
            ▼
   discussions-graph (Spring for GraphQL BFF, Java 21)
        │                        │
        │  REST + engage-auth    │  HTTP/SSE + X-Internal-Auth
        ▼                        ▼
   discussions-service        genai-service
   (Spring Boot MVC + JDBC)   (Spring Boot WebFlux + Gen AI SDK)
        │
        ▼
   PostgreSQL 16
```

### Auth Boundaries (two tokens, two purposes)
- **`engage-auth`** (user JWT, HS256, 7d expiry, env `JWT_SECRET`, signed/verified via jjwt) — proves *who* the user is. UI → BFF → discussions-service, forwarded as-is.
- **`X-Internal-Auth`** (shared secret, `INTERNAL_SERVICE_TOKEN`, compared via `MessageDigest.isEqual`) — proves *what service* is calling. BFF → genai-service only.

Implementation tickets:
- [Implement JWT helper (`engage-auth`) using jjwt](./repo-1-tickets.md#ticket-implement-jwt-helper-engage-auth-using-jjwt)
- [Implement `EngageAuthFilter` (JWT auth filter)](./repo-1-tickets.md#ticket-implement-engageauthfilter-jwt-auth-filter)
- [Implement `InternalAuthWebFilter`](./repo-2-tickets.md#ticket-implement-internalauthwebfilter)
- [Forward Authorization header on every downstream REST call](./repo-3-tickets.md#ticket-forward-authorization-header-on-every-downstream-rest-call)
- [Implement `GenaiClient` with SSE consumption](./repo-3-tickets.md#ticket-implement-genaiclient-with-sse-consumption)

---

## 2. Data Model (PostgreSQL via Flyway)

| Table | Key Columns | Notes |
|---|---|---|
| `users` | `id (uuid pk)`, `email (unique)`, `password_hash (bcrypt)`, `email_verified_at`, `created_at` | bcrypt cost 12; `email` indexed `lower()` |
| `auth_tokens` | `token (pk)`, `user_id (fk)`, `purpose ('verify_email'|'reset_password')`, `expires_at`, `used_at`, `created_at` | Single-use; `purpose` is CHECK-constrained |
| `circles` | `id (uuid pk)`, `topic (immutable)`, `description`, `admin_user_id (fk)`, `member_count (denormalized)`, `created_at` | Index on `(member_count DESC, created_at DESC)`; GIN on topic via `pg_trgm` |
| `memberships` | `(user_id, circle_id) pk`, `handle`, `joined_at` | Trigger increments `circles.member_count` on INSERT, decrements on DELETE — within the same transaction |
| `threads` | `id (uuid pk)`, `circle_id (fk)`, `title`, `created_by (handle snapshot)`, `created_at` | Index `(circle_id, created_at DESC)` |
| `messages` | `id (uuid pk)`, `thread_id (fk)`, `body`, `author (handle snapshot)`, `created_at` | Index `(thread_id, created_at ASC)` |

Implementation tickets:
- [Author Flyway migrations for the full schema](./repo-1-tickets.md#ticket-author-flyway-migrations-for-the-full-schema)
- [Add member_count trigger as a Flyway migration](./repo-1-tickets.md#ticket-add-member_count-trigger-as-a-flyway-migration)
- [Provision local PostgreSQL via Docker Compose](./repo-1-tickets.md#ticket-provision-local-postgresql-via-docker-compose)
- [Configure HikariCP datasource and JdbcTemplate](./repo-1-tickets.md#ticket-configure-hikaricp-datasource-and-jdbctemplate)
- [Define domain entities and DTO records](./repo-1-tickets.md#ticket-define-domain-entities-and-dto-records)
- [Implement repositories with JdbcTemplate](./repo-1-tickets.md#ticket-implement-repositories-with-jdbctemplate)

### Pseudonymous Handle Derivation
Per-circle handle = `<Adjective><Animal>-<2-digit hash>` seeded by `userId + circleId`. Deterministic so the same user → same handle every session, but different per circle (uncorrelatable across circles). Stored on `memberships.handle` and snapshotted on `threads.created_by` / `messages.author` for forward-compatibility.

Implementation: [Build deterministic anon handle generator](./repo-1-tickets.md#ticket-build-deterministic-anon-handle-generator)

---

## 3. Integration Points

| Edge | Protocol | Auth | Failure Handling |
|---|---|---|---|
| UI → BFF | GraphQL over HTTP | `Authorization: Bearer <engage-auth>` (Apollo `authLink`) | `errorLink` catches `UNAUTHENTICATED` → clear token + redirect |
| BFF → discussions-service | REST over HTTP via Spring `WebClient` | Forwards user JWT as-is via `ExchangeFilterFunction` | REST 4xx mapped to typed GraphQL errors |
| BFF → genai-service | HTTP + SSE via Spring `WebClient` | `X-Internal-Auth: <INTERNAL_SERVICE_TOKEN>` | `UNAVAILABLE` events resolve to `DescriptionUnavailable` union branch |
| discussions-service → Postgres | JDBC via HikariCP + `NamedParameterJdbcTemplate` | DATABASE_URL | Parameterized queries; `@Transactional` services for joins/leaves |
| discussions-service → Resend | HTTPS REST (Resend Java SDK or SMTP via `JavaMailSender`) | API key | Dev `@Profile` fallback to console logging |
| genai-service → Gemini | Google Gen AI Java SDK | API key | `Flux.onErrorResume` → `UNAVAILABLE` SSE event |

Implementation tickets:
- [Set up Apollo Client with auth/error/http links](./repo-4-tickets.md#ticket-set-up-apollo-client-with-autherrorhttp-links)
- [Implement `DiscussionsClient` (WebClient wrapper)](./repo-3-tickets.md#ticket-implement-discussionsclient-webclient-wrapper)
- [Implement `GenaiClient` with SSE consumption](./repo-3-tickets.md#ticket-implement-genaiclient-with-sse-consumption)
- [REST → GraphQL error mapping helper](./repo-3-tickets.md#ticket-rest--graphql-error-mapping-helper)
- [Implement Resend email helper](./repo-1-tickets.md#ticket-implement-resend-email-helper)
- [Implement `POST /generate` with Gemini streaming](./repo-2-tickets.md#ticket-implement-post-generate-with-gemini-streaming)

---

## 4. API-by-API Technical Plan

### 4.1 Auth Endpoints (Spring REST → GraphQL mutations)

| Endpoint | Tech Notes | Tickets |
|---|---|---|
| `POST /auth/register` | `BCryptPasswordEncoder(12)`; insert user (or no-op); single-use token in `auth_tokens`; Resend email; always 200 to prevent enumeration; rate-limited 5/min per (IP, email) via Bucket4j | [Implement `POST /auth/register`](./repo-1-tickets.md#ticket-implement-post-authregister), [Configure BCryptPasswordEncoder](./repo-1-tickets.md#ticket-configure-bcryptpasswordencoder), [Implement Resend email helper](./repo-1-tickets.md#ticket-implement-resend-email-helper), [Implement Bucket4j rate-limit filter for auth endpoints](./repo-1-tickets.md#ticket-implement-bucket4j-rate-limit-filter-for-auth-endpoints), [`register(email, password)` mutation](./repo-3-tickets.md#ticket-registeremail-password-mutation) |
| `GET /auth/verify` | `@Transactional`: set `email_verified_at`, mark token `used_at` | [Implement `GET /auth/verify?token=...`](./repo-1-tickets.md#ticket-implement-get-authverifytoken), [`verifyEmail(token)` mutation](./repo-3-tickets.md#ticket-verifyemailtoken-mutation) |
| `POST /auth/login` | Always run `encoder.matches(...)` (constant time); reject unverified; sign JWT with `JWT_SECRET`, `expiresIn=7d`; generic 401 messaging; rate-limited 5/min per (IP, email) | [Implement `POST /auth/login`](./repo-1-tickets.md#ticket-implement-post-authlogin), [Implement JWT helper (`engage-auth`) using jjwt](./repo-1-tickets.md#ticket-implement-jwt-helper-engage-auth-using-jjwt), [`login(email, password): AuthResult` mutation](./repo-3-tickets.md#ticket-loginemail-password-authresult-mutation) |
| `POST /auth/forgot-password` | Single-use token (1h expiry); always 200 | [Implement `POST /auth/forgot-password`](./repo-1-tickets.md#ticket-implement-post-authforgot-password), [`forgotPassword(email)` mutation](./repo-3-tickets.md#ticket-forgotpasswordemail-mutation) |
| `POST /auth/reset-password` | Validate + consume token; BCrypt new password; transactional update | [Implement `POST /auth/reset-password`](./repo-1-tickets.md#ticket-implement-post-authreset-password), [`resetPassword(token, newPassword)` mutation](./repo-3-tickets.md#ticket-resetpasswordtoken-newpassword-mutation) |
| `GET /auth/me` | Returns the authenticated `EngageUserPrincipal` | [Implement `GET /auth/me`](./repo-1-tickets.md#ticket-implement-get-authme), [`me: User` query](./repo-3-tickets.md#ticket-me-user-query) |
| `POST /auth/logout` | Stateless by default; optional `revoked_tokens` blacklist | [Implement `POST /auth/logout`](./repo-1-tickets.md#ticket-implement-post-authlogout), [`logout` mutation](./repo-3-tickets.md#ticket-logout-mutation) |

### 4.2 Circles Endpoints

| Endpoint | Tech Notes | Tickets |
|---|---|---|
| `GET /circles` | Scope-aware WHERE clause via `CircleScopeClauseBuilder`; `?sort=popular` → `ORDER BY member_count DESC, created_at DESC`; `?search` ILIKE on `topic OR description` via `pg_trgm` GIN indexes; **scope-override:** `DISCOVER + search` widens to ALL, `MINE + search` stays within user circles; `LIMIT/OFFSET` pagination, **page size capped at 10**; `isAdmin`/`isMember` computed per row | [Implement `GET /circles` with scope + search + sort + pagination](./repo-1-tickets.md#ticket-implement-get-circles-with-scope--search--sort--pagination), [Build scope-aware SQL clause builder for circles](./repo-1-tickets.md#ticket-build-scope-aware-sql-clause-builder-for-circles), [Add GIN index on `circles.description` for search](./repo-1-tickets.md#ticket-add-gin-index-on-circlesdescription-for-search), [`circles(scope, sort, search, page, limit)` query](./repo-3-tickets.md#ticket-circlesscope-sort-search-page-limit-query), [DataLoader for per-circle membership/admin lookup](./repo-3-tickets.md#ticket-dataloader-for-per-circle-membershipadmin-lookup) |
| `GET /circles/{id}` | Single fetch + viewer flags | [Implement `GET /circles/{id}`](./repo-1-tickets.md#ticket-implement-get-circlesid), [`circle(id): Circle` query](./repo-3-tickets.md#ticket-circleid-circle-query) |
| `POST /circles` | `@Transactional` service method: insert circle (`memberCount=1`) + admin membership w/ handle; per-user rate-limit 5/hr; Jakarta Bean Validation | [Implement `POST /circles`](./repo-1-tickets.md#ticket-implement-post-circles), [Implement per-user mutation rate limits](./repo-1-tickets.md#ticket-implement-per-user-mutation-rate-limits), [Global validation + exception handler](./repo-1-tickets.md#ticket-global-validation--exception-handler), [`createCircle(topic, description)` mutation](./repo-3-tickets.md#ticket-createcircletopic-description-mutation) |
| `PATCH /circles/{id}` | Strict DTO accepts only `description`; Jackson `FAIL_ON_UNKNOWN_PROPERTIES=true` rejects any other key (400); admin check via `AccessDeniedException` (403) | [Implement `PATCH /circles/{id}` (admin-only, description-only)](./repo-1-tickets.md#ticket-implement-patch-circlesid-admin-only-description-only), [`updateCircleDescription(id, description)` mutation](./repo-3-tickets.md#ticket-updatecircledescriptionid-description-mutation) |
| `DELETE /circles/{id}` | Admin-only; FK ON DELETE CASCADE handles dependents | [Implement `DELETE /circles/{id}`](./repo-1-tickets.md#ticket-implement-delete-circlesid), [`deleteCircle(id)` mutation](./repo-3-tickets.md#ticket-deletecircleid-mutation) |
| `POST /circles/{id}/join` | Idempotent insert; 409 if admin; generates handle via seeded RNG | [Implement `POST /circles/{id}/join`](./repo-1-tickets.md#ticket-implement-post-circlesidjoin), [Build deterministic anon handle generator](./repo-1-tickets.md#ticket-build-deterministic-anon-handle-generator), [`joinCircle(id)` mutation](./repo-3-tickets.md#ticket-joincircleid-mutation) |
| `DELETE /circles/{id}/join` | Delete membership row; 409 if admin; trigger decrements count | [Implement `DELETE /circles/{id}/join`](./repo-1-tickets.md#ticket-implement-delete-circlesidjoin), [`leaveCircle(id)` mutation](./repo-3-tickets.md#ticket-leavecircleid-mutation) |

### 4.3 Threads & Messages Endpoints

| Endpoint | Tech Notes | Tickets |
|---|---|---|
| `GET /circles/{id}/threads` | Auth-only; paginated (page size capped at 10) | [Implement `GET /circles/{id}/threads`](./repo-1-tickets.md#ticket-implement-get-circlesidthreads), [`threads(circleId)` query](./repo-3-tickets.md#ticket-threadscircleid-query) |
| `POST /circles/{id}/threads` | Membership check; snapshot `created_by = handle` | [Implement `POST /circles/{id}/threads`](./repo-1-tickets.md#ticket-implement-post-circlesidthreads), [`createThread(circleId, title)` mutation](./repo-3-tickets.md#ticket-createthreadcircleid-title-mutation) |
| `GET /threads/{id}/messages` | Membership check via parent circle; paginated (page size capped at 10) | [Implement `GET /threads/{id}/messages`](./repo-1-tickets.md#ticket-implement-get-threadsidmessages), [`messages(threadId)` query](./repo-3-tickets.md#ticket-messagesthreadid-query) |
| `POST /threads/{id}/messages` | Membership check; per-user rate-limit 30/min; snapshot author handle | [Implement `POST /threads/{id}/messages`](./repo-1-tickets.md#ticket-implement-post-threadsidmessages), [Implement per-user mutation rate limits](./repo-1-tickets.md#ticket-implement-per-user-mutation-rate-limits), [`postMessage(threadId, body)` mutation](./repo-3-tickets.md#ticket-postmessagethreadid-body-mutation) |

### 4.4 GenAI Endpoint

| Endpoint | Tech Notes | Tickets |
|---|---|---|
| `POST /generate` | Behind `InternalAuthWebFilter`; Jakarta-validated `GenerateRequest`; pure `PromptBuilder`; Google Gen AI Java SDK on `gemini-2.5-flash`; controller returns `Flux<ServerSentEvent<String>>`; per-IP 60/min via Bucket4j | [Implement `POST /generate` with Gemini streaming](./repo-2-tickets.md#ticket-implement-post-generate-with-gemini-streaming), [Build pure `PromptBuilder`](./repo-2-tickets.md#ticket-build-pure-promptbuilder), [Graceful UNAVAILABLE fallback for Gemini errors](./repo-2-tickets.md#ticket-graceful-unavailable-fallback-for-gemini-errors), [Implement per-source rate limiting](./repo-2-tickets.md#ticket-implement-per-source-rate-limiting) |
| `generateDescription` (BFF) | Union return `DescriptionGenerated | DescriptionUnavailable`; per-user 10/hr Bucket4j limiter; streams to client (subscription / incremental delivery) | [`generateDescription(topic, description, mode)` mutation with union return](./repo-3-tickets.md#ticket-generatedescriptiontopic-description-mode-mutation-with-union-return), [Stream `generateDescription` output to the client](./repo-3-tickets.md#ticket-stream-generatedescription-output-to-the-client), [Per-user rate limit on `generateDescription` (10/hr)](./repo-3-tickets.md#ticket-per-user-rate-limit-on-generatedescription-10hr) |
| UI consumption | Stream tokens into textarea; disable submit while streaming; `DescriptionUnavailable` → inline message, textarea still editable | [Stream `generateDescription` into textarea](./repo-4-tickets.md#ticket-stream-generatedescription-into-textarea), [Handle `DescriptionUnavailable` branch](./repo-4-tickets.md#ticket-handle-descriptionunavailable-branch) |

---

## 5. Cross-Cutting Technical Constraints

### 5.1 Authentication & Authorization (Defense in Depth)
Every protected request flows through: **authn (`EngageAuthFilter` + Spring Security filter chain)** → **authz (admin / membership checks)** → **rate limit (Bucket4j per-user)** → **input validation (Jakarta Bean Validation)** → **business logic**.

- [Configure Spring Security filter chain](./repo-1-tickets.md#ticket-configure-spring-security-filter-chain)
- [Implement `EngageAuthFilter` (JWT auth filter)](./repo-1-tickets.md#ticket-implement-engageauthfilter-jwt-auth-filter)
- [Global validation + exception handler](./repo-1-tickets.md#ticket-global-validation--exception-handler)
- [Implement per-user mutation rate limits](./repo-1-tickets.md#ticket-implement-per-user-mutation-rate-limits)
- [Implement Bucket4j rate-limit filter for auth endpoints](./repo-1-tickets.md#ticket-implement-bucket4j-rate-limit-filter-for-auth-endpoints)

### 5.2 Anti-Enumeration & Anti-Brute-Force
- Register, login, and forgot-password all return generic messages.
- `BCryptPasswordEncoder.matches` always runs (constant-time login).
- Bucket4j limits on (IP + email) for auth; per-user for mutations.
- Tickets: [Implement `POST /auth/login`](./repo-1-tickets.md#ticket-implement-post-authlogin), [Implement `POST /auth/register`](./repo-1-tickets.md#ticket-implement-post-authregister), [Implement `POST /auth/forgot-password`](./repo-1-tickets.md#ticket-implement-post-authforgot-password)

### 5.3 SQL Injection
All queries use `NamedParameterJdbcTemplate` parameterized statements; no string concatenation. Enforced in code review. Ticket: [Implement repositories with JdbcTemplate](./repo-1-tickets.md#ticket-implement-repositories-with-jdbctemplate)

### 5.4 GraphQL Hardening
- Introspection disabled in `prod` profile.
- `MaxQueryDepthInstrumentation(7)` + `MaxQueryComplexityInstrumentation`.
- Ticket: [Disable introspection + cap query depth/complexity in production](./repo-3-tickets.md#ticket-disable-introspection--cap-query-depthcomplexity-in-production)

### 5.5 N+1 Avoidance
`java-dataloader` `BatchLoader` batches per-request `isAdmin`/`isMember` lookups so a 20-card feed makes a single downstream call.
- Ticket: [DataLoader for per-circle membership/admin lookup](./repo-3-tickets.md#ticket-dataloader-for-per-circle-membershipadmin-lookup)

### 5.6 Transactional Consistency
`circles.member_count` is maintained by a Postgres trigger inside the same transaction as the membership change. Popularity sort never drifts.
- Ticket: [Add member_count trigger as a Flyway migration](./repo-1-tickets.md#ticket-add-member_count-trigger-as-a-flyway-migration)

### 5.7 Streaming
- `genai-service` emits SSE via Spring WebFlux `Flux<ServerSentEvent<String>>`.
- BFF consumes the SSE via `WebClient.bodyToFlux(ServerSentEvent.class)` and re-emits via a GraphQL subscription / incremental delivery.
- UI consumes the streaming GraphQL operation and appends tokens to the textarea in real time.
- Tickets: [Implement `POST /generate` with Gemini streaming](./repo-2-tickets.md#ticket-implement-post-generate-with-gemini-streaming), [Stream `generateDescription` output to the client](./repo-3-tickets.md#ticket-stream-generatedescription-output-to-the-client), [Stream `generateDescription` into textarea](./repo-4-tickets.md#ticket-stream-generatedescription-into-textarea)

### 5.8 Typed Failure Modes
`GenerateDescriptionResult = DescriptionGenerated | DescriptionUnavailable` — TypeScript discriminated union (via `__typename`) forces the React UI to handle the failure branch at compile time.
- Tickets: [`generateDescription(topic, description, mode)` mutation with union return](./repo-3-tickets.md#ticket-generatedescriptiontopic-description-mode-mutation-with-union-return), [Handle `DescriptionUnavailable` branch](./repo-4-tickets.md#ticket-handle-descriptionunavailable-branch)

### 5.9 Token Storage
- Dev: `localStorage` (read by Apollo `authLink`).
- Production upgrade: httpOnly + SameSite=Strict cookie — one-line swap in `authLink`.
- Ticket: [Implement token storage helpers](./repo-4-tickets.md#ticket-implement-token-storage-helpers), [Set up Apollo Client with auth/error/http links](./repo-4-tickets.md#ticket-set-up-apollo-client-with-autherrorhttp-links)

### 5.10 Observability & Errors
- `RestErrorMapper` centralizes downstream-status → GraphQL-error translation (401/403/404/409/400/429).
- Apollo `errorLink` triggers global logout on `UNAUTHENTICATED`.
- `@ControllerAdvice GlobalExceptionHandler` keeps REST error responses uniform.
- Tickets: [REST → GraphQL error mapping helper](./repo-3-tickets.md#ticket-rest--graphql-error-mapping-helper), [Global validation + exception handler](./repo-1-tickets.md#ticket-global-validation--exception-handler), [Set up Apollo Client with auth/error/http links](./repo-4-tickets.md#ticket-set-up-apollo-client-with-autherrorhttp-links)

---

## 6. Environment & Configuration

| Variable | Service | Source |
|---|---|---|
| `DATABASE_URL` / Spring `SPRING_DATASOURCE_URL` | discussions-service | `jdbc:postgresql://...` |
| `JWT_SECRET` (`engage.auth.secret`) | discussions-service | `openssl rand -base64 32` |
| `RESEND_API_KEY`, `EMAIL_FROM`, `APP_BASE_URL` | discussions-service | Resend dashboard |
| `GEMINI_API_KEY` | genai-service | Google AI Studio |
| `INTERNAL_SERVICE_TOKEN` | genai-service **and** discussions-graph (same value) | `openssl rand -base64 32` |
| `DISCUSSIONS_SERVICE_URL`, `GENAI_SERVICE_URL` | discussions-graph | local URLs |
| `VITE_GRAPHQL_URL` | discussions-ui | BFF URL |

The UI receives no shared secret of any kind — only the user's JWT after login. Tickets establishing this: [Bootstrap Spring Boot 3 service skeleton](./repo-1-tickets.md#ticket-bootstrap-spring-boot-3-service-skeleton), [Bootstrap Spring Boot WebFlux project](./repo-2-tickets.md#ticket-bootstrap-spring-boot-webflux-project), [Bootstrap Spring for GraphQL project](./repo-3-tickets.md#ticket-bootstrap-spring-for-graphql-project), [Bootstrap React 18 + Vite + Tailwind project](./repo-4-tickets.md#ticket-bootstrap-react-18--vite--tailwind-project)

---

## 7. Build & Test Strategy

| Phase | Action | Tickets |
|---|---|---|
| 1. Backend foundation | Bootstrap, schema, triggers, datasource | [Bootstrap Spring Boot 3 service skeleton](./repo-1-tickets.md#ticket-bootstrap-spring-boot-3-service-skeleton), [Author Flyway migrations for the full schema](./repo-1-tickets.md#ticket-author-flyway-migrations-for-the-full-schema), [Add member_count trigger as a Flyway migration](./repo-1-tickets.md#ticket-add-member_count-trigger-as-a-flyway-migration) |
| 2. Auth lifecycle | bcrypt, JWT, email, all `/auth/*` routes, Bucket4j filters | [Implement JWT helper (`engage-auth`) using jjwt](./repo-1-tickets.md#ticket-implement-jwt-helper-engage-auth-using-jjwt), [Implement `POST /auth/login`](./repo-1-tickets.md#ticket-implement-post-authlogin), [Testcontainers integration tests — auth lifecycle](./repo-1-tickets.md#ticket-testcontainers-integration-tests--auth-lifecycle) |
| 3. Circles + threads + messages | Scope SQL, CRUD, membership checks | [Implement `GET /circles` with scope + sort + pagination](./repo-1-tickets.md#ticket-implement-get-circles-with-scope--sort--pagination), [Testcontainers integration tests — circles + scopes](./repo-1-tickets.md#ticket-testcontainers-integration-tests--circles--scopes), [Testcontainers integration tests — admin authorization](./repo-1-tickets.md#ticket-testcontainers-integration-tests--admin-authorization) |
| 4. GenAI service | Standalone WebFlux microservice + tests | [Implement `POST /generate` with Gemini streaming](./repo-2-tickets.md#ticket-implement-post-generate-with-gemini-streaming), [Unit tests for `PromptBuilder`](./repo-2-tickets.md#ticket-unit-tests-for-promptbuilder), [WebTestClient integration test for `InternalAuthWebFilter`](./repo-2-tickets.md#ticket-webtestclient-integration-test-for-internalauthwebfilter) |
| 5. BFF | Schema, clients, resolvers, DataLoader, hardening | [Define SDL schema in `schema.graphqls`](./repo-3-tickets.md#ticket-define-sdl-schema-in-schemagraphqls), [`generateDescription(topic, description, mode)` mutation with union return](./repo-3-tickets.md#ticket-generatedescriptiontopic-description-mode-mutation-with-union-return), [Integration tests — auth pass-through and scope mapping](./repo-3-tickets.md#ticket-integration-tests--auth-pass-through-and-scope-mapping) |
| 6. UI | Apollo links, AuthGuard, auth pages, feeds, modals | [Set up Apollo Client with auth/error/http links](./repo-4-tickets.md#ticket-set-up-apollo-client-with-autherrorhttp-links), [Build `<AuthGuard>` wrapper component](./repo-4-tickets.md#ticket-build-authguard-wrapper-component), [Build reusable `<CircleFeed scope />` component](./repo-4-tickets.md#ticket-build-reusable-circlefeed-scope--component), [Build `<CreateCircleModal>` with AI-assisted description](./repo-4-tickets.md#ticket-build-createcirclemodal-with-ai-assisted-description) |

---

## 8. Risks & Mitigations

| Risk | Mitigation | Ticket |
|---|---|---|
| JWT secret leak compromises every user | Env-only via `@ConfigurationProperties`; short rotation horizon; document blacklist upgrade path | [Implement JWT helper (`engage-auth`) using jjwt](./repo-1-tickets.md#ticket-implement-jwt-helper-engage-auth-using-jjwt), [Implement `POST /auth/logout`](./repo-1-tickets.md#ticket-implement-post-authlogout) |
| Internal service token leak | Separate from `JWT_SECRET`; constant-time `MessageDigest.isEqual`; never logged | [Implement `InternalAuthWebFilter`](./repo-2-tickets.md#ticket-implement-internalauthwebfilter) |
| Gemini quota exhaustion | Per-user 10/hr Bucket4j in BFF + per-IP 60/min in service; UNAVAILABLE fallback | [Per-user rate limit on `generateDescription` (10/hr)](./repo-3-tickets.md#ticket-per-user-rate-limit-on-generatedescription-10hr), [Implement per-source rate limiting](./repo-2-tickets.md#ticket-implement-per-source-rate-limiting), [Graceful UNAVAILABLE fallback for Gemini errors](./repo-2-tickets.md#ticket-graceful-unavailable-fallback-for-gemini-errors) |
| Member count drift | Postgres trigger inside same transaction; never app-side | [Add member_count trigger as a Flyway migration](./repo-1-tickets.md#ticket-add-member_count-trigger-as-a-flyway-migration) |
| GraphQL DoS via deep queries | `MaxQueryDepthInstrumentation(7)` + complexity; disable introspection in prod | [Disable introspection + cap query depth/complexity in production](./repo-3-tickets.md#ticket-disable-introspection--cap-query-depthcomplexity-in-production) |
| N+1 on discovery feed | `java-dataloader` batching | [DataLoader for per-circle membership/admin lookup](./repo-3-tickets.md#ticket-dataloader-for-per-circle-membershipadmin-lookup) |
| XSS-based token theft | Production move to httpOnly cookie via one-line `authLink` swap | [Set up Apollo Client with auth/error/http links](./repo-4-tickets.md#ticket-set-up-apollo-client-with-autherrorhttp-links) |
| Privilege escalation on circle edits | Authn → admin authz (`AccessDeniedException`) → field allowlist (Jackson) → Jakarta validation (4 layers) | [Implement `PATCH /circles/{id}` (admin-only, description-only)](./repo-1-tickets.md#ticket-implement-patch-circlesid-admin-only-description-only), [Global validation + exception handler](./repo-1-tickets.md#ticket-global-validation--exception-handler) |
