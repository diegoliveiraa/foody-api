# ADR-0017 — Rate limit in memory with Bucket4j

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

Registration and login are open to anyone (ADR-0013): bots can create accounts and guess passwords,
and an authenticated client can hammer any endpoint. ADR-0014 already fixed how a limit answers
(429 `RATE_LIMITED` with `Retry-After`), but not where limits live, how callers are keyed or where
the counters are kept. The API runs as a single instance, with no Redis or gateway in front of it.

## Decision

- `RateLimitFilter` (`shared.ratelimit`) runs in the API security chain right after
  `BearerTokenAuthenticationFilter`, so it knows the authenticated user. `SecurityConfiguration`
  creates it; it is never a bean. `GET /actuator/health/**` is never limited.
- Policies are configuration (`foody.rate-limit.*`, capacity and period, greedy refill). Routes are
  declared by method and path pattern in `RateLimitedRoute`; a request uses the first matching
  route, otherwise the general bucket of its caller — the user id when authenticated, the IP when not.

  | Route | Key | Default |
  |---|---|---|
  | `POST /auth/login` | IP · e-mail + IP | 20/min · 5/min |
  | `POST /auth/register` | IP | 5/h |
  | `POST /auth/refresh` | IP | 30/min |
  | `POST /my-orders` · `POST /my-orders/{id}/cancellation-request` | user | 10/min · 5/min |
  | any other request, authenticated · anonymous | user · IP | 300/min · 60/min |

- The login limit per e-mail + IP needs the body, so `AuthService.login` checks it before comparing
  the password; the controller passes the IP in. The e-mail is normalized and hashed in the key.
- One Bucket4j bucket per policy and key, in a Caffeine cache bounded by `max-tracked-keys`
  (100 000) and expiring after the longest period without access. Buckets read the injected `Clock`
  (ADR-0016). An empty bucket throws `RateLimitExceededException`, rendered by the global handler.
- The IP is `getRemoteAddr()`; `X-Forwarded-For` is never trusted.
- Tests and the `seed` profile run with generous limits; the limits are tested in their own context.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Bucket4j with Redis or JCache | Infrastructure Foody does not run while there is one instance |
| A hand-written fixed-window counter | Bursts at window edges, unbounded memory, refill math to maintain |
| Limits at a reverse proxy or gateway | There is none, and per-user limits need the authenticated identity |
| The filter as a `@Component` | The servlet container and MVC test slices would run it before security, without the user |
| Keying anonymous callers by `X-Forwarded-For` | Any client could pick a fresh bucket by sending the header |

## Pros

- Password guessing is capped per account and per IP; sign-up spam at 5 accounts an hour per IP.
- No new infrastructure; a limit changes through configuration, a new route through one enum entry.
- Clients get the usual Problem Details and know when to retry; tests cross refills through the clock.

## Cons

- Counting is per instance and lost on restart: with N replicas a client gets N times the limit.
  Running replicas means moving the buckets to a shared store (Bucket4j with Redis); policies and
  filter stay.
- Behind a proxy every request carries the proxy's address and all anonymous callers share one
  bucket. Adding a proxy requires trusting only it (`server.forward-headers-strategy=native` with
  `server.tomcat.remoteip.internal-proxies` set to its address).
- Callers behind one NAT share the per-IP limits.
- Spraying keys can fill the cache and evict buckets early; the per-IP login limit bounds how many
  e-mail keys one address creates.
- Requests with an invalid token are answered 401 by the bearer filter before the limit is checked,
  and a limited request with a valid token has already read the user's role.
