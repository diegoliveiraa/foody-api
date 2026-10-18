# ADR-0030 — Dependabot targets develop and leaves platform majors to planned upgrades

- **Status:** Accepted
- **Date:** 2026-10-18
- **Decided by:** Diego

## Context — why this became a decision

ADR-0011 set Dependabot to propose every update weekly, majors included, as pull requests. When the
repository reached GitHub, its first run opened a pull request against `main` that moved the
`eclipse-temurin` images from Java 21 to 24 — a release without long-term support, which also
downgraded Alpine. `main` and `develop` now accept changes only through pull requests with green CI,
and changes reach `main` through `develop`. A Java or PostgreSQL major is a platform decision that
touches `pom.xml`, both Dockerfiles, CI and the data, never a routine bump.

## Decision

- Every Dependabot update targets **`develop`** (`target-branch: develop`), like any other change.
- **Platform majors are ignored:** `eclipse-temurin` (the Java runtime and JDK) and `postgres`
  (the database in the compose files). Their minor and patch updates still arrive.
- Majors of libraries (Maven) and of GitHub Actions still arrive as pull requests, to be judged on
  their own. Everything else in ADR-0011 stands.
- A platform major is a planned change in its own pull request: for Java, the next LTS, moving
  `java.version`, both Dockerfiles and the CI setup together; for PostgreSQL, with a data upgrade
  path.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Keep proposing platform majors | Proposals like Java 21 → 24 (no LTS, older Alpine) get opened and closed every week |
| Ignore every major | Library majors with security or bug fixes would go unnoticed |
| Pull requests against `main` | Skips `develop`, the branch every change goes through |

## Pros

- The weekly pull requests are the ones worth merging.
- Updates follow the same path as any change: `develop`, green CI, then `main`.

## Cons

- Nobody is reminded of a new Java or PostgreSQL major: checking for one is a manual habit.
- The ignore rules name images: a new platform image needs its own rule.
