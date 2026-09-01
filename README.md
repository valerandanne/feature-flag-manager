# Feature Flag Manager

A feature flag manager to enable/disable functionality per environment. Administrative endpoints
manage flags; an open client endpoint resolves flag state for a given environment.

## Main features

- Per-environment flag config (3 environments), optimistic locking on updates — concurrent
  edits get a clean 409 instead of clobbering each other.
- Split Admin/Client APIs: Admin backs the frontend console, Client is what services call.
  `sdk/` has a working example.
- Zero-setup local dev: embedded H2 + Flyway migrations, no Docker or external database needed to
  clone and run.
- In-process caching on the Client read path (30s TTL, invalidated on Admin writes) so flag
  resolution stays fast without extra infra.
- Auto-generated interactive API docs (Swagger UI / OpenAPI) straight from the controllers.
- Special care on exception handling: every error comes back in the same shape, with a stable
  `code` clients can branch on instead of parsing error messages.

## Project structure

```
feature-flag-manager/
├── backend/    Kotlin + Spring Boot API (controller → service → repository)
│   └── src/main/resources/db/   Flyway migrations + seed data
├── frontend/   React + TypeScript admin UI (Vite)
├── sdk/        optional Kotlin client (see Assumptions below)
└── docs/spec/  per-feature spec/plan/tasks
```

## Set up requirements

- JDK 17+
- Node.js + npm

## Setup & run

If you're using [Claude Code](https://claude.com/claude-code), this repo ships two custom slash
commands under `.claude/commands/` that do the steps below for you:

- `/start-flag-manager` — starts the backend and frontend.

Otherwise, follow the manual steps below.

### 1. Backend

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Starts the API on `http://localhost:8080`. The `dev` profile applies the schema migration *and*
seeds demo data (see below); without it, only the schema is created.

To just build a jar and run it directly:

```bash
./gradlew bootJar
java -jar build/libs/feature-flag-backend-0.1.0.jar --spring.profiles.active=dev
```

#### Seeded demo data

With the `dev` profile active, you get:

- Environments `development`, `staging`, `production`
- Demo flags `new-payment-flow` and `homepage-variant` — those are their **key** (the immutable
  identifier used in the API path), each with its own display **name** and different enabled
  values per environment


### 2. Frontend

With the backend running:

```bash
cd frontend
npm install
npm run dev
```

Opens the admin UI (Vite dev server). It calls the backend at `http://localhost:8080` by default
— set `VITE_API_BASE` if you're running the backend elsewhere.

### 3. SDK demo (optional)

With the backend running:

```bash
cd sdk
./gradlew run
```

Polls `new-payment-flow` in `staging` every 2s and prints its state whenever it changes — the
console output includes `curl` commands to flip the flag live from another terminal so you can see
it react.

## Trying the API directly

```bash
# List flags
curl http://localhost:8080/api/v1/flags

# Create a flag: `key` is the immutable API identifier, `name` is a free-text display label
# (max 25 chars). New flags start disabled in every environment.
curl -X POST -H "Content-Type: application/json" \
  -d '{"key": "new-checkout", "name": "New Checkout"}' \
  http://localhost:8080/api/v1/flags

# Update a flag's name/description (its key cannot be changed)
curl -X PATCH -H "Content-Type: application/json" \
  -d '{"name": "New Checkout v2"}' \
  http://localhost:8080/api/v1/flags/new-checkout

# Client snapshot (no auth required) — flags are identified by key
curl "http://localhost:8080/api/v1/client/flags/environments/production"

# Update a flag's per-env config (optimistic locking — a concurrent update returns 409)
curl -X PATCH -H "Content-Type: application/json" \
  -d '{"enabled": true}' \
  http://localhost:8080/api/v1/flags/new-payment-flow/environments/production
```

## Tests

```bash
cd backend
./gradlew test
```

Integration tests run against an in-memory H2 database (`test` Spring profile) — no external
services required.


## Assumptions

- Single-tenant: no multi-project or per-project API keys.
- No authentication on either API surface. Fine for an internal MVP, not for production.
- No UI/API to add environments beyond the three (`development`, `staging`, `production`) seeded by
  the migration.
- New flags start disabled (`enabled=false`) in every environment.
- `sdk/` is a small optional Kotlin client showing how a service would consume the Client API —
  included for visibility, not polished.

## Tradeoffs

- **Sequential (`bigint auto_increment`) primary keys over UUIDs** — simpler and better index
  locality than UUIDs, at the cost of leaking flag count/creation order through the unauthenticated
  Client API (not a concern yet since there's no sensitive data in flag IDs, but worth revisiting
  if that ever changes).
- **Embedded H2 over PostgreSQL** — no coverage against a production-grade engine, no Postgres-only
  features.
- **In-process Caffeine cache over Redis** — no extra infra, but it's per-instance, so it stops
  helping once this runs on more than one node.
- **Separate Admin/Client controllers** — some duplication now, in exchange for auth on Admin or
  caching/rate-limiting on Client later not touching the other side.
- **Wildcard CORS** — fine while there's no auth to protect; needs revisiting once there is.
- **Minimal security surface** — time went into optimistic locking and per-env config integrity;
  auth, rate limiting, and a health endpoint didn't get the same treatment.

## What I'd improve with another day

- **Auth on both APIs.** Anyone can mutate flags via `/api/v1/flags` or read
  `/api/v1/client/features` right now.
- **More than on/off.** Percentage-based rollout, targeting rules, multivariant payloads — a flag
  today is just a boolean.
- **PostgreSQL over H2**, with Testcontainers-backed tests so a green build actually means it works
  against what production runs.
- **Observability.** `spring-boot-starter-actuator` + Micrometer gets RPS, latency percentiles, and
  HikariCP pool stats for free via `/actuator/metrics`.
- **A health/readiness endpoint**, for whatever orchestrator ends up running this.
- **A CI pipeline.** Right now nothing but convention stops a broken build from landing on main.
- **Pagination on `GET /api/v1/flags`**, before the flag count makes returning everything expensive.
- **Frontend tests.** No automated coverage on the admin UI today.
