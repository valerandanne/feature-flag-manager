## Stack and project description

- **Primary stack:** Node.js, TypeScript, an HTTP framework (Express or similar), Prisma (Postgres), Jest for tests, and `ts-node`/`ts-node-dev` for development.
- **Description:** Feature flag manager to enable/disable functionality per client/environment. Provides administrative endpoints to manage flags and client endpoints to resolve flag state for users/clients. Persistent storage via Prisma.

## Project structure

- `backend/` — server code (TypeScript)
  - `src/` — source code (`index.ts`, `prismaClient.ts`, middleware, routes)
  - `prisma/` — Prisma schema, migrations and seed
  - `test/` — Jest tests
  - `package.json`, `tsconfig.json`, `jest.config.cjs`

## Commands

- From `backend`:
  - `npm install` — install dependencies
  - `npm run dev` — start in development mode (if provided)
  - `npm test` — run tests
  - `npx prisma migrate dev` — apply migrations in development
  - `npx ts-node --transpile-only prisma/seed.ts` — run the seed script

## Conventions

- TypeScript with `strict: true`.
- Automated tests for critical features (Jest).
- Clear separation: routes → controllers/handlers → services → repositories.
- Changes to `schema.prisma` must include a migration and updated seed.
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
