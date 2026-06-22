# discussions-service

Java 21 + Spring Boot REST API for AnonCircles. Auth (JWT `engage-auth`), circles, memberships, threads, messages on PostgreSQL.

## Run locally

> **First time on this machine?** Follow **[../SETUP.md](../SETUP.md)** — it walks through installing JDK 21, Postgres, configuring all 4 `.env` files, and booting every service in order. The instructions below assume that's already done.

```bash
# Already configured .env and have Postgres running? Just boot:
set -a; source .env; set +a
./mvnw spring-boot:run
```

Service listens on `http://localhost:4001`. Liveness: `GET /actuator/health`.

### Postgres options

- **Native (recommended)** — `brew services start postgresql@16 && createdb anoncircles` (no Docker needed)
- **Docker** — `docker compose up -d` (requires Docker Desktop installed)

## Layout

```
src/main/java/com/anoncircles/discussions/
  controller/   REST controllers (auth, circles, threads, messages)
  service/      Business logic, @Transactional boundaries
  repository/   JdbcTemplate-based repositories
  domain/       Entity records
  dto/          Request/response records
  security/     EngageAuthFilter, security config
  config/       DatasourceConfig and other @Configuration classes
  exception/    Domain exceptions + @ControllerAdvice
  lib/          Pure helpers (JWT, handles, password)
```

Migrations: `src/main/resources/db/migration/` (Flyway runs on boot).
