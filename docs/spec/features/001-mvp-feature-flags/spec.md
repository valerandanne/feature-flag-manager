# MVP: Feature Flag Manager — Spec

## Summary

A minimal Feature Flag Manager that lets teams create and manage feature flags, with a
client-facing snapshot API so services can fetch flag state per environment. Built as a Kotlin /
Spring Boot service (`backend/`) with an embedded H2 database — no external database or Docker
required to run it.

Flags live in a single, global namespace (no multi-project/multi-tenant concept — see Out of
scope). The Admin and Client APIs are kept as two separate surfaces, because they have
opposite access profiles regardless of whether there's one flag namespace or many: Admin is
low-frequency, audited mutation, used by people; Client is high-frequency, read-only, used by
services. Keeping them separate from day one means future optimizations on one side (caching,
rate limiting, auth) don't force changes on the other.

Goals:
- Admin API to manage `Feature Flags` (create, list, update)
- Client API to fetch flag state for a given environment — no authentication required for the MVP
- Minimal data model with environments and per-env enabled/rollout
- Optimistic locking on per-environment updates
- Seeded demo data and integration test coverage

Out of scope for MVP:
- Multi-project support / API keys per project (explicitly cut — see Out of scope below)
- Multivariant payloads (deferred)
- Delta/change streams and real-time invalidation (deferred)
- Authentication for Admin (deferred)
- Flag archiving / soft-delete (explicitly removed — see Out of scope below)

## Acceptance Criteria

- Admin API under `/api/v1/flags` supports creating/listing flags and updating a flag's metadata
  and per-env config.
- Admin endpoint to update per-env config: `PATCH /api/v1/flags/:flagName/envs/:envName` requires
  `version` and returns `409` (with the current state) on a stale version.
- Client API `GET /api/v1/client/features` returns `200` with JSON
  `{ env, features: [{name, enabled}] }` — no authentication required.
- Seed data (applied only under the `dev` Spring profile) includes environments `development`,
  `staging`, `production`, and a couple of demo flags with per-env config.
- Integration tests (JUnit 5 + Spring Boot Test + MockMvc, in-memory H2 via a `test` profile)
  validate the client endpoint and the optimistic-locking conflict path — runnable with
  `./gradlew test`, no external services required.

## Data Model (summary)

- `FeatureFlag`: id (UUID), name (unique), description, createdAt, updatedAt
- `Environment`: id (UUID), name (unique) — `development`, `staging`, `production`
- `FlagEnv`: id (UUID), flagId, envId, enabled (boolean), rollout (int 0..100), version (int,
  JPA `@Version` — optimistic locking)

Schema lives in `backend/src/main/resources/db/migration/V1__init.sql` (Flyway).

## API Contracts

### Admin

- `POST /api/v1/flags`
  - body: `{ name: string, description?: string }`
  - 201 => flag

- `GET /api/v1/flags`
  - 200 => list of all flags, each with its per-env config embedded:
    `{ name, description, envs: [{ env, enabled, rollout, version }] }`.
    An env with no `FlagEnv` row yet is simply absent from the list. This lets a client (e.g. the
    admin frontend) load once and switch between environments without extra requests. No
    pagination — see Out of scope below.
  - Responses identify flags and envs by name (`name`/`env`), not by their internal UUID `id` — the
    UUID primary key exists for storage/index reasons (see the Primary key design note in the
    README) but isn't part of the API contract, since the name is what the client used to look the
    resource up in the first place.

- `PATCH /api/v1/flags/:flagName`
  - body: `{ description?: string }`
  - 200 => updated flag

- `PATCH /api/v1/flags/:flagName/envs/:envName`
  - body: `{ enabled?: boolean, rollout?: number, version: number }`
  - 200 => updated FlagEnv (`{ flagName, env, enabled, rollout, version }`)
  - 409 => version conflict, body includes `current` (the FlagEnv's current state). Two paths lead
    here: a manual check in `FlagService` (stale read — the common case, a client read an old
    version before writing) and Hibernate's own `@Version` check at flush time (a true
    simultaneous write-write race, where both requests pass the manual check before either
    commits). Both are mapped to the same clean `409`/`current` shape — see
    `GlobalExceptionHandler.handleConcurrentModification`. See the README's Optimistic locking
    design note for why this is a manual check rather than relying on JPA's `@Version` alone, and
    for the `ETag`/`If-Match` alternative considered and deferred.

- `GET /api/v1/flags/:flagName/envs/:envName`
  - 200 => FlagEnv config

### Client

- `GET /api/v1/client/features?env=production`
  - 200 => `{ env: string, features: [{ name, enabled }] }`
  - 400 => unknown env
  - `enabled` reflects the flag's `FlagEnv.enabled` only; `rollout` is not applied to this
    response — see Out of scope below.

- `GET /api/v1/client/features/:name`
  - Query: `env` (default `production`)
  - 200 => `{ name, enabled }` for a single flag

### Errors

All error responses share one shape: `{ error: string, code: string, current?: object }`. `code` is
a stable, machine-readable identifier (`FLAG_NOT_FOUND`, `ENV_NOT_FOUND`, `FLAG_ENV_NOT_FOUND`,
`FLAG_ALREADY_EXISTS`, `INVALID_ENV`, `VALIDATION_ERROR`, `VERSION_CONFLICT`) meant for clients to
branch on programmatically instead of parsing `error`'s free-text message, which is for humans/logs
and can change wording without being a breaking change.

## Testing

- Integration tests (JUnit 5 + Spring Boot Test + MockMvc): seed data in `@BeforeEach`, run
  against an in-memory H2 database (`test` Spring profile). Cover the client endpoint (unknown
  env => 400, known env => 200) and the per-env PATCH (happy path + stale-version 409).

## Migration / Seed

- Flyway runs automatically on startup: `V1__init.sql` (schema) always; `V2__seed.sql` (demo
  data) only under the `dev` profile (`db/seed`, added to `spring.flyway.locations` for that
  profile only).
- Run locally: `./gradlew bootRun --args='--spring.profiles.active=dev'`.

## Out of scope

- **Metrics (RPS, per-endpoint latency percentiles, DB pool stats).** No metrics or per-request
  timing instrumentation of any kind is in the codebase. A dependency-free option was considered —
  a `HandlerInterceptor` logging one line per request (method, path, status, duration in ms) — but
  was deliberately not added: a log line isn't a metric, and it doesn't get you any of RPS (needs
  a counter aggregated over a time window), latency percentiles (needs a histogram structure, e.g.
  HdrHistogram), or HikariCP pool stats (connections in use, acquire time, max tx duration —
  exposed internally by HikariCP but need a binder to make them queryable). Building a half
  version that only gives per-call durations isn't worth the code for what it delivers. All four
  of the above are exactly what Micrometer already does, auto-configured, the moment you add
  `spring-boot-starter-actuator`: `http.server.requests` (a `Timer`, tagged by `uri`/`status`,
  gives you RPS via its count and percentiles via `management.metrics.distribution.percentiles-histogram.http.server.requests=true`)
  and `hikaricp.connections.*`/`hikaricp.connections.acquire` (auto-registered against the
  existing `HikariDataSource` bean) via `/actuator/metrics` (and `/actuator/prometheus` if
  scraped). That one dependency is the correct next step whenever real metrics are actually
  needed, not a hand-rolled substitute.
- **Percentage-based rollout enforcement.** `FlagEnv.rollout` is stored and editable via the
  Admin API/UI, but `GET /api/v1/client/features` ignores it — a flag with `enabled=true` is
  reported as enabled to every client regardless of `rollout`. Real enforcement needs a stable
  per-client identity to bucket on, which doesn't exist yet (see the multi-project bullet below),
  so wiring rollout to it now would be premature. The intended
  design once client identity exists: deterministic bucketing — `hash(flagName + clientId) % 100
  < rollout` — so a given client always gets the same result for a given flag/rollout value
  instead of a coin flip per request. This also makes rollout increases "sticky": raising 10% to
  20% keeps everyone already included, only adding new clients, rather than reassigning everyone.
- **Multi-project support / API keys per project.** An earlier iteration of this spec had flags
  scoped to a `Project` entity, with `ApiKey`s scoped to a project and resolved via
  `Authorization: Bearer <key>` on the client endpoint. This was cut: it's effectively an
  authentication mechanism (service-to-service instead of human) layered on top of a
  multi-tenancy model, and the MVP doesn't need either — one Spring Boot service has one global
  flag namespace, and the client endpoint is open. If multiple services need isolated namespaces
  and independent access control later, the natural next step is reintroducing `Project` as an
  entity, with `ApiKey` scoped to a project and the client endpoint resolving the project from the
  key instead of returning a single global set of flags.
- **Flag archiving / soft-delete.** An earlier iteration of this spec (and the original Node
  prototype it replaced) had a soft-delete concept (`archived` flag, filtered out of listings and
  client resolution). This was removed entirely at the user's request: no `archived` column, no
  archive endpoint, no filtering — `GET /flags` and the client features endpoint return all flags
  unconditionally. If archiving is needed later, it should come back as its own feature spec.
- **Flag deletion.** There is no `DELETE /api/v1/flags/:flagName` (or equivalent for a `FlagEnv`).
  Explicitly out of scope for the MVP, not an oversight: flags are expected to be long-lived
  toggles that get disabled rather than removed, and a hard delete raises questions (audit trail,
  what happens to historical data, whether it should really be a soft-delete instead — see the
  bullet above) that don't need answers yet. If deletion is needed later, it should come back as
  its own feature spec rather than being bolted on.
- **PostgreSQL.** The service was briefly built against PostgreSQL (with Testcontainers-based
  tests) before switching to embedded H2, to keep the project dependency-free (no Docker, no
  external DB). Trade-offs: no test coverage against a production-grade engine, and Postgres-only
  SQL features (JSONB, arrays, extensions) aren't available — neither matters at the current
  scope. If production parity becomes a requirement, swapping back means reintroducing a Postgres
  driver + Testcontainers, and changing `V1__init.sql`'s `random_uuid()` (H2) back to
  `gen_random_uuid()` (Postgres, needs the `pgcrypto` extension).
- Admin auth (API keys / JWT), delta/change-stream + caching (Redis pub/sub or a changes table),
  multivariant payloads and targeting rules — all deferred as future enhancements.
- **Pagination on `GET /api/v1/flags`.** Returns the full flag list as a plain top-level JSON
  array. Fine at MVP scale (a handful to a few dozen flags); revisit once the flag count grows
  enough that fetching everything on every admin page load stops being cheap. Note that adding
  pagination later (`?page=&size=`, plus wrapping the response in an object carrying `{ items,
  page, ... }` metadata) would be a breaking change for any existing client parsing the response as
  a bare array — worth keeping in mind if/when this needs to change.
