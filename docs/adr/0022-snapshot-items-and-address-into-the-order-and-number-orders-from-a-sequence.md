# ADR-0022 — Snapshot items and address into the order and number orders from a sequence

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

An order records what a customer bought, at what price and where it went. Products are renamed,
repriced, disabled and, when unused, deleted (ADR-0006); the customer's address exists nowhere else
in the API. Screens and kitchen tickets show a short human number ("#1042"), not the UUID. Staff
filter orders by day, but instants are stored in UTC (ADR-0005) while the restaurant works on São
Paulo days. Customers and staff look at the same order and must not see the same data.

## Decision

- **Items are snapshots.** Each `order_items` row copies `product_name` and `unit_price` from the
  `CartPricingService` quote at placement (ADR-0020), with `quantity` and `subtotal`; the order's
  `total` is the quote's total. `product_id` stays as a reference without cascade. Nothing on an
  order is read back from the catalog or recomputed.
- **The delivery address is a snapshot** in the order's `delivery_*` columns (`DeliveryAddress`
  embeddable), the CEP normalised to `00000-000` and the state a `BrazilianState`.
- **The human number comes from `order_number_seq`** (starting at 1001), read by
  `OrderRepository.nextNumber()` before the insert and unique by constraint. Gaps are allowed; the
  UUID v7 stays the id in URLs.
- **The restaurant's time zone is configuration**, `foody.restaurant.time-zone`, bound to
  `RestaurantProperties`. A date filter takes `LocalDate`s and becomes the half-open range from the
  start of `from` to the start of the day after `to` in that zone. The dashboard reuses it.
- **Customers and staff get separate DTOs and endpoints.** `/my-orders` answers `Customer*` records,
  whose history has no actor names; `/orders` answers `Staff*` records with the customer's contact
  and each actor's name. The endpoint picks the DTO, never the role.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Read name and price from `products` when showing an order | A price change rewrites past receipts and revenue |
| Store the cart as a JSON blob | Not queryable for top products or for the product-in-use check |
| Reference a customer address book | Editing an address would move past deliveries; there is no book |
| `max(number) + 1` | Concurrent placements race for the same number without a lock |
| Show the UUID or a hash | Unreadable over the phone or on a ticket |
| Filter by UTC days, or have the front send instants | A 22:30 order in São Paulo falls on the next UTC day; every client repeats the conversion |
| One DTO with staff fields nulled for customers | A forgotten null leaks staff data; the contract cannot tell the audiences apart |

## Pros

- Receipts, history and reports keep the name shown and the price paid, whatever the catalog does.
- Renaming, repricing or disabling a product needs no migration of orders.
- Numbers are short, unique and need no lock at placement.
- "Today" and date filters mean the restaurant's day, from one property.
- Staff-only data cannot reach a customer: the customer records have no field for it.

## Cons

- Names and addresses are duplicated per order and item; a typo fixed in the catalog stays in old
  orders.
- A product that appears in any order can no longer be deleted, only disabled.
- Sequence gaps surprise anyone expecting consecutive numbers, and numbers never reset per day.
- `nextval` is one more round trip per placement and lives outside JPA: a restore must bring the
  sequence along.
- One time zone for the whole API; a restaurant in another zone reopens this decision.
- Two DTO families describe the same order and must change together.
