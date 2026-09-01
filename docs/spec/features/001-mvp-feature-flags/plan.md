# Plan - MVP Feature Flag Manager

## Approach

A single Spring Boot service (`backend/`) implementing the requirements in `spec.md` directly —
no separate services, no message queues, no external database. Layering: controllers → services →
repositories, with entities never exposed directly through the API (DTOs at the controller
boundary).

Stack:

- **Language/build:** Kotlin + Gradle (Kotlin DSL)
- **Web framework:** Spring Boot (Web MVC)
- **Data access:** Spring Data JPA / Hibernate
- **Database:** H2, embedded — file-based (`./data/feature-flags`) for local/dev runs, in-memory
  for tests. No Docker, no external DB process.
- **Migrations:** Flyway — `db/migration` (schema, always applied) and `db/seed` (demo data, only
  under the `dev` Spring profile)
- **Client identification (no auth):** the client endpoint is open, with no client identification
  mechanism.
- **Optimistic locking:** JPA's native `@Version` on `FlagEnv` — a concurrent update that races
  past a stale read fails at flush time with a `409` and the current state
- **Error handling:** a `@RestControllerAdvice` mapping domain exceptions to consistent
  `{ error, current? }` JSON bodies with correct status codes (400/401/404/409)
- **Tests:** JUnit 5 + Spring Boot Test + MockMvc, against in-memory H2 via a `test` Spring
  profile — `./gradlew test` needs no external services

## Files

- `backend/build.gradle.kts`, `settings.gradle.kts`
- `backend/src/main/kotlin/com/featureflagmanager/`
  - `FeatureFlagManagerApplication.kt`
  - `entity/{FeatureFlag,Environment,FlagEnv}.kt`
  - `repository/Repositories.kt` (Spring Data JPA interfaces)
  - `service/{FlagService,ClientFlagService,DomainExceptions}.kt`
  - `controller/{AdminController,ClientController,Dtos,GlobalExceptionHandler}.kt`
  - `config/WebConfig.kt` (CORS, so the `frontend/` dev server can call the API)
  - `config/CacheConfig.kt` (in-process Caffeine caches for the Client read path)
- `backend/src/main/resources/`
  - `application.yml` (default profile: H2 file DB, schema-only Flyway; `dev` profile: adds seed
    migrations + H2 console; `test` profile: in-memory H2)
  - `db/migration/V1__init.sql` — schema
  - `db/seed/V2__seed.sql` — demo data (`dev` profile only)
- `backend/src/test/kotlin/com/featureflagmanager/FeatureFlagIntegrationTest.kt`

## Data model

- `FeatureFlag`: id (bigint, auto-increment), key (unique, immutable identifier), name (free-text
  display label, max 25 chars), description, createdAt, updatedAt
- `Environment`: id (bigint, auto-increment), name (unique) — seeded with
  `development`/`staging`/`production`
- `FlagEnv`: id (bigint, auto-increment), flag (FK), env (FK), enabled, version (`@Version`),
  updatedAt — unique on (flag, env)

## API

See `spec.md`'s API Contracts section for the full request/response shapes. Routes:

- `POST /api/v1/flags`, `GET /api/v1/flags`
- `PATCH /api/v1/flags/:flagKey`
- `PATCH /api/v1/flags/:flagKey/environments/:envName`
- `GET /api/v1/flags/:flagKey/environments/:envName`
- `GET /api/v1/client/flags/environments/:env` (open, no auth)
- `GET /api/v1/client/flags/:flagKey/environments/:env` (open, no auth)

`frontend/` (React/Vite) only calls the admin routes above; its default `VITE_API_BASE` points at
`http://localhost:8080`, matching `backend/`'s default port.
