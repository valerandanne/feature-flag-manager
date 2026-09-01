# Feature Flag Manager

A feature flag manager to enable/disable functionality per client/environment. Administrative
endpoints manage flags; a client endpoint resolves flag state for services by API key.

- **Backend** (`backend/`): Kotlin + Spring Boot, JPA/Hibernate, embedded H2 (file-based), Flyway.
- **Frontend** (`frontend/`): React + TypeScript + Vite, admin UI for projects/flags.

No Docker, no external database — everything runs with just a JDK and Node installed. See
[docs/spec/features/001-mvp-feature-flags](docs/spec/features/001-mvp-feature-flags/spec.md) for
the full requirements and design rationale, and [docs/spec/constitution](docs/spec/constitution/spec.md)
for the overall stack/architecture.

## Requirements

- JDK 17+
- Node.js + npm

## Setup & run

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

#### Seeded demo data

With the `dev` profile active, you get:

- Projects `payments` (key `payments`) and `web-frontend` (key `web-frontend`)
- API keys `payments-mvp-key` (for `payments`) and `search-mvp-key` (for `web-frontend`)
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

## Trying the API directly

```bash
# List projects
curl http://localhost:8080/api/v1/projects

# List flags for a project (use an id from the call above)
curl http://localhost:8080/api/v1/projects/<projectId>/flags

# Client snapshot (use a seeded API key)
curl -H "Authorization: Bearer payments-mvp-key" \
  "http://localhost:8080/api/v1/client/features?env=production"

# Update a flag's per-env config (optimistic locking — version must match the current one)
curl -X PATCH -H "Content-Type: application/json" \
  -d '{"enabled": true, "version": 1}' \
  http://localhost:8080/api/v1/projects/<projectId>/flags/new-payment-flow/env/production
```

## Tests

```bash
cd backend
./gradlew test
```

Integration tests run against an in-memory H2 database (`test` Spring profile) — no external
services required.

## Project structure

- `backend/` — Kotlin/Spring Boot API (`src/main/kotlin/...`, Flyway migrations under
  `src/main/resources/db/`)
- `frontend/` — React/Vite admin UI
- `docs/spec/` — product spec, architecture, and per-feature spec/plan/tasks
