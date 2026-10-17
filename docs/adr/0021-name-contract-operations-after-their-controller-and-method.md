# ADR-0021 — Name contract operations after their controller and method

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

springdoc names each operation after its Java method and, when two controllers share a method name,
appends `_1`, `_2`… in discovery order. Adding the catalog's `list`, `disable` and `enable` renamed
the users endpoints from `list`/`disable`/`enable` to `list_2`/`disable_2`/`enable_2`, and handed the
plain `list` to the products list. foody-app types query parameters through
`operations["<operationId>"]`, so a new controller anywhere in the API silently retyped an
unrelated screen.

## Decision

- Every `operationId` is `<controller><Method>`: the controller's simple name without `Controller`,
  uncapitalized, followed by the method name, capitalized — `adminUserList`, `categoryDisable`,
  `menuQuote`.
- The rule is applied on the test classpath only, by `OperationIdCustomizer`, an `OperationCustomizer`
  registered in `OpenApiConfiguration`: main code never references springdoc (ADR-0008).
- `OpenApiContractTest` asserts the scheme on representative operations and that every published
  `operationId` is unique and has no numeric suffix — a duplicate fails the build instead of being
  renamed.
- A controller method whose name repeats its controller's (`MeController.me`) is named `get`, so the
  id reads `meGet`, not `meMe`.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| springdoc's default | Ids depend on which other controllers exist and on discovery order |
| `@Operation(operationId = …)` on every method | Swagger annotations in main code; the forgotten one falls back to the default |
| Globally unique method names (`listUsers`, `listProducts`) | Depends on discipline; a collision still gets a silent `_1` |
| Front types from `paths["/admin/users"]["get"]` only | Fixes this repo's consumer, not the contract; any other client sees the churn |

## Pros

- An operation keeps its id when other controllers are added or renamed.
- Ids say which controller answers, which helps in logs, Swagger UI and generated clients.
- A collision is a failing test, not a silent rename.

## Cons

- Renaming a controller or a controller method renames its operation — a contract change the front
  sees on its next `npm run api:types`.
- Two controllers with the same simple name in different packages would collide; the test catches
  it, and the fix is a rename.
- The rule lives in a test-scope customizer tied to springdoc's API.
