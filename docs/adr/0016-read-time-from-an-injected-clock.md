# ADR-0016 — Read time from an injected Clock

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

Access tokens expire in 15 minutes, refresh tokens in 7 days and refresh families in 30, and
orders will soon record when they were placed and changed. Tests must cross those boundaries
without waiting, and the code that issues a token and the code that validates it must agree on
"now". `Instant.now()` scattered through the code makes both impossible.

## Decision

- One `Clock` bean, `Clock.systemUTC()`, declared in `com.foody.api.shared.time`. New code reads
  the current instant from it (`clock.instant()`) — token issue and expiry, refresh lifetimes,
  `JwtTimestampValidator.setClock(clock)` and future domain timestamps.
- Tests that cross a time boundary use `MutableClock` (`com.foody.testsupport.time`), registered as
  the primary `Clock` of the integration test context. It follows the system clock until a test
  fixes or advances it, and the test resets it afterwards.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| `Instant.now()` and waiting in tests | A 30-day rule cannot be tested by waiting |
| Static mocking of `Instant.now()` | Bound to one thread, invisible in the code, fragile across libraries |
| A `TimeProvider` interface of our own | `java.time.Clock` already is that abstraction, and Spring Security accepts it |

## Pros

- Expiry rules are tested deterministically, to the second.
- Issuing and validating tokens read the same clock.
- One place to change the time source.

## Cons

- Every time-aware class takes a `Clock` in its constructor.
- Nothing yet fails the build on a new `Instant.now()`; reviewers must catch it.
- Hibernate's `createdAt`/`updatedAt` and `ActivatableEntity.disable()` still read the JVM clock.
- The test clock is shared by every test in the context; a test that forgets to reset it leaks
  time into the next.
