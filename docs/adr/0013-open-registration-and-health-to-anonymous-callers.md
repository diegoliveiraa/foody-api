# ADR-0013 — Open registration and health to anonymous callers

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

ADR-0002 made only `/auth/login`, `/auth/refresh` and `/auth/logout` public. Customers must be able
to create their own account, and the deploy healthcheck probes `/actuator/health` without any
credential. The refresh cookie also needed a name, and the stateless chain a stance on CSRF.

## Decision

- The public routes, listed only in `SecurityConfiguration`, are `POST /auth/register`,
  `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout` and `GET /actuator/health/**`.
  Error dispatches are permitted so error bodies render. Everything else needs a valid access token.
- `POST /auth/register {name, email, password}` creates a `CUSTOMER` and logs them in:
  `201 { accessToken, expiresIn }` plus the refresh cookie. Staff and admins are never
  self-registered.
- The refresh cookie is `foody_refresh`: `HttpOnly; Secure; SameSite=Strict; Path=/auth`, with
  `Max-Age` equal to the token's remaining life.
- CSRF protection is off. The only cookie the API reads is the refresh cookie, which a
  `SameSite=Strict` browser never sends on a cross-site request; every other endpoint
  authenticates with an `Authorization` header, which a browser never attaches on its own. CORS
  allows credentials only for `foody.web.allowed-origins`.
- Health answers status only, never details.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Accounts created only by an admin or by invitation | Customers order on their own; staff are promoted from customers by an admin |
| Health behind a token | Probes cannot hold a token that expires every 15 minutes |
| Health on a separate management port | A second port and its configuration for one endpoint |
| CSRF tokens (double-submit cookie) | Protects nothing that `SameSite=Strict` and bearer headers do not; one more header and round trip for the front |

## Pros

- Customers sign up without staff involvement and start logged in.
- Container healthchecks work with no credential.
- One place lists every public route — reviewing it reviews the whole anonymous surface.

## Cons

- Registration is open to bots until rate limiting lands.
- `409 EMAIL_ALREADY_REGISTERED` tells a caller that an e-mail has an account; login does not.
- Dropping CSRF relies on `SameSite=Strict` and on front and API being on the same site (ADR-0002);
  a browser ignoring `SameSite` could be made to call refresh or logout, without reading the answer.
- Anyone can learn whether the API is up.
