# ADR-0012 — Grant screens and actions to roles in code

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

ADR-0001 left open how grants are stored. Foody has three kinds of user — customers who sign up
themselves, staff who run the store, and admins who also manage users — and the same grants must
draw `/me` and authorize every request. Hand-written `@PreAuthorize` strings compile with typos,
and an endpoint that forgets its check is open to any authenticated caller.

## Decision

- A user has exactly one `Role`: `CUSTOMER`, `STAFF` or `ADMIN`, stored on `users.role`. It never
  leaves the API — `/me` sends navigation, not the role (ADR-0001).
- `Module`, `Screen` and `Action` are enums in `com.foody.api.shared.security`. A screen carries
  its key, pt-BR label, module and actions; declaration order is display order.
- `RoleGrants` holds the single `EnumMap<Role, Set<Screen>>`. A role that reaches a screen holds
  every action on it. `CUSTOMER` → `store.menu`, `store.my-orders`; `STAFF` →
  `dashboard.overview`, `orders.order-list`, `catalog.products`, `catalog.categories`;
  `ADMIN` → everything `STAFF` has plus `admin.users`.
- One function builds the authorities: screen presence is the enum name (`ORDERS_ORDER_LIST`), an
  action is `SCREEN:ACTION` (`ORDERS_ORDER_LIST:UPDATE_STATUS`). They are derived from the
  user's current role on every request, never stored in the token (ADR-0002).
- Endpoints declare access with `@RequiresAuthentication`, `@RequiresScreen(screen)` or
  `@RequiresGrant(screen, action)` — meta-annotations over `@PreAuthorize` templates
  (`AnnotationTemplateExpressionDefaults`). Enum attributes render as `name()`, which is why
  authorities use enum names.
- `ArchitectureTest` fails the build when a public method of a `@RestController` outside
  `..auth.controller..` is not meta-annotated with `@PreAuthorize`.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Roles, permissions and assignments in tables, edited in an admin screen | No requirement to change grants at runtime; a schema, a screen and a cache for three fixed roles |
| `hasRole('STAFF')` on endpoints | Ties endpoints to roles: granting a screen to another role means editing every endpoint, and `/me` cannot be derived from it |
| Raw `@PreAuthorize` strings | Typos compile; nothing ties the expression to the screens `/me` shows |
| Grants inside the JWT | Rejected in ADR-0002: a change would wait for the token to expire |

## Pros

- One map draws the menu and authorizes the request — they cannot drift apart.
- Screens and actions are compile-checked; a missing annotation breaks the build, not production.
- A role change applies on the user's next request.

## Cons

- Changing what a role reaches is a code change and a deploy.
- A role cannot hold only some of a screen's actions; when that is needed the map must carry
  actions per screen.
- Renaming an enum constant changes an authority; renaming a screen key breaks the front (ADR-0001).
- Every request reads the user to learn the role; method security proxies the controllers.
