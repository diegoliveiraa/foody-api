# ADR-0010 — Log structured JSON in deploy, every line tied to a request id

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

In production nobody reads logs line by line: they are searched, filtered and correlated. Plain
text lines need fragile parsing, and nothing ties together the lines of one request — or links a
user's "it failed" to the lines that explain it.

## Decision

- In deploy, logs are ECS JSON (`LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` in `compose.deploy.yaml`),
  with service name and version. Locally they stay human-readable.
- `RequestIdFilter`, first in the chain, takes the client's `X-Request-Id` when it is safe to log
  (`[A-Za-z0-9._-]{1,64}`) or generates a UUID, puts it in the MDC and returns it in the response
  header. Every log line inside the request carries `requestId` — a JSON field in deploy, a
  `[requestId]` prefix locally.
- Every error body carries `requestId` next to `code` (an addition to ADR-0005's contract), so a
  reported error leads straight to its logs.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Micrometer Tracing with OpenTelemetry | Full distributed tracing, but megabytes in the image for a single service; revisit when a second service or a tracing backend exists |
| Logstash or GELF format | Equally structured; ECS is a published schema that most log stores map natively |
| Trusting any client request id | Newlines and oversized values would forge or bloat log lines |
| JSON logs locally too | Unreadable in a terminal, where people read them |

## Pros

- Logs are queryable by level, logger, service and request without parsing.
- One id joins the client's error, the response header and every server line.
- No new dependency and nothing added to the image.

## Cons

- Work handed to other threads (`@Async`, schedulers) loses the MDC unless it is copied on purpose.
- The id stops at this service: there is no cross-service trace yet.
- Logger levels set through environment variables only work on package names, which are lowercase
  (`LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB`), not on class names.
