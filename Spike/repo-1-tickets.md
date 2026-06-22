# Repo 1 — `discussions-service` Tickets

REST API for auth, circles, memberships, threads, and messages. **Java 21 + Spring Boot 3.x + Spring Web + Spring Security + PostgreSQL.**

---

## Project Foundation

### Ticket: Bootstrap Spring Boot 3 service skeleton
**Description:** Initialize a Maven/Gradle Java 21 project with Spring Initializr selecting dependencies: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-data-jdbc` (or JPA), `flyway-core`, `postgresql`, `lombok`. Configure `application.yml` profiles (`dev`, `test`, `prod`), base package layout (`controller`, `service`, `repository`, `domain`, `dto`, `security`, `config`, `exception`), and Maven Wrapper. Add `spring-boot-starter-test`, `testcontainers-postgresql`, `rest-assured` for tests.
**Link:** `[Link to be added]()`

### Ticket: Provision local PostgreSQL via Docker Compose
**Description:** Create `docker-compose.yml` with a single `postgres:16-alpine` service on port 5432, named volume for persistence, default `anoncircles` database. Document `docker compose up -d`. Add `application-dev.yml` pointing `spring.datasource.url` at `jdbc:postgresql://localhost:5432/anoncircles`.
**Link:** `[Link to be added]()`

### Ticket: Author Flyway migrations for the full schema
**Description:** Add `V1__init.sql` under `src/main/resources/db/migration` creating: `pg_trgm` extension, `users`, `auth_tokens` (with `purpose` CHECK constraint), `circles`, `memberships`, `threads`, `messages` per the design doc. Include all indexes (`idx_users_email`, `idx_circles_member_count`, `idx_circles_topic_trgm`, `idx_memberships_circle`, `idx_threads_circle`, `idx_messages_thread`). Configure Flyway to auto-run on startup.
**Link:** `[Link to be added]()`

### Ticket: Add member_count trigger as a Flyway migration
**Description:** Add `V2__member_count_trigger.sql` defining a Postgres trigger function that increments `circles.member_count` on `memberships` INSERT and decrements on DELETE, within the same transaction. Include rollback notes in a comment.
**Link:** `[Link to be added]()`

### Ticket: Configure HikariCP datasource and JdbcTemplate
**Description:** Expose `JdbcTemplate`/`NamedParameterJdbcTemplate` beans (auto-configured by Spring Boot). Tune HikariCP (max pool size, idle timeout). Add a slow-query log filter and a `@Profile("dev")` SQL logger.
**Link:** `[Link to be added]()`

---

## Domain, Repositories, and DTOs

### Ticket: Define domain entities and DTO records
**Description:** Create Java records / classes for `User`, `AuthToken`, `Circle`, `Membership`, `Thread`, `Message`, and matching request/response DTO records (`RegisterRequest`, `LoginRequest`, `AuthResultResponse`, `CircleResponse`, `CreateCircleRequest`, `UpdateCircleDescriptionRequest`, `JoinResponse`, etc.). Use Java `record` where immutability is appropriate. Centralize mapping helpers (or use MapStruct) between entity and DTO.
**Link:** `[Link to be added]()`

### Ticket: Implement repositories with JdbcTemplate
**Description:** Build `UserRepository`, `AuthTokenRepository`, `CircleRepository`, `MembershipRepository`, `ThreadRepository`, `MessageRepository`. Use parameterized SQL via `NamedParameterJdbcTemplate` (no string concat). Provide row mappers. Wrap multi-statement operations in `@Transactional` services.
**Link:** `[Link to be added]()`

---

## Auth Libraries

### Ticket: Implement JWT helper (`engage-auth`) using jjwt
**Description:** Add `io.jsonwebtoken:jjwt-api/-impl/-jackson`. Build `EngageAuthTokenService` with `String sign(UUID userId)` and `Claims verify(String token)`. Use HS256 with `JWT_SECRET` loaded from env via `@ConfigurationProperties`. `expiresIn = 7d`. Throw typed exceptions (`TokenExpiredException`, `InvalidTokenException`) mapped to 401 by a `@ControllerAdvice`.
**Link:** `[Link to be added]()`

### Ticket: Configure BCryptPasswordEncoder
**Description:** Expose a `BCryptPasswordEncoder(12)` bean. Build a `PasswordService` that always calls `encoder.matches(...)` — including when the user is not found — to maintain constant-time login.
**Link:** `[Link to be added]()`

### Ticket: Implement Resend email helper
**Description:** Add the Resend Java SDK (or fall back to `spring-boot-starter-mail` with SMTP). Build `EmailService` with `sendVerificationEmail(email, token)` and `sendPasswordResetEmail(email, token)`. URLs built from `app.base-url` config. `@Profile({"dev","test"})` implementation logs the email instead.
**Link:** `[Link to be added]()`

### Ticket: Build deterministic anon handle generator
**Description:** Build `HandleGenerator.generate(UUID userId, UUID circleId): String` returning `<Adjective><Animal>-<2-digit hash>`. Seed `java.util.Random` deterministically from a SHA-256 of `userId + circleId`. Ship curated adjective/animal word lists (~50 each). Unit-test determinism + spread.
**Link:** `[Link to be added]()`

---

## Security Filters & Middleware

### Ticket: Configure Spring Security filter chain
**Description:** Define `SecurityFilterChain` bean: disable CSRF (stateless JWT), `sessionCreationPolicy=STATELESS`, permit `/auth/**` and `/actuator/health`, require auth for everything else. Register the `EngageAuthFilter` before `UsernamePasswordAuthenticationFilter`. Configure CORS for the UI origin only.
**Link:** `[Link to be added]()`

### Ticket: Implement `EngageAuthFilter` (JWT auth filter)
**Description:** `OncePerRequestFilter` that reads `Authorization: Bearer <token>`, calls `EngageAuthTokenService.verify`, loads the user (`SELECT * FROM users WHERE id = sub AND email_verified_at IS NOT NULL`), and sets the `SecurityContext` with a custom `EngageUserPrincipal` (id, email). 401 on missing/invalid/expired/unverified. Skip on `/auth/**` and `/actuator/health`.
**Link:** `[Link to be added]()`

### Ticket: Implement Bucket4j rate-limit filter for auth endpoints
**Description:** Add `bucket4j-core`. Build a per-`(IP, email)` token bucket (5 req/min) applied to `POST /auth/login` and `POST /auth/register`. Return 429 with `Retry-After` on exhaustion. Implement as a Spring filter or interceptor scoped to those endpoints.
**Link:** `[Link to be added]()`

### Ticket: Implement per-user mutation rate limits
**Description:** Bucket4j-backed `@RateLimited(maxPerHour=...)` AOP aspect keyed on the authenticated user id from the `SecurityContext`. Apply: 5 circles/hr on `POST /circles`, 30 messages/min on `POST /threads/{id}/messages`. Return 429 with structured JSON via `@ControllerAdvice`.
**Link:** `[Link to be added]()`

### Ticket: Global validation + exception handler
**Description:** Use Jakarta Bean Validation on DTOs (`@NotBlank`, `@Size`, `@Email`, `@Pattern`). Add `@ControllerAdvice GlobalExceptionHandler` mapping `MethodArgumentNotValidException` → 400 with field-level errors, `AccessDeniedException` → 403, `ResponseStatusException` pass-through, `TokenExpiredException`/`InvalidTokenException` → 401, generic 500 with sanitized payload. Enforce: topic 3–80, description ≤500, message body ≤2000.
**Link:** `[Link to be added]()`

---

## Auth Endpoints

### Ticket: Implement `POST /auth/register`
**Description:** Accept `{ email, password }`. BCrypt-hash, insert user (or no-op if email exists). Always create a single-use `auth_tokens` row with `purpose='verify_email'`, 24h expiry. Send verification email via Resend. Always return 200 with generic copy ("If new, check your inbox") to prevent enumeration. Apply the auth rate-limit filter.
**Link:** `[Link to be added]()`

### Ticket: Implement `GET /auth/verify?token=...`
**Description:** Look up `auth_tokens` row where `purpose='verify_email'`, not used, not expired. In one `@Transactional` block: set `users.email_verified_at = now()` and `auth_tokens.used_at = now()`. Return 200 on success, 400 on invalid/expired/used token.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /auth/login`
**Description:** Accept `{ email, password }`. Always call `PasswordService.matches(...)` (constant time even when user missing). Reject with generic 401 ("Invalid email or password") on mismatch or unverified email. On success, sign `engage-auth` JWT and return `{ engageAuth, user }`. Apply auth rate-limit filter.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /auth/forgot-password`
**Description:** Accept `{ email }`. If user exists, create a single-use `auth_tokens` row with `purpose='reset_password'`, 1h expiry, and email the reset link. Always return 200 with generic copy.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /auth/reset-password`
**Description:** Accept `{ token, newPassword }`. Validate token row. In a transaction: BCrypt-hash new password, update `users.password_hash`, mark token used. Validate password strength (≥8) via Bean Validation.
**Link:** `[Link to be added]()`

### Ticket: Implement `GET /auth/me`
**Description:** Behind the security filter chain. Return `{ user: { id, email, emailVerifiedAt, createdAt } }` for the authenticated principal.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /auth/logout`
**Description:** Returns 204. Optionally insert the token's jti into a `revoked_tokens` table for blacklist; document the stateless-vs-blacklist trade-off in code comments.
**Link:** `[Link to be added]()`

---

## Circles Endpoints

### Ticket: Build scope-aware SQL clause builder for circles
**Description:** Build `CircleScopeClauseBuilder.build(scope, search, userId)` returning a parameterized SQL WHERE fragment + params Map. `MINE` → admin OR membership row exists (scope always applies, even when `search` is non-empty). `DISCOVER` → not admin AND no membership row **when `search` is empty/null**; when `search` is non-empty, the scope filter is dropped and results widen to ALL (search-overrides-scope rule for Discover). `ALL` → no scope filter. Unit test the scope-override matrix: `(scope, search?) → expected WHERE`.
**Link:** `[Link to be added]()`

### Ticket: Implement `GET /circles` with scope + search + sort + pagination
**Description:** Behind security filter. Query params: `scope=all|mine|discover` (default `all`), `sort=popular|newest` (default `popular`), `search` (≤80 chars, trimmed; matches `topic` OR `description` via `pg_trgm` ILIKE), `page` (default 1, min 1), `limit` (**default 10, max 10** — enforced via `@Max(10)` Bean Validation; any value >10 is clamped to 10 server-side). Sort `popular`: `member_count DESC, created_at DESC`; `newest`: `created_at DESC`. Return `{ data, total, page, limit }`. Each row includes computed `isAdmin` and `isMember` for the authenticated user.
**Link:** `[Link to be added]()`

### Ticket: Add GIN index on `circles.description` for search
**Description:** Add Flyway migration `V3__circles_description_trgm.sql` creating `CREATE INDEX idx_circles_description_trgm ON circles USING gin (description gin_trgm_ops);` so the `search` parameter (which matches `topic OR description`) stays index-backed at scale.
**Link:** `[Link to be added]()`

### Ticket: Implement `GET /circles/{id}`
**Description:** Behind security filter. Return the circle with `isAdmin`/`isMember` computed for the caller. 404 if not found.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /circles`
**Description:** Behind security filter + Bean Validation (`topic` 3–80, `description` ≤500). In a `@Transactional` service method: insert circle with `adminUserId = principal.id` and `memberCount = 1`, insert membership for the admin with a generated handle. Apply per-user rate limit (5/hr). Return the created circle.
**Link:** `[Link to be added]()`

### Ticket: Implement `PATCH /circles/{id}` (admin-only, description-only)
**Description:** Accept a strict DTO containing only `description`. Use `@JsonInclude(NON_NULL)` plus a custom Jackson `FAIL_ON_UNKNOWN_PROPERTIES=true` deserializer setting to reject any other key (400). Load circle; 403 via `AccessDeniedException` if `circle.adminUserId != principal.id`. Update description, return updated circle.
**Link:** `[Link to be added]()`

### Ticket: Implement `DELETE /circles/{id}`
**Description:** Behind security filter. Load circle; 403 if not admin. Delete circle (FK ON DELETE CASCADE removes memberships/threads/messages). Return 204.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /circles/{id}/join`
**Description:** 409 if caller is the circle's admin. Idempotent: existing membership returns existing handle. Otherwise insert membership with `HandleGenerator.generate(userId, circleId)`; trigger updates `member_count`. Return `{ handle }`.
**Link:** `[Link to be added]()`

### Ticket: Implement `DELETE /circles/{id}/join`
**Description:** 409 if caller is the admin ("Admins cannot leave; delete the circle instead"). Otherwise delete the membership row (trigger decrements `member_count`). Return 204.
**Link:** `[Link to be added]()`

---

## Threads & Messages Endpoints

### Ticket: Implement `GET /circles/{id}/threads`
**Description:** Behind security filter. Paginated list (`page` default 1, `limit` **default 10, max 10**) of threads in the circle, sorted `created_at DESC`. No membership check on read.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /circles/{id}/threads`
**Description:** Membership check (admin OR membership row exists; 403 otherwise). Validate `title` (3–120 chars). Insert thread with `created_by = membership.handle` snapshot.
**Link:** `[Link to be added]()`

### Ticket: Implement `GET /threads/{id}/messages`
**Description:** Membership check on parent circle (403 otherwise). Paginated list (`page` default 1, `limit` **default 10, max 10**) of messages sorted `created_at ASC`.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /threads/{id}/messages`
**Description:** Membership check on parent circle. Validate `body` ≤ 2000. Insert message with `author = membership.handle`. Apply per-user rate limit (30/min).
**Link:** `[Link to be added]()`

---

## Tests

### Ticket: Testcontainers integration tests — auth lifecycle
**Description:** Spring Boot `@SpringBootTest` + Testcontainers `PostgreSQLContainer` + `MockMvc`/`RestAssured`. Cover: register → verify-email → login returns token; wrong password → generic 401; rate limiter blocks the 6th login attempt within a minute; unverified user cannot log in; `GET /auth/me` requires token; forgot-password + reset-password full flow.
**Link:** `[Link to be added]()`

### Ticket: Testcontainers integration tests — circles + scopes
**Description:** Create three verified users A/B/C. A creates a circle (becomes admin). B joins. Assert `GET /circles?scope=mine` returns the circle for A and B, `?scope=discover` returns it only for C, `?scope=all` for all three. Default sort by `member_count DESC, created_at DESC`.
**Link:** `[Link to be added]()`

### Ticket: Testcontainers integration tests — admin authorization
**Description:** B (non-admin) PATCH on A's circle → 403. A PATCH description → 200. A PATCH with `topic` field present → 400. A DELETE → 204. A attempts to leave own circle → 409.
**Link:** `[Link to be added]()`

### Ticket: Testcontainers integration tests — global auth enforcement
**Description:** Every protected route returns 401 without `Authorization`; malformed token → 401; expired token → 401; valid token for unverified user → 401.
**Link:** `[Link to be added]()`

### Ticket: Testcontainers integration tests — pagination cap and search semantics
**Description:** Seed 25 circles. Assert: `GET /circles?limit=10` returns 10 with `total=25`; `?limit=50` returns 10 (server-side clamp); `?limit=0` returns 400. Search: with three users A (admin of circle X "java"), B (member of X), C (not joined). `GET /circles?scope=mine&search=java` returns X for A and B, empty for C. `GET /circles?scope=discover&search=java` returns X for C (DISCOVER widens to ALL when search is non-empty) **and** for B (already a member — search override). `GET /circles?scope=discover` (no search) returns X only for C (scope filter active). Search across description: create circle Y with `description='spring boot tips'`, `GET /circles?search=spring` returns Y.
**Link:** `[Link to be added]()`
