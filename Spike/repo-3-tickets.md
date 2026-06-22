# Repo 3 — `discussions-graph` Tickets

GraphQL BFF federating `discussions-service` (REST) and `genai-service` (HTTP/SSE). **Java 21 + Spring Boot 3.x + Spring for GraphQL + Spring WebFlux.**

---

## Project Foundation

### Ticket: Bootstrap Spring for GraphQL project
**Description:** Initialize Java 21 + Spring Boot 3 project with `spring-boot-starter-graphql`, `spring-boot-starter-webflux` (for reactive resolvers + WebClient), `spring-boot-starter-validation`, `spring-boot-starter-actuator`, plus `com.graphql-java:java-dataloader` (already pulled in transitively). Configure `application.yml` and base packages: `resolver`, `client`, `loader`, `context`, `config`, `security`. Add test deps: `spring-graphql-test`, `webtestclient`.
**Link:** `[Link to be added]()`

### Ticket: Define SDL schema in `schema.graphqls`
**Description:** Author `src/main/resources/graphql/schema.graphqls` with types `User`, `Circle`, `Thread`, `Message`, `AuthResult`; enums `CircleScope { ALL MINE DISCOVER }`, `CircleSort { POPULAR NEWEST }`; union `GenerateDescriptionResult = DescriptionGenerated | DescriptionUnavailable`; queries (`me`, `circles(scope, sort, search, page, limit)`, `circle`, `threads`, `messages`) and mutations (`register`, `verifyEmail`, `login`, `forgotPassword`, `resetPassword`, `logout`, `createCircle`, `updateCircleDescription`, `deleteCircle`, `joinCircle`, `leaveCircle`, `createThread`, `postMessage`, `generateDescription`). Add an SDL comment on the `circles` query documenting the scope-override search rule (`DISCOVER + search` widens to all circles; `MINE + search` stays within the user's circles).
**Link:** `[Link to be added]()`

### Ticket: Configure `GraphQlSource` and per-request context
**Description:** Provide a `RuntimeWiringConfigurer` registering scalar/union type resolvers. Build a `WebGraphQlInterceptor` that extracts `Authorization` from the incoming request, stuffs `engageAuth` and a per-request `DataLoaderRegistry` into the `GraphQLContext`. Disable introspection when `spring.profiles.active=prod`. Add `MaxQueryDepthInstrumentation(7)` and a basic complexity instrumentation.
**Link:** `[Link to be added]()`

---

## Downstream Clients

### Ticket: Implement `DiscussionsClient` (WebClient wrapper)
**Description:** Build `DiscussionsClient` over `WebClient`. Typed methods for every REST endpoint (`getCircles`, `getCircle`, `createCircle`, `patchCircle`, `deleteCircle`, `joinCircle`, `leaveCircle`, `getThreads`, `createThread`, `getMessages`, `postMessage`, auth: `register`, `verifyEmail`, `login`, `forgotPassword`, `resetPassword`, `logout`, `me`). Every method accepts `engageAuth` and forwards `Authorization: Bearer <engageAuth>`. Base URL from `discussions.service.url` config.
**Link:** `[Link to be added]()`

### Ticket: Implement `GenaiClient` with SSE consumption
**Description:** Build `GenaiClient.generate(topic, description?, mode): Flux<String>` over `WebClient`. Attach `X-Internal-Auth: ${internal.service.token}` header on every call. Subscribe to the SSE stream via `bodyToFlux(ServerSentEvent.class)`. Parse JSON payloads; if any event has `{ status: "UNAVAILABLE" }` or the stream errors, signal a typed `GenaiUnavailableException`. Base URL from `genai.service.url`.
**Link:** `[Link to be added]()`

### Ticket: REST → GraphQL error mapping helper
**Description:** Build `RestErrorMapper.map(WebClientResponseException)` returning `graphql.GraphqlErrorException` with extensions: 401 → `UNAUTHENTICATED`, 403 → `FORBIDDEN`, 404 → `NOT_FOUND`, 409 → `CONFLICT`, 400 → `BAD_USER_INPUT`, 429 → `RATE_LIMITED`. Preserve downstream message; never leak internal stack traces.
**Link:** `[Link to be added]()`

---

## DataLoaders

### Ticket: DataLoader for per-circle membership/admin lookup
**Description:** Implement a `BatchLoader<UUID, CircleMembership>` that, given a list of circle ids, makes a single downstream call to resolve `isMember`/`isAdmin` for the current viewer. Register the loader per request through the `WebGraphQlInterceptor`. Wire into the `Circle` type resolvers for `isAdmin` and `isMember` fields.
**Link:** `[Link to be added]()`

---

## Auth Mutations (proxy to discussions-service)

### Ticket: `register(email, password)` mutation
**Description:** `@MutationMapping` on `AuthMutationController`. Calls `DiscussionsClient.register`. Returns generic success status; never reveals whether email existed.
**Link:** `[Link to be added]()`

### Ticket: `verifyEmail(token)` mutation
**Description:** Calls `DiscussionsClient.verifyEmail`. Returns `{ success: Boolean }`. Maps 400 to `BAD_USER_INPUT` via `RestErrorMapper`.
**Link:** `[Link to be added]()`

### Ticket: `login(email, password): AuthResult` mutation
**Description:** Calls `DiscussionsClient.login`. Returns `AuthResult { engageAuth, user }`. Maps 401 to `UNAUTHENTICATED` with generic message.
**Link:** `[Link to be added]()`

### Ticket: `forgotPassword(email)` mutation
**Description:** Calls `DiscussionsClient.forgotPassword`. Always returns success.
**Link:** `[Link to be added]()`

### Ticket: `resetPassword(token, newPassword)` mutation
**Description:** Calls `DiscussionsClient.resetPassword`. Maps 400 to `BAD_USER_INPUT`.
**Link:** `[Link to be added]()`

### Ticket: `logout` mutation
**Description:** Calls `DiscussionsClient.logout` with the forwarded JWT. Always returns success.
**Link:** `[Link to be added]()`

### Ticket: `me: User` query
**Description:** `@QueryMapping` calling `DiscussionsClient.me` with the JWT from `GraphQLContext`. Returns `User`. Consumed by the React `<AuthGuard>`.
**Link:** `[Link to be added]()`

---

## Circles, Threads, Messages

### Ticket: `circles(scope, sort, search, page, limit)` query
**Description:** `@QueryMapping` mapping GraphQL enum `CircleScope` → REST `?scope=` and `CircleSort` → REST `?sort=`. Accepts optional `search: String` (trimmed; forwards to REST `?search=`). Defaults: `sort=POPULAR, page=1, limit=10`. **Hard cap `limit` at 10** — values >10 are clamped at the resolver before forwarding so the downstream cap is never the user-visible failure. Returns paginated `{ data, total, page, limit }`. `Circle.isAdmin`/`isMember` resolved via the DataLoader. Document the scope-override rule for `DISCOVER + search` in the SDL field comment.
**Link:** `[Link to be added]()`

### Ticket: `circle(id): Circle` query
**Description:** Delegates to `GET /circles/{id}`. Returns Circle with `isAdmin`/`isMember`. 404 mapped to `NOT_FOUND`.
**Link:** `[Link to be added]()`

### Ticket: `threads(circleId)` query
**Description:** Delegates to `GET /circles/{id}/threads` with pagination args (`page` default 1, `limit` default 10, **max 10** — clamp at resolver).
**Link:** `[Link to be added]()`

### Ticket: `messages(threadId)` query
**Description:** Delegates to `GET /threads/{id}/messages` with pagination args (`page` default 1, `limit` default 10, **max 10** — clamp at resolver).
**Link:** `[Link to be added]()`

### Ticket: `createCircle(topic, description)` mutation
**Description:** Delegates to `POST /circles`. Returns created Circle. Maps downstream 400/429 cleanly.
**Link:** `[Link to be added]()`

### Ticket: `updateCircleDescription(id, description)` mutation
**Description:** Delegates to `PATCH /circles/{id}` with `{ description }` only. Downstream enforces admin-only; resolver propagates 403 as `FORBIDDEN`.
**Link:** `[Link to be added]()`

### Ticket: `deleteCircle(id)` mutation
**Description:** Delegates to `DELETE /circles/{id}`. Returns `{ success: Boolean }`. Admin enforcement happens downstream.
**Link:** `[Link to be added]()`

### Ticket: `joinCircle(id)` mutation
**Description:** Delegates to `POST /circles/{id}/join`. Returns the updated `Circle` (with `isMember=true`) so the Apollo cache normalizes correctly. Maps 409 (admin) to `CONFLICT`.
**Link:** `[Link to be added]()`

### Ticket: `leaveCircle(id)` mutation
**Description:** Delegates to `DELETE /circles/{id}/join`. Returns updated `Circle`. Maps 409 (admin) to `CONFLICT`.
**Link:** `[Link to be added]()`

### Ticket: `createThread(circleId, title)` mutation
**Description:** Delegates to `POST /circles/{id}/threads`. Returns Thread. Maps 403 (non-member) to `FORBIDDEN`.
**Link:** `[Link to be added]()`

### Ticket: `postMessage(threadId, body)` mutation
**Description:** Delegates to `POST /threads/{id}/messages`. Returns Message. Maps 403 and 429 cleanly.
**Link:** `[Link to be added]()`

---

## GenAI Integration

### Ticket: `generateDescription(topic, description, mode)` mutation with union return
**Description:** `@MutationMapping` calling `GenaiClient.generate`. Buffers the `Flux<String>` and resolves to one of the union variants: success → `Map.of("__typename", "DescriptionGenerated", "text", joined)`; on `GenaiUnavailableException` → `Map.of("__typename", "DescriptionUnavailable", "message", ...)`. Register a `TypeResolver` for the union in the `RuntimeWiringConfigurer`. Any authenticated user may call.
**Link:** `[Link to be added]()`

### Ticket: Stream `generateDescription` output to the client
**Description:** Expose `generateDescription` as a GraphQL **subscription** (returning `Flux<DescriptionChunk>`) or use Spring for GraphQL's incremental delivery support so the React UI can render tokens progressively. Document the chosen approach + client consumption pattern in the resolver header.
**Link:** `[Link to be added]()`

### Ticket: Per-user rate limit on `generateDescription` (10/hr)
**Description:** Bucket4j-backed limiter keyed on the authenticated user id (resolved from the forwarded JWT or a downstream `/auth/me` call cached per request). On exceed, throw a `GraphqlErrorException` with extension `code: RATE_LIMITED`. Document upgrade path to Redis for multi-instance deployments.
**Link:** `[Link to be added]()`

---

## Hardening & Tests

### Ticket: Disable introspection + cap query depth/complexity in production
**Description:** In the `GraphQlSourceBuilderCustomizer`, register `MaxQueryDepthInstrumentation(7)` and a `MaxQueryComplexityInstrumentation`. Conditionally disable introspection in `prod` profile via `spring.graphql.schema.introspection.enabled=false`. Document the limits in README.
**Link:** `[Link to be added]()`

### Ticket: Forward Authorization header on every downstream REST call
**Description:** Centralize the `Authorization` pass-through in a `WebClient` `ExchangeFilterFunction` so resolvers cannot forget it. Add a JUnit test using `MockWebServer` (okhttp3) that asserts the header is present on every method.
**Link:** `[Link to be added]()`

### Ticket: Integration tests — auth pass-through and scope mapping
**Description:** Spring for GraphQL `HttpGraphQlTester` + MockWebServer. Assert: (a) `Authorization` header forwarded on every downstream call; (b) `circles(scope: MINE)` → `GET /circles?scope=mine`; (c) `circles(scope: DISCOVER)` → `?scope=discover`; (d) `circles(scope: DISCOVER, search: "java")` → `?scope=discover&search=java` and the downstream contract widens results (verify via mock); (e) `circles(limit: 50)` is clamped to `limit=10` before forwarding; (f) default sort is `POPULAR`.
**Link:** `[Link to be added]()`

### Ticket: Integration tests — `generateDescription` success + unavailable + rate-limit
**Description:** Mock `GenaiClient`. Assert success returns `DescriptionGenerated`; unavailable returns `DescriptionUnavailable`; 11th call within an hour for the same user returns a `RATE_LIMITED` GraphQL error.
**Link:** `[Link to be added]()`

### Ticket: Integration tests — REST → GraphQL error mapping
**Description:** For each REST status (401/403/404/409/400/429), assert the resolver throws the correct GraphQL error code with a sanitized message via `HttpGraphQlTester`.
**Link:** `[Link to be added]()`
