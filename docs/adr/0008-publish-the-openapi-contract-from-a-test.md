# ADR-0008 — Publish the OpenAPI contract from a test, keeping springdoc out of production

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

foody-app needs the API's request and response types without retyping them, and a change to the
contract should show up in review. A documentation endpoint in production exposes the whole API
surface, and its UI adds megabytes of assets to an image sized on purpose (ADR-0004).

## Decision

- springdoc-openapi is a **test** dependency. `OpenApiContractTest` boots the API, reads
  `/v3/api-docs` and writes `docs/openapi.json` on every test run — keys ordered,
  pretty-printed, LF line endings, server `/` — so the file only changes when the contract does.
- `docs/openapi.json` is committed with the change that alters it: the contract's diff is part
  of the PR.
- Swagger UI exists only under `./mvnw spring-boot:test-run`, at `/swagger-ui.html`.
- foody-app generates its types from that file (foody-app ADR-0010).

## Why this one and not the others

| Alternative | Why not |
|---|---|
| springdoc in production, behind authentication | Exposes the API surface and ships the UI's assets in the image |
| springdoc's Maven plugin at build time | Starts the whole application during the build and still needs the dependency |
| A hand-written contract first (design-first) | A second source of truth next to the code, and it drifts |
| No published contract | The front retypes every request and response |

## Pros

- Every contract change is a visible diff in review.
- The production jar carries nothing from springdoc or Swagger (checked on the built jar).
- The front generates its types from a file, without a running API.

## Cons

- Forgetting to commit `docs/openapi.json` leaves the front behind; CI must fail when the file
  differs after the tests.
- Descriptions and examples in the contract need Swagger's annotations in main code — a small
  compile dependency, added with the first endpoint that needs them.
- The front's generation reads the file from a foody-api clone beside it.
