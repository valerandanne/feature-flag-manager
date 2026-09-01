
# Product Constitution and Architecture

## Purpose

This document captures the product vision, technology stack, and feature prioritization.

## Stack

- Backend: Kotlin + Spring Boot (`backend/`), see [001-mvp-feature-flags](../features/001-mvp-feature-flags/spec.md)
- Data access: Spring Data JPA / Hibernate + H2 (embedded, file-based) — no external database or Docker required
- Migrations: Flyway
- Tests: JUnit 5 + Spring Boot Test (in-memory H2)
- Build: Gradle (Kotlin DSL)
- Frontend: React + Vite + TypeScript (`frontend/`)

## Principles

- Spec-driven development: each feature is specified before implementation.
- Versioned documentation alongside code.
