# ADR-0018 — Every contract field is required unless marked @Nullable

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

springdoc marks a property required only when Bean Validation says so (`@NotNull`, `@NotBlank`).
Response records carry no validation, so every response field in `docs/openapi.json` was optional,
and foody-app's generated types (`accessToken?: string`, `user?: MeUser`) forced null handling on
values the API always sends. Java has no non-null type, so the contract needs an explicit rule.

## Decision

- Every component of a DTO record is **required** in the published contract — requests and
  responses alike.
- A component that may be absent is annotated with JSpecify's `@Nullable`
  (`org.jspecify.annotations.Nullable`, on the type: `@Nullable String note`). It is published
  optional and nullable: `type: [..., "null"]`, or `oneOf: [$ref, {type: "null"}]` for a record.
- The rule is applied on the test classpath only, by `RequiredByDefaultModelConverter`, a swagger
  `ModelConverter` registered in `OpenApiConfiguration`: main code never references springdoc
  (ADR-0008). It covers records; any other class keeps springdoc's default.
- `OpenApiContractTest` asserts the required lists of the published schemas;
  `RequiredByDefaultContractTest` covers `@Nullable` through a test-only probe controller that never
  reaches the published file.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| `@Schema(requiredMode = REQUIRED)` on each component | Swagger annotations in main code, one per field — the forgotten one is the bug |
| `@NotNull` on every response component | Validation annotations on bodies that are never validated |
| Rewriting the finished document by schema name | Loses the Java type, so it cannot see which component is `@Nullable` |
| Marking everything required when the front generates its types | The contract would lie, and nullable fields would be indistinguishable |

## Pros

- Generated types match what the API sends: no `?` on fields that are always present.
- Nullability is visible in the record and in the contract, from one annotation.
- JSpecify is the annotation Spring itself uses; no new tool.

## Cons

- Forgetting `@Nullable` on a field that can be null publishes a lie the front will trust; nothing
  checks at compile time that the other components are never null.
- The rule lives in a test-scope converter tied to swagger-core's API; an upgrade can break it.
- Only records follow the rule.
- Required in the contract is not validation: a request record still needs Bean Validation to
  answer 400.
