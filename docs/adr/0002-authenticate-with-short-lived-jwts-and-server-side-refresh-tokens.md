# ADR-0002 — Authenticate with short-lived JWTs and server-side refresh tokens

- **Status:** Accepted — public endpoint list superseded by ADR-0013
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

foody-app needs to know who is calling (`GET /me`, ADR-0001) and every endpoint must authorize
the caller. A server session ties every API instance to a shared session store; a long-lived
token cannot be taken back when a device is lost, a user is blocked or a token leaks.

## Decision

- `POST /auth/login` checks e-mail and password (Spring Security's `PasswordEncoder`) and returns
  `{ accessToken, expiresIn }` in the body plus a refresh token in a cookie.
- **Access token:** a JWT signed with RS256, valid for 15 minutes, carrying only `sub` (user id),
  `iss`, `iat`, `exp` and `jti`. Issued by Spring Security's `JwtEncoder` and validated by its
  OAuth2 resource server — no third-party JWT library. Grants are not in the token: each request
  is authorized against the current grants (ADR-0001).
- **Refresh token:** an opaque 256-bit random value, sent only as an `HttpOnly`, `Secure`,
  `SameSite=Strict` cookie scoped to `/auth`. The database keeps only its SHA-256 hash, with the
  user, a family id, the expiry and the revocation time.
- **Rotation:** `POST /auth/refresh` revokes the token it receives and issues a new pair.
  Presenting a token that was already rotated revokes its whole family: the user logs in again.
- **Lifetimes:** a refresh token lasts 7 days, renewed on each rotation; a family ends 30 days
  after login.
- `POST /auth/logout` revokes the family and clears the cookie.
- Spring Security runs stateless. Only the three `/auth/*` endpoints are public. Signing keys
  come from configuration outside the repository.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Server session (`JSESSIONID`, Spring Session) | Every instance depends on a shared session store |
| Long-lived JWT without refresh | A stolen token works until it expires; nothing can revoke it |
| Refresh token as an unstored JWT | Rotation and reuse detection need a store anyway |
| Refresh token in the response body | Any script on the page can read it |
| HS256 with a shared secret | Whoever can verify can also sign; RS256 verifiers need only the public key |
| Grants inside the JWT | A grant change would wait for the token to expire |

## Pros

- Logout, blocking a user or a detected reuse stop new access tokens at once; the grant check on
  each request stops a blocked user even before the access token expires.
- A stolen refresh token is caught the first time both it and the legitimate one are used.
- Authentication needs no database read per request — only authorization does.

## Cons

- Refresh tokens need a table and periodic cleanup of expired rows.
- Two tabs refreshing at once would trip reuse detection; the front must serialize refreshes
  (foody-app ADR-0008).
- A key pair to generate, keep out of git and rotate.
- The cookie flow needs CORS with credentials for the front's origin, and front and API on the
  same site.
- The database engine and migration tool are still open, and the token table depends on them.
