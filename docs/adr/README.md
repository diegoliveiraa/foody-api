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
| [ADR-0002](./0002-authenticate-with-short-lived-jwts-and-server-side-refresh-tokens.md) | 2026-10-16 | Authenticate with short-lived JWTs and server-side refresh tokens | Accepted — public endpoint list superseded by ADR-0013 |
| [ADR-0003](./0003-persist-in-postgresql-through-jpa-with-flyway-migrations.md) | 2026-10-16 | Persist in PostgreSQL through Spring Data JPA, with Flyway migrations | Accepted |
| [ADR-0004](./0004-ship-a-jlink-trimmed-jre-on-alpine-with-local-and-deploy-compose.md) | 2026-10-16 | Ship a jlink-trimmed JRE on Alpine, with separate local and deploy compose files | Accepted — local compose file superseded by ADR-0028; memory default superseded by ADR-0029 |
| [ADR-0005](./0005-one-http-and-data-contract-for-every-endpoint.md) | 2026-10-16 | One HTTP and data contract for every endpoint | Accepted — soft delete superseded by ADR-0006; exception bases superseded by ADR-0014 |
| [ADR-0006](./0006-disable-with-disabledat-and-delete-for-real.md) | 2026-10-16 | Disable with `disabledAt`; delete for real; soft delete only by exception | Accepted |
| [ADR-0007](./0007-seed-local-data-with-factories-and-seeders-on-the-test-classpath.md) | 2026-10-16 | Seed local data with factories and seeders on the test classpath | Accepted |
| [ADR-0008](./0008-publish-the-openapi-contract-from-a-test.md) | 2026-10-16 | Publish the OpenAPI contract from a test, keeping springdoc out of production | Accepted |
| [ADR-0009](./0009-enforce-the-code-rules-with-tooling-and-ci.md) | 2026-10-16 | Enforce the code rules with tooling and CI | Accepted |
| [ADR-0010](./0010-log-structured-json-with-a-request-id.md) | 2026-10-16 | Log structured JSON in deploy, every line tied to a request id | Accepted |
| [ADR-0011](./0011-keep-dependencies-current-with-dependabot.md) | 2026-10-16 | Keep dependencies current with Dependabot | Accepted — target branch and platform majors refined by ADR-0030 |
| [ADR-0012](./0012-grant-screens-and-actions-to-roles-in-code.md) | 2026-10-16 | Grant screens and actions to roles in code | Accepted |
| [ADR-0013](./0013-open-registration-and-health-to-anonymous-callers.md) | 2026-10-16 | Open registration and health to anonymous callers | Accepted |
| [ADR-0014](./0014-render-security-failures-and-rate-limits-as-problem-details.md) | 2026-10-16 | Render security failures and rate limits as Problem Details | Accepted |
| [ADR-0015](./0015-load-jwt-signing-keys-from-pem-files-mounted-as-secrets.md) | 2026-10-16 | Load JWT signing keys from PEM files mounted as secrets | Accepted |
| [ADR-0016](./0016-read-time-from-an-injected-clock.md) | 2026-10-16 | Read time from an injected Clock | Accepted |
| [ADR-0017](./0017-rate-limit-in-memory-with-bucket4j.md) | 2026-10-16 | Rate limit in memory with Bucket4j | Accepted |
| [ADR-0018](./0018-every-contract-field-is-required-unless-marked-nullable.md) | 2026-10-16 | Every contract field is required unless marked @Nullable | Accepted |
| [ADR-0019](./0019-react-across-features-with-synchronous-domain-events.md) | 2026-10-17 | React across features with synchronous domain events | Accepted |
| [ADR-0020](./0020-serve-the-menu-unpaginated-and-price-the-cart-on-the-server.md) | 2026-10-17 | Serve the menu unpaginated and price the cart on the server | Accepted |
| [ADR-0021](./0021-name-contract-operations-after-their-controller-and-method.md) | 2026-10-17 | Name contract operations after their controller and method | Accepted |
| [ADR-0022](./0022-snapshot-items-and-address-into-the-order-and-number-orders-from-a-sequence.md) | 2026-10-17 | Snapshot items and address into the order and number orders from a sequence | Accepted |
| [ADR-0023](./0023-query-another-feature-through-an-interface-it-implements.md) | 2026-10-17 | Query another feature through an interface it implements | Accepted |
| [ADR-0024](./0024-drive-the-order-lifecycle-through-one-policy-per-side-endpoints-and-a-single-cancellation-request.md) | 2026-10-17 | Drive the order lifecycle through one policy, per-side endpoints and a single cancellation request | Accepted |
| [ADR-0025](./0025-compute-dashboard-kpis-on-demand-in-the-restaurants-time-zone.md) | 2026-10-17 | Compute dashboard KPIs on demand in the restaurant's time zone | Accepted |
| [ADR-0026](./0026-sort-user-facing-names-with-a-portuguese-icu-collation-on-the-column.md) | 2026-10-17 | Sort user-facing names with a Portuguese ICU collation on the column | Accepted |
| [ADR-0027](./0027-search-names-ignoring-accents-with-unaccent-and-trigram-indexes.md) | 2026-10-17 | Search names ignoring accents with unaccent and trigram indexes | Accepted |
| [ADR-0028](./0028-one-command-runs-a-seeded-demo-stack-separate-from-the-production-image.md) | 2026-10-18 | One command runs a seeded demo stack, separate from the production image | Accepted |
| [ADR-0029](./0029-give-the-api-1-gb-now-that-its-non-heap-memory-outgrew-512-mb.md) | 2026-10-18 | Give the API 1 GB now that its non-heap memory outgrew 512 MB | Accepted |
| [ADR-0030](./0030-dependabot-targets-develop-and-leaves-platform-majors-to-planned-upgrades.md) | 2026-10-18 | Dependabot targets develop and leaves platform majors to planned upgrades | Accepted |
