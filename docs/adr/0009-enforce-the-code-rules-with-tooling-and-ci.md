# ADR-0009 — Enforce the code rules with tooling and CI

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

`CLAUDE.md` holds rules (R1–R20) that only held as long as someone remembered a `grep` before
handing off. Rules checked by memory drift first under pressure, and formatting differences turn
reviews into noise. The repository is still small enough that every rule passes today, which makes
automating them cheap now and expensive later.

## Decision

- **Format:** spring-javaformat — Spring's own style, tabs — validated in the `validate` phase,
  so `./mvnw verify` fails on unformatted code; `./mvnw spring-javaformat:apply` fixes it.
- **Architecture:** `ArchitectureTest` (ArchUnit) enforces layers (R5), packages per role (R9),
  entities extending `BaseEntity` and never reaching controllers (R18), `@ExceptionHandler` only
  in `GlobalExceptionHandler` (R13), no standard streams (R10), no field injection, no
  `java.util.logging`, no generic exceptions thrown. Rules about types that do not exist yet
  accept an empty set and bind from the first class.
- **Comments (R14):** `scripts/check-comments.sh` checks every Java file, ignoring string and char
  literals.
- **CI** (GitHub Actions) on pushes to `main`/`develop` and on pull requests: `./mvnw verify`, the
  committed `docs/openapi.json` unchanged after the tests (ADR-0008), the comment check, then the
  image built and the deploy compose brought up until healthy (ADR-0004).

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Checks by memory (`grep` in `CLAUDE.md`) | The first thing skipped under a deadline |
| Spotless with google-java-format or palantir | Two- or four-space style: a full reformat for no gain over the style already in place |
| Checkstyle or PMD for the architecture | Line-level checks; ArchUnit reads dependencies between classes, which is what R5/R9/R18 are about |
| `grep` for comments | Flags URLs inside strings, so it cannot gate a build |

## Pros

- A rule broken in a PR fails the build before a reviewer has to spot it.
- Formatting never shows up in a diff again.
- The image is proven to start healthy on every change, not only when someone remembers.

## Cons

- `verify` is slower: formatting and architecture checks run on every build.
- Architecture rules are code: a new layer or package convention means changing them, in review.
- The comment check strips literals with patterns, not a parser — Java text blocks are not handled.
- CI needs a GitHub remote to run; until then the gates run only locally.
