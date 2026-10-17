# ADR-0023 — Query another feature through an interface it implements

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

Deleting a product must be refused while an order item references it (ADR-0006). Products live in
`catalog`, order items in `order`, and `order` already depends on `catalog`: it prices the cart
through `CartPricingService` (ADR-0020). A call from `catalog` into `order` closes a cycle that
`ArchitectureTest` rejects (`featurePackagesAreFreeOfCycles`). ADR-0019 settles how a feature reacts
to another one's change; here `catalog` needs an answer before it acts, and more such questions will
come.

## Decision

- **The feature that asks declares the question** as an interface in its own `service` package,
  named after what it asks, taking and returning ids and values, never another feature's entities:
  `catalog.service.ProductReferenceLookup#isReferenced(UUID productId)`.
- **The feature that owns the data answers it** with an implementation in its own `service` package,
  through its own repository: `order.service.OrderedProductLookup` →
  `OrderItemRepository.existsByProductId`.
- The asking service injects `List<ProductReferenceLookup>` and asks every implementation. A new
  feature that references products adds an implementation; `catalog` does not change.
- The call is synchronous and read-only, inside the asking service's transaction. The foreign key
  stays the safety net (409 `DATA_CONFLICT`).
- Dependencies point one way: the answering feature depends on the asking feature's interface, never
  the reverse. `featurePackagesAreFreeOfCycles` checks it.
- **Events are for reactions, interfaces are for questions.** "This happened, react" is a domain
  event (ADR-0019); "tell me before I act" is an interface. A repository never reads another
  feature's table.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| `catalog` calls `order`'s service or repository | Closes the `catalog` ↔ `order` cycle; `ArchitectureTest` fails |
| A native query on `order_items` in `ProductRepository` | No Java cycle, but `catalog` reads a table it does not own: ArchUnit cannot see the dependency, and a change to `order`'s schema breaks `catalog` silently |
| An event that a listener vetoes by throwing | A reaction mechanism bent into a question: the answer is an exception and the flow is implicit |
| The interface in `shared` | `shared` grows one interface per question and learns every feature's vocabulary; the question belongs to the feature that asks it |
| The foreign key alone | Answers 409 `DATA_CONFLICT`, without the message that points to disabling (ADR-0006) |

## Pros

- Feature packages stay acyclic, and the feature that owns the data keeps its schema to itself.
- The question is visible where it is asked: `ProductService.delete` shows the check.
- A new feature that references products adds one implementation; the asking feature is untouched.
- Integration tests cover it through the real endpoint, with no mocks.

## Cons

- Who answers is found by searching for implementations, not by reading the asking code.
- With no implementation registered, the question silently answers "no"; only the foreign key
  catches the missing one.
- A slow implementation slows the asking request; implementations must stay one indexed query.
- Each question is one more interface; a feature that is asked many things accumulates them.
- Check-then-act is not atomic: an order placed between the check and the delete is caught only by
  the foreign key.
