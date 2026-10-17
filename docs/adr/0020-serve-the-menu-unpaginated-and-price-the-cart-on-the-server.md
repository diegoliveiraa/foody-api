# ADR-0020 — Serve the menu unpaginated and price the cart on the server

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

ADR-0005 paginates every list on the server so no response grows with a table. Two catalog lists do
not fit that rule: the customer reads the menu as one screen, scrolling through its categories, and
the staff's category list feeds the ordering screen and the product form's category picker. Foody
serves one restaurant, whose categories number in the units and whose menu in the tens to low
hundreds of products. The cart also needs totals, and ADR-0005 keeps money arithmetic out of the
front, which parses JSON numbers as floats.

## Decision

- `GET /menu` returns, unpaginated, every active category with at least one active product, by
  `displayOrder` then name, each with its active products by name. It is one query — active products
  joined with their active categories — grouped in the service.
- `GET /catalog/categories` returns every category, unpaginated, by `displayOrder` then name, with its
  product count, in two queries whatever the size.
- **The bound is the business, not the code:** both lists are sized for one restaurant — up to about
  50 categories and 500 active products. The `LARGE` seed (8 categories, about 4,500 active products)
  is the stress case, not the target. Outgrowing the bound needs a new ADR.
- `GET /catalog/products` stays paginated (ADR-0005): staff filter and browse it, disabled rows
  included.
- **Only the API computes money.** `POST /menu/quote {items: [{productId, quantity}]}` reads every
  unit price from the database; subtotal and total are `BigDecimal` with scale 2, rounding
  `HALF_EVEN` where it applies. A product that does not exist, is disabled or sits in a disabled
  category comes back `available: false`, without price or subtotal, and stays out of the total.
  The request carries 1 to 50 distinct products, each with a quantity from 1 to 20.
- `CartPricingService` is the only code that prices a cart. Order creation calls it, so an order's
  total is the quote's total by construction.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Paginate the menu like every other list | Pages split categories, and one screen would need several calls |
| Paginate categories | A handful of rows that the picker and the ordering screen need all at once |
| One request per category for its products | Client-side N+1 for data the screen needs in full anyway |
| Totals computed in the front from menu prices | Float arithmetic (ADR-0005); a stale price would show one total and charge another |
| The front sends prices and the API checks them | Client prices are never trusted, so the API computes them anyway — twice |
| Reject the whole quote when an item is unavailable | The bag must show which items went away and still price the rest |

## Pros

- The menu renders from one request backed by one query.
- One pricing function: the quote and the order cannot disagree.
- A price change or a disabled product shows on the next quote; the front never holds a price it
  computes with.

## Cons

- Nothing in the code enforces the bound: a catalog far beyond it makes the menu and the category
  list heavy until a new decision paginates or caches them.
- No HTTP caching on the menu yet: every visit reads the whole active catalog.
- Every change to the bag needs a round trip to show its total; there are no offline totals.
- A quote is not a reservation: a price can change between the quote and the order, which re-prices
  and may show a different total at checkout.
- The menu and the category list are exceptions to R19's pagination rule; reviewers must keep them
  the only ones.
