# ADR-0026 — Sort user-facing names with a Portuguese ICU collation on the column

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

Lists sorted by name came out in byte order: "Anéis de cebola" before "Açaí na tigela", and every
accented initial ("Álvaro", "Érica", "Ícaro") after "Z". The `postgres:18-alpine` cluster is created
with the libc provider and `en_US.utf8`, and musl implements no collation, so `ORDER BY name`
compares code points. The admin user list, the product list, the category list, the menu and the
dashboard's top products are read in Portuguese, and sort and pagination happen in the database
(ADR-0005). The image ships ICU: `pt-BR-x-icu` exists in every database it creates.

## Decision

- A column holding a name users see sorted is declared `COLLATE "pt-BR-x-icu"` in its migration:
  `users.name`, `categories.name`, `products.name` and `order_items.product_name` (V7). A new one
  does the same in the migration that creates it.
- Queries keep a plain `ORDER BY` / `Sort.by(...)`; the column's collation applies to every query,
  index and sort on it. No `COLLATE` in a query, no re-sorting in Java.
- Identifiers that only look like text — e-mails, statuses, codes — keep the database default, and
  so do their unique indexes and lookups.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| `ORDER BY name COLLATE "pt-BR-x-icu"` in each query | Every query and every `Sort` must remember it, Spring Data's `Sort` cannot say it, and an index on the column would not serve that order |
| Sort in Java with a `Collator` | Only orders what is already in memory; with server-side pagination, page 2 depends on the database's order |
| Change the database default collation | Fixed when the cluster or database is created: re-creating the local volume and the deployed database, beyond what a migration can do; it would also reorder e-mails and codes |
| `unaccent` and `ORDER BY unaccent(lower(name))` | An extension to install, an expression in every sort, and ASCII order of stripped text rather than Portuguese order |

## Pros

- Every list on these columns — paginated, unpaginated, the menu's and the dashboard's tie-breaks —
  follows Portuguese order with no change to queries, and a new query gets it for free.
- Sorting stays in the database, so page boundaries agree with the order users see.
- The collation is deterministic: equality is still byte-wise, so the case-insensitive uniqueness of
  `lower(categories.name)` keeps its meaning; V7 rebuilt that index under the new collation.

## Cons

- The order depends on the ICU version inside the image. An upgrade that changes it makes
  PostgreSQL warn about a collation version mismatch: indexes on these columns
  (`categories_name_lower_key`) then need a `REINDEX` and the collation an
  `ALTER COLLATION … REFRESH VERSION`.
- The collation is PostgreSQL's (ADR-0003) and needs an image built with ICU; without it V7 fails.
- The collation does nothing for `LIKE`/`ILIKE`: a search on the column alone stays accent-sensitive,
  so ignoring accents takes its own mechanism (ADR-0027).
- `lower()` on these columns now uses ICU case mapping while `lower(:parameter)` uses the database
  default; they agree on Portuguese letters, but they are two implementations.
- A name column added later sorts in byte order until its migration declares the collation —
  review has to catch it (R18).
- Changing a column's collation locks the table and rebuilds its indexes: trivial today, a
  maintenance window on a large table.
