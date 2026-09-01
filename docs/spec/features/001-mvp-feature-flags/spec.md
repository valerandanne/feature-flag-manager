# MVP: Feature Flag Manager — Spec

## Summary

A minimal Feature Flag Manager that lets teams create and manage feature flags per project and
exposes a client-facing snapshot API so services can fetch flags by API key. Built as a Kotlin /
Spring Boot service (`backend/`) with an embedded H2 database — no external database or Docker
required to run it.

Goals:
- Admin API to manage `Projects` and `Feature Flags` (create, list, update)
- Client API to fetch flags for a service using an API key (no project id required in the request)
- Minimal data model with environments and per-env enabled/rollout
- Optimistic locking on per-environment updates
- Seeded demo data and integration test coverage

Out of scope for MVP:
- Multivariant payloads (deferred)
- Delta/change streams and real-time invalidation (deferred)
- Authentication for Admin (deferred)
- Flag archiving / soft-delete (explicitly removed — see Out of scope below)

## Acceptance Criteria

- Admin API under `/api/v1/projects` supports creating/listing projects and creating/listing/
  updating flags.
- Admin endpoint to update per-env config: `PATCH /api/v1/projects/:projectId/flags/:flagName/env/:envName`
  requires `version` and returns `409` (with the current state) on a stale version.
- Client API `GET /api/v1/client/features` returns `200` with JSON
  `{ project, env, features: [{name, enabled}] }` when called with `Authorization: Bearer <api-key>`,
  where `project` is the project's `key` (not its internal id).
- Seed data (applied only under the `dev` Spring profile) includes environments `development`,
  `staging`, `production`, projects `payments`/`web-frontend`, their API keys
  (`payments-mvp-key`/`search-mvp-key`), and a couple of demo flags with per-env config.
- Integration tests (JUnit 5 + Spring Boot Test + MockMvc, in-memory H2 via a `test` profile)
  validate the client endpoint and the optimistic-locking conflict path — runnable with
  `./gradlew test`, no external services required.

## Data Model (summary)

- `Project`: id (UUID), key (unique), name, createdAt
- `FeatureFlag`: id (UUID), projectId, name (unique per project), description, createdAt, updatedAt
- `Environment`: id (UUID), name (unique) — `development`, `staging`, `production`
- `FlagEnv`: id (UUID), flagId, envId, enabled (boolean), rollout (int 0..100), version (int,
  JPA `@Version` — optimistic locking)
- `ApiKey`: id (UUID), key (unique), projectId, description

Schema lives in `backend/src/main/resources/db/migration/V1__init.sql` (Flyway).

## API Contracts

### Admin

- `POST /api/v1/projects`
  - body: `{ key: string, name: string }`
  - 201 => project

- `GET /api/v1/projects`
  - 200 => list of projects

- `POST /api/v1/projects/:projectId/flags`
  - body: `{ name: string, description?: string }`
  - 201 => flag

- `GET /api/v1/projects/:projectId/flags`
  - 200 => list of all flags for the project

- `PATCH /api/v1/projects/:projectId/flags/:flagName`
  - body: `{ description?: string }`
  - 200 => updated flag

- `PATCH /api/v1/projects/:projectId/flags/:flagName/env/:envName`
  - body: `{ enabled?: boolean, rollout?: number, version: number }`
  - 200 => updated FlagEnv
  - 409 => version conflict, body includes `current` (the FlagEnv's current state)

- `GET /api/v1/projects/:projectId/flags/:flagName/env/:envName`
  - 200 => FlagEnv config

### Client

- `GET /api/v1/client/features?env=production`
  - Header: `Authorization: Bearer <api-key>`
  - 200 => `{ project: string, env: string, features: [{ name, enabled }] }`
  - 401 => invalid/missing key
  - 400 => unknown env

## Testing

- Integration tests (JUnit 5 + Spring Boot Test + MockMvc): seed data in `@BeforeEach`, run
  against an in-memory H2 database (`test` Spring profile). Cover the client endpoint (valid/
  invalid/missing key) and the per-env PATCH (happy path + stale-version 409).

## Migration / Seed

- Flyway runs automatically on startup: `V1__init.sql` (schema) always; `V2__seed.sql` (demo
  data) only under the `dev` profile (`db/seed`, added to `spring.flyway.locations` for that
  profile only).
- Run locally: `./gradlew bootRun --args='--spring.profiles.active=dev'`.

## Out of scope

- **Flag archiving / soft-delete.** An earlier iteration of this spec (and the original Node
  prototype it replaced) had a soft-delete concept (`archived` flag, filtered out of listings and
  client resolution). This was removed entirely at the user's request: no `archived` column, no
  archive endpoint, no filtering — `GET /flags` and the client features endpoint return all of a
  project's flags unconditionally. If archiving is needed later, it should come back as its own
  feature spec.
- **PostgreSQL.** The service was briefly built against PostgreSQL (with Testcontainers-based
  tests) before switching to embedded H2, to keep the project dependency-free (no Docker, no
  external DB). Trade-offs: no test coverage against a production-grade engine, and Postgres-only
  SQL features (JSONB, arrays, extensions) aren't available — neither matters at the current
  scope. If production parity becomes a requirement, swapping back means reintroducing a Postgres
  driver + Testcontainers, and changing `V1__init.sql`'s `random_uuid()` (H2) back to
  `gen_random_uuid()` (Postgres, needs the `pgcrypto` extension).
- Admin auth (API keys / JWT), delta/change-stream + caching (Redis pub/sub or a changes table),
  multivariant payloads and targeting rules — all deferred as future enhancements.
