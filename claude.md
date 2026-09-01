## Stack and project description

- **Primary stack:** Kotlin, Spring Boot (Web, Data JPA), Hibernate/JPA, H2 (embedded, file-based), Flyway for migrations, JUnit 5 + Spring Boot Test, Gradle (Kotlin DSL). See [docs/spec/features/001-mvp-feature-flags](docs/spec/features/001-mvp-feature-flags/spec.md).
- **Frontend:** React + TypeScript + Vite (`frontend/`).
- **Description:** Feature flag manager to enable/disable functionality per client/environment. Provides administrative endpoints to manage flags and client endpoints to resolve flag state for users/clients. Persistent storage via JPA/H2 — no external database or Docker required.

## Project structure

- `backend/` — server code (Kotlin)
  - `src/main/kotlin/com/featureflagmanager/` — entities, repositories, services, controllers, config
  - `src/main/resources/db/migration/` — Flyway schema migrations
  - `src/main/resources/db/seed/` — Flyway seed data (applied only under the `dev` profile)
  - `src/test/kotlin/` — JUnit 5 integration tests (run against in-memory H2, `test` profile)
  - `build.gradle.kts`, `settings.gradle.kts`
- `frontend/` — admin UI (React/Vite)

## Commands

- From `backend`:
  - `./gradlew build` — build
  - `./gradlew bootRun --args='--spring.profiles.active=dev'` — run locally with schema + seed data
  - `./gradlew test` — run tests (no external services needed)

## Conventions

- Kotlin with Spring idioms: constructor injection, `@Version`-based optimistic locking, DTOs at the controller boundary (not exposing entities directly).
- Automated tests for critical features (JUnit 5, in-memory H2).
- Clear separation: controllers → services → repositories.
- Changes to the data model must include a Flyway migration (`db/migration`) and, where relevant, updated seed data (`db/seed`).
- Number features in `docs/spec/features` using a `00X-` prefix to preserve order.

## Don'ts

- Do not place business logic directly inside route handlers.
- Do not modify production migrations without a rollback plan and backups.
- Do not leave failing tests on the main branch.

## General workflow

1. Document the feature in `docs/spec/features/00X-task-name/spec.md` with acceptance criteria.
2. Write `plan.md` describing files, data models, and API requirements.
3. Break the plan into `tasks.md` with small, verifiable tasks.
4. Implement in a `feature/00X-task-name` branch with small PRs.
5. Verify with tests and acceptance criteria; iterate if necessary.

## Starting a new feature (detailed)

Product and architecture documentation lives under `docs/spec/constitution/` (spec-driven structure: what we're building, tech stack, and feature order). New features get their own `docs/spec/features/00X-task-name/{spec,plan,tasks}.md`, following this flow:

1. **Specify** (`spec.md`) — what will be built, with acceptance criteria.
2. **Plan** (`plan.md`) — how it will be built: technical approach, files to change, data models, and API contracts.
3. **Tasks** (`tasks.md`) — the plan broken into small, verifiable tasks.
4. **Implement** — develop tasks in order and open PRs for review.
5. **Verify** — validate against acceptance criteria in `spec.md`. If it fails, adjust the plan and repeat.

Keep documentation versioned and close to the implementation. Prioritize clarity in `spec.md` to reduce iteration.
