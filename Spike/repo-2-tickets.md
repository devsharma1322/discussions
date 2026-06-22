# Repo 2 — `genai-service` Tickets

Stateless HTTP microservice that generates circle descriptions via Google Gemini. **Java 21 + Spring Boot 3.x + Spring WebFlux (for SSE streaming).**

---

## Project Foundation

### Ticket: Bootstrap Spring Boot WebFlux project
**Description:** Initialize Java 21 + Spring Boot 3 project with dependencies: `spring-boot-starter-webflux`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, `lombok`, plus the Google Gen AI Java SDK (`com.google.genai:google-genai`). Configure `application.yml` profiles. Test deps: `spring-boot-starter-test`, `reactor-test`, `webtestclient`. Add `.env.example` documenting `GEMINI_API_KEY` and `INTERNAL_SERVICE_TOKEN`.
**Link:** `[Link to be added]()`

### Ticket: Configure server, actuator, and graceful shutdown
**Description:** Wire the WebFlux server on `PORT` (default 4002). Enable `spring.lifecycle.timeout-per-shutdown-phase` and `server.shutdown=graceful`. Expose `/actuator/health` (public). Register the `InternalAuthWebFilter` for everything except `/actuator/health`.
**Link:** `[Link to be added]()`

---

## Auth & Security

### Ticket: Implement `InternalAuthWebFilter`
**Description:** Reactive `WebFilter` that reads `X-Internal-Auth` and compares to `INTERNAL_SERVICE_TOKEN` (loaded via `@ConfigurationProperties`) using `MessageDigest.isEqual(...)` for constant-time comparison. Returns 401 on missing/mismatch via `ServerHttpResponse.setStatusCode`. Never log the actual header value. Skip the filter for `/actuator/health`.
**Link:** `[Link to be added]()`

### Ticket: Implement per-source rate limiting
**Description:** Add Bucket4j and apply a 60 req/min limit per source IP on `/generate` as defense-in-depth. Return 429 with `Retry-After`. Implement as a reactive filter ordered after the internal-auth filter.
**Link:** `[Link to be added]()`

---

## Prompt & Generation

### Ticket: Build pure `PromptBuilder`
**Description:** Implement `PromptBuilder.build(GenerateRequest req): Prompt` returning a record `Prompt(String systemInstruction, String userMessage)`. `FROM_TOPIC` mode → ask for a welcoming 2–3 sentence description of the topic. `FROM_TOPIC_AND_DESCRIPTION` mode → ask the model to polish the user's draft while preserving intent. Pure function, no I/O.
**Link:** `[Link to be added]()`

### Ticket: Implement `POST /generate` with Gemini streaming
**Description:** Behind `InternalAuthWebFilter`. Controller accepts a Jakarta-validated `GenerateRequest { topic 3–80, description? ≤500, mode: enum }`. Validate that `description` is non-null when `mode == FROM_TOPIC_AND_DESCRIPTION`. Call the Google Gen AI Java SDK on `gemini-2.5-flash` using its streaming API and adapt the resulting `Iterable`/`Stream` to a `Flux<String>`. Return `Flux<ServerSentEvent<String>>` from the controller (Spring WebFlux maps this to `text/event-stream`).
**Link:** `[Link to be added]()`

### Ticket: Graceful UNAVAILABLE fallback for Gemini errors
**Description:** Use `Flux.onErrorResume` around the Gemini stream. On any failure (quota, safety, network, timeout) emit a single SSE event with JSON payload `{"status":"UNAVAILABLE","message":"..."}` followed by an `event: done` SSE event, then complete. Never bubble 5xx to the client. Log the underlying error server-side with redacted prompt context.
**Link:** `[Link to be added]()`

### Ticket: Expose `GET /actuator/health` liveness endpoint
**Description:** Public via Spring Boot Actuator (`management.endpoints.web.exposure.include=health`). Bypass `InternalAuthWebFilter`. Returns no sensitive info, no upstream check.
**Link:** `[Link to be added]()`

---

## Tests

### Ticket: Unit tests for `PromptBuilder`
**Description:** JUnit 5 tests covering both modes, very short topic (3 chars), draft with typos, draft at 500-char boundary, mode mismatch (description missing when required). Assert structure of `Prompt.systemInstruction` and `Prompt.userMessage`.
**Link:** `[Link to be added]()`

### Ticket: WebTestClient integration test for `InternalAuthWebFilter`
**Description:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `WebTestClient`. Assert: `POST /generate` without header → 401; with wrong token → 401; with correct token + valid body → 200 and `text/event-stream` content. `GET /actuator/health` returns 200 with no header.
**Link:** `[Link to be added]()`

### Ticket: WebTestClient integration test for UNAVAILABLE fallback
**Description:** Replace the Gemini client bean with a mock that throws on the streaming call. Assert `POST /generate` returns 200 with an SSE body containing `{"status":"UNAVAILABLE",...}` and a `done` event; no 5xx ever surfaces.
**Link:** `[Link to be added]()`
