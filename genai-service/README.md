# genai-service

Stateless Spring Boot WebFlux microservice that generates AnonCircles description text via Google Gemini. Behind the `X-Internal-Auth` shared secret.

## Run locally

> **First time on this machine?** Follow **[../SETUP.md](../SETUP.md)** for prerequisites and shared-secret configuration.

```bash
set -a; source .env; set +a       # exports GEMINI_API_KEY, INTERNAL_SERVICE_TOKEN
./mvnw spring-boot:run
```

Service listens on `http://localhost:4002`. Liveness: `GET /actuator/health` (public).

### Provider selection

`GEMINI_API_KEY` in `.env` drives which provider Spring wires up:

| `GEMINI_API_KEY` value | Active provider | Behaviour |
|---|---|---|
| empty (default) | `MockGenaiProvider` | Templated text, ~80 ms/chunk, no network |
| real key from [aistudio.google.com/apikey](https://aistudio.google.com/apikey) | `GeminiGenaiProvider` | Calls `gemini-2.5-flash`, real streaming |

`INTERNAL_SERVICE_TOKEN` MUST match the same key in `../discussions-graph/.env`.

## Layout

```
src/main/java/com/anoncircles/genai/
  controller/   GenerateController (SSE)
  service/      Gemini client wrapper
  prompt/       Pure PromptBuilder (unit-testable, no I/O)
  security/     InternalAuthWebFilter
  config/       Bucket4j rate limiter, beans
```
