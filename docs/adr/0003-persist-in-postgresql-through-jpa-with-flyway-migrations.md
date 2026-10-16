# ADR-0003 — Persist in PostgreSQL through Spring Data JPA, with Flyway migrations

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

Users, refresh tokens (ADR-0002) and grants (ADR-0001) need durable storage, and foody-api had
none. Three things had to be chosen together: the database engine, how the code talks to it, and
how the schema evolves without drifting between environments.

## Decision

- PostgreSQL is the only database. Locally it runs in Docker Compose on `localhost`
  (standing rule 1).
- Code talks to it through Spring Data JPA (Hibernate): entities plus repository interfaces, and
  repositories are the only layer that touches the database (R5).
- The schema changes only through Flyway migrations in `src/main/resources/db/migration/`
  (`V<n>__<description>.sql`), applied at startup. Hibernate only validates the schema
  (`ddl-auto=validate`) — it never creates or alters a table.
- An applied migration is never edited; a fix is a new migration.
- `spring.jpa.open-in-view=false`: lazy loading ends with the service's transaction.
- Tests that touch the database run against a real PostgreSQL in Testcontainers.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| MySQL / MariaDB | No gain for this app; PostgreSQL has richer types (UUID, JSONB, arrays) and stricter SQL |
| JDBC or jOOQ without an ORM | More hand-written mapping for a CRUD-heavy domain |
| Hibernate `ddl-auto=update` | Unreviewed schema changes that other environments cannot reproduce, and no safe renames or drops |
| Liquibase | XML/YAML changelogs where plain SQL is enough |
| H2 in tests | Another SQL dialect: tests pass where PostgreSQL fails |

## Pros

- Every schema change is a reviewed, versioned SQL file, applied the same way everywhere.
- Startup fails fast when entities and schema disagree.
- Tests exercise the real database's constraints, types and SQL.

## Cons

- ORM pitfalls to watch: N+1 queries, lazy loading outside a transaction (now an error, not a
  hidden query), and dirty checking writing updates nobody asked for.
- Entities and migrations change in pairs; forgetting either fails startup.
- Docker is required to run the API locally and to run the database tests.
- Migrations are PostgreSQL SQL; changing engines means rewriting them.
