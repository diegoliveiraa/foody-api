# ADR-0006 — Disable with `disabledAt`; delete for real; soft delete only by exception

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

ADR-0005 put soft delete on every entity, but the need behind it was another one: products,
customers and suppliers are switched off and on again. That is a reversible business state that
admin screens list, filter and revert, and that history — an order's items — keeps pointing at.
Soft delete hides rows from every JPA query: a disabled product would vanish from the screen that
re-enables it and from the orders that sold it. And a customer's erasure request under LGPD needs
the data removed or anonymised, not hidden.

## Decision

- `BaseEntity` keeps `id`, `createdAt`, `updatedAt` and `version` — no soft delete.
- An entity that can be switched off extends `ActivatableEntity`: a visible `disabledAt`
  (`disabled_at timestamptz`, `null` while active), `disable()` (keeps the first timestamp),
  `enable()` and `isActive()`. Each query decides whether it filters on it.
- Deleting removes the row. The service refuses to delete a row that is still referenced, with a
  `ConflictException` pointing to disabling; the foreign key's violation (409 `DATA_CONFLICT`)
  is the safety net.
- Soft delete is opt-in per entity — `@SoftDelete(strategy = TIMESTAMP, columnName =
  PersistenceConstants.DELETED_AT_COLUMN)` with partial unique indexes — when a case needs deleted
  data kept but hidden.
- A state richer than on/off (a product paused, out of stock) is a domain enum on that entity.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Soft delete on every entity (ADR-0005) | Disabled rows vanish from admin screens and from history; erasure still needs a real delete |
| `disabledAt` and soft delete on every entity | Columns and rules on tables that are never disabled (orders, payments) |
| An `active` boolean | Same cost, and it forgets when the row was switched off |

## Pros

- Disabled is not gone: admin screens list and revert it, and orders keep their products.
- Filtering is explicit in each query, so it is visible in review and covered by tests.
- A real delete satisfies an erasure request.

## Cons

- Every query on an activatable entity must decide on the filter; forgetting it shows disabled
  items — in a customer-facing list, that is a bug.
- Each delete needs a reference check in its service.
- There is no built-in recycle bin for deleted rows.
