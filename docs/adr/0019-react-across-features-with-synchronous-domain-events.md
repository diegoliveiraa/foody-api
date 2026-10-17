# ADR-0019 — React across features with synchronous domain events

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

Disabling a user must revoke their refresh tokens. Users live in `user` and refresh tokens in
`auth`, and `auth` already depends on `user`: it reads and creates users to sign people in. A call
from `user` into `auth` closes a cycle between feature packages, which `ArchitectureTest` rejects
(`featurePackagesAreFreeOfCycles`). Orders, catalog and the dashboard will meet the same need — a
change in one feature that another must react to — so the answer has to be one pattern, not a
workaround per case.

## Decision

- The feature that changes publishes a **domain event**: a record named after the fact in the past
  tense (`UserDisabledEvent`), in its own `event` package, carrying ids and values — never
  entities. Its service publishes it through Spring's `ApplicationEventPublisher` after applying the
  change, and only when something actually changed.
- The feature that reacts handles it in an `@EventListener` component in its `service` package,
  which delegates to its own services (`auth.service.UserDisabledListener` →
  `RefreshTokenService.revokeAllOf`).
- Listeners are **synchronous**: they run in the publisher's thread and transaction, so the change
  and every reaction commit or roll back together, and a failing listener fails the request.
  Publishing happens inside a `@Transactional` service method.
- Dependencies between features point one way only: the listener's feature may depend on the
  publisher's, never the reverse. `ArchitectureTest` keeps feature packages free of cycles.
- `@TransactionalEventListener`, `@Async` listeners and an outbox stay out until a reaction must not
  share the publisher's transaction (e-mail, third-party APIs); that gets its own ADR.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| `user` calls `auth`'s service directly | Closes the `user` ↔ `auth` cycle; `ArchitectureTest` fails |
| Revoke the tokens from `user` through `RefreshTokenRepository` | Same cycle, and the refresh-token rules leak into another feature |
| A port in `shared` (`UserDisabledHandler`) that `auth` implements | Every new reaction adds an interface to `shared`, and the publisher has to know who reacts |
| `@TransactionalEventListener(AFTER_COMMIT)` or `@Async` | A failed reaction leaves the user disabled with live refresh tokens; the reaction runs without the publisher's transaction |
| Spring Modulith's event publication registry | A new dependency and its tables, for in-process reactions on a single instance |

## Pros

- Feature packages stay acyclic, and the publisher does not know who listens: adding a reaction
  touches only the feature that reacts.
- The change and its reactions are atomic: a disabled user never keeps a usable refresh token.
- Integration tests cover the reaction through the real endpoint, with no mocks.

## Cons

- The flow is implicit: reading `AdminUserService.disable` does not show that tokens are revoked;
  finding the reactions means searching for the event type.
- A slow or failing listener slows or fails the publisher's request; synchronous reactions must stay
  short and local to the database.
- An event published outside a transaction runs its listeners without one — nothing enforces that
  publishers are `@Transactional`; reviewers must catch it.
- Listeners of the same event run in no guaranteed order unless they declare `@Order`; reactions
  must not depend on each other.
- Side effects that cannot roll back (e-mail, payments) do not fit this pattern and need a new
  decision.
