# Tasks - MVP Feature Flag Manager

1. [x] Scaffold `backend/` — Gradle (Kotlin DSL) + wrapper, Spring Boot starters (web, data-jpa,
   validation), Flyway, H2.
2. [x] Flyway migration for the schema (`FeatureFlag`, `Environment`, `FlagEnv`), H2-compatible
   (`random_uuid()` defaults). Global flag namespace — no `Project`/`ApiKey` tables,
   `feature_flag.name` unique across the whole table.
3. [x] JPA entities (`FeatureFlag`, `Environment`, `FlagEnv` with `@Version`) and Spring Data
   repositories.
4. [x] Implement `AdminController` + service layer: create/list flags, PATCH flag metadata
   (description), PATCH flag env with `@Version`-based optimistic locking → 409 with current state
   on conflict, GET per-env config. Routes under `/api/v1/flags`, no `projectId`.
5. [x] Implement `ClientController` + service layer: `GET /api/v1/client/features` — no
   authentication, returns `{ env, features }`, 400 on unknown env.
6. [x] Add a `@RestControllerAdvice` for consistent error responses (400/404/409) instead of
   leaking raw exception messages.
7. [x] Add `WebConfig` (CORS) so the `frontend/` dev server can call the API cross-origin.
8. [x] Seed data (`db/seed/V2__seed.sql`, `dev` profile only): environments and demo flags with
   per-env config.
9. [x] Integration tests (JUnit 5 + MockMvc, in-memory H2 via a `test` profile) covering: create
   flag, PATCH env happy path + 409 conflict, client features endpoint (known env, unknown env →
   400). `./gradlew test` passes.
10. [x] Manually verify `frontend/` end-to-end against `backend/`: flag listing (no `projectId`),
    CORS preflight, and the running app itself (not just tests). API-level behavior verified via
    `curl` (flag listing, client features, PATCH happy path + 409); visual verification in an
    actual browser confirmed by the user.
11. [x] Write a single root `README.md` covering setup/run/test for both `backend/` and
    `frontend/` (superseding earlier separate per-directory READMEs, per the user's preference
    for one general README). Describes the current (no multi-project) state.
12. [x] Update `docs/spec/constitution/spec.md` with the stack.

## Notes on how we got here

This spec originally described a Node/Express/Prisma implementation, then went through a Kotlin
rewrite (not a line-by-line port — see `spec.md`'s Out of scope for the deliberate differences)
and a PostgreSQL → H2 datastore switch, both at the user's request. The intermediate
`002-kotlin-backend-migration` feature folder that tracked the rewrite has been folded back into
this one so there's a single, current spec for the MVP rather than a spec plus a migration
tracking an old stack that no longer exists.

Multi-project support and API-key auth (`Project`, `ApiKey`, `ApiKeyAuthFilter`) were then cut
from the already-implemented MVP to keep scope aligned with what the challenge actually asks for
— see `spec.md`'s Out of scope. The code has been reworked to match: `Project`/`ApiKey` entities,
repositories, `ProjectService`, and `ApiKeyAuthFilter` are deleted (not reworked), routes moved
from `/api/v1/projects/:projectId/flags...` to `/api/v1/flags...`, and the client endpoint is fully
open. `./gradlew test` passes against the reworked code.
