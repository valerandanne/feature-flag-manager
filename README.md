# Feature Flag Manager

A feature flag manager to enable/disable functionality per environment. Administrative endpoints
manage flags; an open client endpoint resolves flag state for a given environment.

The API is split into two surfaces — Admin and Client — because they have opposite access
profiles: Admin is low-frequency, audited mutation, used by people; Client is high-frequency,
read-only, used by services. Keeping them separate from day one means future changes to one side
(caching, rate limiting, auth) don't force changes on the other.

- **Backend** (`backend/`): Kotlin + Spring Boot, JPA/Hibernate, embedded H2 (file-based), Flyway.
- **Frontend** (`frontend/`): React + TypeScript + Vite, admin UI for flags.

No Docker, no external database — everything runs with just a JDK and Node installed. See
[docs/spec/features/001-mvp-feature-flags](docs/spec/features/001-mvp-feature-flags/spec.md) for
the full requirements and design rationale, and [docs/spec/constitution](docs/spec/constitution/spec.md)
for the overall stack/architecture.

## Requirements

- JDK 17+
- Node.js + npm

## Setup & run

If you're using [Claude Code](https://claude.com/claude-code), this repo ships two custom slash
commands under `.claude/commands/` that do the steps below for you:

- `/flag-manager-start` — starts the backend and frontend.
- `/run-demo` — starts the backend with seeded demo data and runs the SDK demo.

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

The port can be overridden with `--server.port=<port>` or the `PORT` env var.

#### API docs (Swagger)

With the backend running, browse the interactive API docs at `http://localhost:8080/swagger-ui.html`
(raw OpenAPI spec at `/v3/api-docs`) — generated automatically from the controllers/DTOs, no manual
annotations added yet.

#### Seeded demo data

With the `dev` profile active, you get:

- Environments `development`, `staging`, `production`
- Demo flags (`new-payment-flow`, `homepage-variant`) with different enabled/rollout values per
  environment

#### Inspecting the database

With `dev` active, the H2 web console is available at `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:file:./data/feature-flags`, user `sa`, empty password). Data persists across
restarts under `backend/data/`.

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

# Client snapshot (no auth required)
curl "http://localhost:8080/api/v1/client/features?env=production"

# Update a flag's per-env config (optimistic locking — version must match the current one)
curl -X PATCH -H "Content-Type: application/json" \
  -d '{"enabled": true, "version": 1}' \
  http://localhost:8080/api/v1/flags/new-payment-flow/envs/production
```

## Tests

```bash
cd backend
./gradlew test
```

Integration tests run against an in-memory H2 database (`test` Spring profile) — no external
services required.

## Project structure

```
feature-flag-manager/
├── backend/    Kotlin + Spring Boot API (controller → service → repository)
│   └── src/main/resources/db/   Flyway migrations + seed data
├── frontend/   React + TypeScript admin UI (Vite)
├── sdk/        optional Kotlin client (see Assumptions below)
└── docs/spec/  per-feature spec/plan/tasks
```

## Assumptions

- Single-tenant: no multi-project or per-project API keys (see the MVP spec's Out of scope).
- No authentication on either API surface — acceptable for an internal MVP, not for production.
- The three environments (`development`, `staging`, `production`) are fixed and seeded by the
  migration; there's no UI/API to create new ones.
- A new flag is created disabled (`enabled=false`) across all environments by default.
- `sdk/` is a small optional Kotlin client for consuming the Client API from another service. It's
  outside the original scope of this exercise — included here for visibility, not polish.

## Tradeoffs

- **Minimal security surface:** optimistic locking and per-env config integrity are worked through
  in depth; authentication, rate limiting, and a health endpoint are not — a deliberate scope cut
  for this exercise, not an oversight.
- **Wildcard CORS (`allowedOriginPatterns("*")`):** harmless today with no auth to protect, but a
  latent risk once auth is added if this isn't revisited.
- **UUID primary keys over sequential IDs:** avoids leaking flag count/creation order on the
  unauthenticated Client API, at the cost of a larger key and more index fragmentation than a
  sequential ID.
- **Embedded H2 over PostgreSQL:** clone-and-run with no Docker or external service, but no test
  coverage against a production-grade engine and no access to Postgres-only features.
- **In-process Caffeine cache over a distributed cache (Redis):** the Client read path
  (`resolveFeatures`/`isEnabled`) is cached with a 30s TTL and evicted on Admin writes, at zero
  extra infra cost — but it's per-instance: horizontal scaling multiplies DB load instead of
  reducing it (each instance caches independently and starts cold), so it stops paying off past a
  single instance.
- **Rollout percentage stored but not enforced:** the field exists and is editable end to end, but
  `GET /api/v1/client/features` ignores it rather than faking enforcement with a non-sticky function.
- **Manual `version` field over `ETag`/`If-Match`:** simpler to implement and test for an internal
  API with a single consumer, but not the idiomatic REST mechanism, and needs an app-specific
  convention instead of standard HTTP semantics.
- **Separate Admin/Client controllers over one unified controller:** a bit of duplication today so
  that adding auth to Admin, or caching/rate limiting to Client, later doesn't mean untangling
  logic mixed into a single controller.


## What I'd improve with another day

Ordered by what actually blocks a real deployment, not by effort:

- **Add authentication to both APIs.** Anyone with network access can create or mutate flags via
  `/api/v1/flags`, and anyone can read `/api/v1/client/features` — this is the single biggest gap
  on this list.
- **More configuration and versatility on flags, not just on/off.** Today a flag is a boolean plus
  a `rollout` percentage that isn't even enforced — real usage needs more axes than that 
  (rollout, targetting user, variants).
- **PostgreSQL instead of embedded H2.** H2 keeps the project dependency-free for this exercise,
  but it doesn't hold up under multiple instances or real concurrent load. Bringing Postgres back
  also means bringing back a Testcontainers-backed so a passing build actually means "works against
   what production runs".
- **Actual observability.** No metrics or request timing exist today. Adding
  `spring-boot-starter-actuator` + Micrometer gets RPS and latency percentiles (via
  `http.server.requests`) and HikariCP pool stats for free, exposed at `/actuator/metrics`.
- **Health/readiness endpoint.** Needed by any orchestrator (Kubernetes, ECS) to know when to route
  traffic to an instance or restart it — there's currently no way to ask the service "are you up."
- **A CI pipeline.** "Don't leave failing tests on main" is currently enforced by convention only;
  nothing actually stops a broken build from landing.
- **Pagination on `GET /api/v1/flags`.** Fine at current scale (a handful of flags), but returning
  the full list stops being cheap as it grows — and adding pagination later is a breaking change for
  anything parsing today's response as a bare array, so it's cheaper to get ahead of it than to
  retrofit it.
- **Frontend tests.** No automated coverage on the admin UI — regressions currently rely on manual
  testing.