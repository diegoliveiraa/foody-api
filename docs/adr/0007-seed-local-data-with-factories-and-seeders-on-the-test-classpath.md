# ADR-0007 — Seed local data with factories and seeders on the test classpath

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

Local development, manual testing and E2E need realistic data in volume, created the same way on
every run, and integration tests need valid entities without filling every field by hand. Seed
code in the main source set ships in the production image, and a seed that can run in production
is a risk on its own.

## Decision

- Seed code lives in `src/test/java/com/foody/seed`, outside the application's scanned packages,
  with Datafaker (pt-BR) as a test dependency.
- **Per entity, an `EntityFactory<E>`:** `build()` returns a valid, unsaved entity with fake data;
  `create()`, `create(customizer)` and `createMany(n)` persist it. Integration tests and seeders
  both use it.
- **Per domain, a `Seeder`:** `alreadySeeded()` makes it idempotent, `seed(scale)` creates the
  data, `@Order` sequences dependencies. Each one runs in its own transaction, with base counts
  multiplied by `foody.seed.scale` (`SMALL` ×1, `MEDIUM` ×10, `LARGE` ×100).
- **`./mvnw spring-boot:test-run`** starts `TestFoodyApiApplication`: the API under the `seed`
  profile, against the local compose PostgreSQL, running the seeders on startup.
- Faker draws from a fixed seed (`foody.seed.random-seed`, 42): the same data on every run.
- Test-only entities and migrations live in `com.foody.testsupport` and `db/testsupport`, loaded
  only by the tests that ask for them — `test-run` never creates them in the local database.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| SQL seed scripts in a Flyway location | Fine for fixed rows, poor for volume and variety; not reusable by tests; credentials would need committed hashes |
| Seed code in main behind a profile | Ships in the production jar and image; a wrong profile seeds production |
| Seeding through the HTTP API | Slow at volume, and held back by the endpoints' own auth and validation |
| Fresh random data on every run | Bugs and E2E runs stop being reproducible |

## Pros

- The production jar and image carry no seed code and no Datafaker: seeding production is
  impossible, not merely forbidden.
- One factory per entity serves both tests and local data.
- Volume is one property away, and running again never duplicates data.

## Cons

- Changing the scale or the data of a table already seeded needs a fresh local database.
- Classes in `com.foody.seed` that are tests must be named `*Test`, so the seed scan skips their
  fakes; slice tests there declare `FoodyApiApplication` explicitly.
- Fixed login accounts (an admin) arrive with the auth slice and read their password from `.env`.
- `spring-boot:test-run` is a second way to run the API locally, next to `spring-boot:run`.
