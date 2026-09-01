# Tasks - MVP Feature Flag Manager

1. [x] Scaffold `backend/` — Gradle (Kotlin DSL) + wrapper, Spring Boot starters (web, data-jpa,
   validation), Flyway, H2.
2. [x] Write `V1__init.sql` Flyway migration for the schema (Project, FeatureFlag, Environment,
   FlagEnv, ApiKey), H2-compatible (`random_uuid()` defaults, `"key"` quoted as a reserved word).
3. [x] Add JPA entities (`Project`, `FeatureFlag`, `Environment`, `FlagEnv` with `@Version`,
   `ApiKey`) and Spring Data repositories.
4. [x] Implement `ApiKeyAuthFilter`: resolve `Authorization: Bearer <key>` to a project, reject
   with 401 when missing/invalid.
5. [x] Implement `AdminController` + service layer: create/list projects, create/list flags,
   PATCH flag metadata (description), PATCH flag env with `@Version`-based optimistic locking →
   409 with current state on conflict, GET per-env config.
6. [x] Implement `ClientController` + service layer: `GET /api/v1/client/features` resolving
   project from API key, returning the project's `key` and per-env enabled state, 400 on unknown
   env.
7. [x] Add a `@RestControllerAdvice` for consistent error responses (400/401/404/409) instead of
   leaking raw exception messages.
8. [x] Add `WebConfig` (CORS) so the `frontend/` dev server can call the API cross-origin.
9. [x] Add seed data (`db/seed/V2__seed.sql`, `dev` profile only): environments, `payments`/
   `web-frontend` projects, their API keys, and demo flags with per-env config.
10. [x] Write integration tests (JUnit 5 + MockMvc, in-memory H2 via a `test` profile) covering:
    create project/flag, PATCH env happy path + 409 conflict, client features endpoint (valid
    key, invalid key, missing key). Runs with `./gradlew test`, no external services.
11. [x] Manually verify `frontend/` end-to-end against `backend/`: project/flag listing, CORS
    preflight, and the running app itself (not just tests).
12. [x] Write a single root `README.md` covering setup/run/test for both `backend/` and
    `frontend/` (superseding earlier separate per-directory READMEs, per the user's preference
    for one general README).
13. [x] Update `docs/spec/constitution/spec.md` with the stack.

## Notes on how we got here

This spec originally described a Node/Express/Prisma implementation, then went through a Kotlin
rewrite (not a line-by-line port — see `spec.md`'s Out of scope for the deliberate differences)
and a PostgreSQL → H2 datastore switch, both at the user's request. The intermediate
`002-kotlin-backend-migration` feature folder that tracked the rewrite has been folded back into
this one so there's a single, current spec for the MVP rather than a spec plus a migration
tracking an old stack that no longer exists.
