# ADR — Architecture Decision Records

A short record of a **technical decision that constrains future code**. It answers "why is this
like this?" six months later, without PR archaeology.

## When to write one

Write one when the decision **closes a door**:

- A choice between alternatives with a real trade-off (library, protocol, caching strategy).
- A pattern that now applies to everyone (e.g. every request goes through one client).
- A structural change: new cross-cutting module, database swap, API contract break.
- A decision **not** to do something, when the question is likely to come back.

**Don't write one** for: an obvious choice with no alternative, an implementation detail of one
task, a bug fix, or a style rule — code rules live in `CLAUDE.md`.

Rule of thumb: if someone could legitimately redo this choice a year from now without knowing
what you knew, it's an ADR.

## How it works

- One file per decision in this folder, named `NNNN-title-in-kebab.md`, with the ID
  `ADR-NNNN`. Numbers are sequential and **never reused**.
- References — in test names, other docs, PRs — always use the full ID (`ADR-0004`), never the
  number alone.
- ADRs are written in **English**.
- An ADR is **immutable once accepted**. Changed your mind? Write a new one that supersedes it
  and mark the old one `Superseded by ADR-NNNN`.
- One page maximum. If it doesn't fit, the rest is documentation, not a decision.
- The file is **versioned alongside the code it decides** — same PR, not later.
- A new ADR is added to the index below.

## Format

```markdown
# ADR-NNNN — Imperative title stating the decision

- **Status:** Proposed | Accepted | Superseded by ADR-NNNN | Rejected
- **Date:** YYYY-MM-DD
- **Decided by:** name

## Context — why this became a decision
The problem and the real constraint, no solution yet: what hurt, what already
existed, what could not change. Two to five sentences.

## Decision
What now holds, in active voice: "Every X goes through Y". Specific enough that
someone can tell whether they are following it.

## Why this one and not the others
| Alternative | Why not |
If there was no real alternative, it probably isn't an ADR.

## Pros
What you gain, concretely. "It's cleaner" is not a gain; "one failing integration
does not take the others with it" is.

## Cons
What you lose and what becomes forbidden. **An ADR with no cons listed is
marketing, not a record** — if you found none, you didn't look. Include whatever
needs migration, rework or monitoring.
```

## Status

| Status | Meaning |
|---|---|
| `Proposed` | Written, still under discussion — not binding yet |
| `Accepted` | Binding. New code follows it |
| `Superseded by ADR-NNNN` | Replaced; kept in history, never deleted |
| `Rejected` | Discussed and turned down — the record prevents re-litigating it |

## Index

| ID | Date | Decision | Status |
|---|---|---|---|
| [ADR-0001](./0001-navigation-and-permissions-are-served-by-get-me.md) | 2026-10-16 | Navigation and permissions are served by GET /me | Accepted |
| [ADR-0002](./0002-authenticate-with-short-lived-jwts-and-server-side-refresh-tokens.md) | 2026-10-16 | Authenticate with short-lived JWTs and server-side refresh tokens | Accepted |
| [ADR-0003](./0003-persist-in-postgresql-through-jpa-with-flyway-migrations.md) | 2026-10-16 | Persist in PostgreSQL through Spring Data JPA, with Flyway migrations | Accepted |
| [ADR-0004](./0004-ship-a-jlink-trimmed-jre-on-alpine-with-local-and-deploy-compose.md) | 2026-10-16 | Ship a jlink-trimmed JRE on Alpine, with separate local and deploy compose files | Accepted |
| [ADR-0005](./0005-one-http-and-data-contract-for-every-endpoint.md) | 2026-10-16 | One HTTP and data contract for every endpoint | Accepted |
