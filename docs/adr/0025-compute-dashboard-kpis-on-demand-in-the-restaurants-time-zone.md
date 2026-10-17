# ADR-0025 — Compute dashboard KPIs on demand in the restaurant's time zone

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

Staff keep one screen of KPIs open — orders, revenue, average ticket, cancellations, orders per hour
or day, time per stage, best sellers, live counts and pending cancellation requests — for today, the
last 7 or the last 30 days, and it polls every 60 s. There is one restaurant in one time zone
(ADR-0022); instants are stored in UTC (ADR-0005), but "today" and a day on the chart are the
restaurant's day. Every number comes from orders, items and status history, which `order` owns, and a
repository never reads another feature's table (ADR-0023).

## Decision

- `GET /dashboard?period=TODAY|LAST_7_DAYS|LAST_30_DAYS` computes every number on each request with
  SQL aggregates — a fixed set of statements whatever the volume. Nothing is cached or stored.
- The window is computed in Java from the injected `Clock` in the restaurant zone: from the start of
  the first local day to now. The previous window is the same span shifted back by the period length
  on both bounds, so `TODAY` compares with yesterday up to the same clock time.
- The series groups with `date_trunc(unit, placed_at, zone)` — hours for `TODAY`, days otherwise.
  Java fills empty buckets with 0 and sends each bucket as the instant of its local start, with the
  zone id in the response.
- Order and cancellation counts take every order placed in the window; revenue, average ticket and
  top products take only `DELIVERED` ones. Stage times come from status-history lines that occurred
  in the window, measured from the line that entered the previous status; cancellations are not
  stages. Live counts and pending requests ignore the period.
- The SQL stays with its owner: native queries in `order.repository.OrderMetricsRepository`, behind
  the read-only `order.service.OrderMetricsService`, which takes instants and returns `order` value
  records. `dashboard` calls that service and owns windows, percentages and the response; `order`
  never imports `dashboard`. ADR-0023's interface is for when the owner already depends on the asker.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Rollup or materialized tables, scheduled snapshots | Jobs, refreshes and invalidation for windows of a few thousand rows; numbers lag the 60 s poll |
| An in-memory cache | Stale after every order command, per instance, and invalidating it needs events from all of them |
| Loading the entities and aggregating in Java | Every row of a 30-day window crosses the wire on every poll; memory and time grow with volume |
| A dashboard repository over `orders` | Another feature reading `order`'s tables, the coupling ADR-0023 rejects; a schema change breaks it silently |
| Computing in the front | Raw orders in the browser, zone and money rules repeated there; the front never computes money (ADR-0005) |

## Pros

- Numbers are current at the request, with no job, table or invalidation to maintain.
- The statement count is fixed and tested; queries are range scans on indexed `placed_at` and
  `occurred_at` (V6 adds `(to_status, occurred_at)` on the history).
- Windows and buckets follow the restaurant's day; the front only formats.
- `order` keeps its schema private; `dashboard` depends on one read service, without a cycle.

## Cons

- Every poll scans its window: cost grows with order volume and with the number of open dashboards,
  and a busy month is read in full every 60 s per screen. Past that point rollups need a new ADR.
- Numbers move after the fact: an order delivered or cancelled later changes the revenue, ticket,
  best sellers and cancellation rate of the period it was placed in.
- `TODAY` compares partial days, so a quiet morning swings the change; it is null when the previous
  window had no orders.
- One zone for the whole restaurant; another zone or a second restaurant reopens this.
- `order` carries a read model (projections, queries, service) shaped by what the dashboard shows,
  and `date_trunc` with a zone ties those queries to PostgreSQL.
