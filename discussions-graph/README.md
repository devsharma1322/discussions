# discussions-graph

Spring for GraphQL BFF that federates `discussions-service` (REST) and `genai-service` (HTTP/SSE) for the React UI.

## Run locally

> **First time on this machine?** Follow **[../SETUP.md](../SETUP.md)** for prerequisites (JDK 21) and shared-secret configuration across services.

```bash
set -a; source .env; set +a       # exports DISCUSSIONS_SERVICE_URL, GENAI_SERVICE_URL, INTERNAL_SERVICE_TOKEN, UI_ORIGIN
./mvnw spring-boot:run
```

Service listens on `http://localhost:4003`. GraphiQL is enabled in `dev` at `/graphiql`.

> **Important** — `INTERNAL_SERVICE_TOKEN` in this repo's `.env` MUST match the same key in `../genai-service/.env`, otherwise every `generateDescription` call returns 401. `SETUP.md` handles that automatically.

## Layout

```
src/main/java/com/anoncircles/graph/
  resolver/   @Controller-based GraphQL resolvers
  client/     WebClient wrappers for discussions-service and genai-service
  loader/     java-dataloader batch loaders
  context/    GraphContext (per-request: engageAuth + loaders)
  config/     GraphQlConfig, CorsConfig, AuthContextInterceptor
  security/   future hardening (rate limiter etc.)

src/main/resources/graphql/schema.graphqls   SDL: queries, mutations, union, enums
```

Introspection and GraphiQL are disabled in the `prod` profile (`application-prod.yml`).
Query depth is capped at 7 via `graph.query.max-depth`.
