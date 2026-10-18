# ADR-0027 — Search names ignoring accents with unaccent and trigram indexes

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

Every typed search was `lower(column) LIKE '%term%'`: users by name or e-mail, products by name,
staff orders by customer name. It was accent-sensitive — "joao" missed "João", "acai" missed
"Açaí" — while the customer menu already ignores accents on the client. And a B-tree cannot serve a
pattern with a leading `%`, so every search read the whole table.

## Decision

- V8 creates the `pg_trgm` and `unaccent` extensions and two `IMMUTABLE PARALLEL SAFE STRICT` SQL
  functions: `public.f_unaccent(text)`, returning
  `public.unaccent('public.unaccent'::regdictionary, $1)`, and `public.f_contains_pattern(text)`,
  which normalizes the term with `f_unaccent(lower($1))`, **then** escapes `\`, `%` and `_`, and
  wraps it in `%…%`.
- Every typed search goes through `TextSearchUtils.matches`:
  `f_unaccent(lower(column)) LIKE f_contains_pattern(:term) ESCAPE '\'`, with the stripped raw term
  bound as a parameter. Java does no escaping.
- Each searched column has a GIN index on exactly that expression with `gin_trgm_ops`:
  `users.name`, `users.email`, `products.name` (the order search reads `users.name`).
- Facts relied on (PostgreSQL 18 docs): GIN trigram indexes serve `LIKE`/`ILIKE` and "the search
  string need not be left-anchored"; both modules are "trusted" — installable by a non-superuser with
  `CREATE` on the database, which a Testcontainers probe confirmed. `unaccent()` is declared `STABLE`
  and an index expression only accepts `IMMUTABLE` functions, hence the wrapper.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| A nondeterministic ICU collation (`und-u-ks-level1`) | The docs warn of a performance penalty, no B-tree deduplication and pattern matching operations it cannot do |
| Full-text search (`tsvector`) | Matches whole lexemes, not the partial word typed so far |
| `text_pattern_ops` B-tree | Serves prefix searches only, not `%term%` |
| Normalizing in Java | Two normalizations that can disagree, and no index on either |

## Pros

- "joao", "JOAO" and "João" find the same rows, on every search, through one expression.
- The planner can answer `%term%` from the index instead of reading the table (checked with
  `EXPLAIN` in `TrigramSearchIndexTest`).
- The term stays a bound parameter, and escaping happens after normalization: no unaccent rule can
  produce a live wildcard (`％`, `﹪`, `＿`, `＼`, `﹨` and `∖` fold into `%`, `_` or `\` and are
  escaped like them).

## Cons

- Two extensions to keep installed and upgraded with the database.
- The wrappers' `IMMUTABLE` is a promise PostgreSQL cannot check: if the unaccent rules file
  changes, the three indexes hold stale values until `REINDEX`.
- GIN indexes cost write time and disk on every insert and update of these columns.
- A term of one or two characters has no trigrams: the search degrades to a full index scan.
- After a `pg_upgrade`, the PostgreSQL 18 release notes recommend reindexing `pg_trgm` indexes on
  clusters whose default collation provider is not libc; ours is libc today.
- The `\` escape character is fixed in two places that must agree: `f_contains_pattern` and
  `PersistenceConstants.LIKE_ESCAPE`.
- `TextSearchUtils` binds the term through Hibernate's `HibernateCriteriaBuilder.value`; JPA alone
  would inline it as a literal in the SQL text.
