# ADR-0014 — Render security failures and rate limits as Problem Details

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

ADR-0005 listed four domain exception bases (400, 404, 409, 422). Authentication brings 401 and
403, and rate limiting brings 429 — whose answers need headers (`WWW-Authenticate`,
`Retry-After`). Spring Security answers its own failures from filters, with an empty body and no
`code` or `requestId`, and a method-security denial thrown inside a controller fell into the
catch-all 500.

## Decision

- Domain exceptions extend one of `InvalidRequestException` (400), `UnauthorizedException` (401),
  `NotFoundException` (404), `ConflictException` (409), `BusinessRuleException` (422) or
  `TooManyRequestsException` (429).
- `DomainException.headers()` (empty by default) lets a base add response headers, and the
  handler answers a `ResponseEntity` carrying them: `UnauthorizedException` sends
  `WWW-Authenticate: Bearer`; `TooManyRequestsException` sends `Retry-After` in whole seconds,
  rounded up, at least 1.
- Spring Security's `AuthenticationEntryPoint` and `AccessDeniedHandler` delegate to the
  `HandlerExceptionResolver`, so `GlobalExceptionHandler` renders every failure:
  `AuthenticationException` → 401 `UNAUTHENTICATED` with `WWW-Authenticate: Bearer`,
  `AccessDeniedException` → 403 `FORBIDDEN`. Their messages never reach the client.
- When a status has no code of its own, 401 maps to `UNAUTHENTICATED`, 403 to `FORBIDDEN` and 429
  to `RATE_LIMITED`.
- Login answers 401 `INVALID_CREDENTIALS` for a wrong password, an unknown e-mail and a disabled
  account alike; refresh answers 401 `REFRESH_TOKEN_INVALID`.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Spring Security's default bearer entry point | Empty body, no `code` or `requestId`; the front would need a second error extractor |
| A filter that writes the JSON itself | A second renderer that drifts from `GlobalExceptionHandler` |
| `ResponseStatusException` with headers | Loose messages and no catalog code (R7) |
| An `@ExceptionHandler` per exception | R13: one handler per base, not per exception |

## Pros

- Every status, from filters or controllers, has the same body with `code` and `requestId`.
- A 429 tells the client when to retry; distinct codes let the front choose between refreshing
  and sending the user to login.

## Cons

- Filter-level failures depend on the MVC resolver handling a request with no handler, so the
  advice must stay global.
- The only way to add a header to an error is `headers()` on its base.
- One login code means a disabled user cannot tell why login fails; support must check.
- Login and refresh failures also carry `WWW-Authenticate: Bearer`, though no bearer was sent.
