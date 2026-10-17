# ADR-0024 — Drive the order lifecycle through one policy, per-side endpoints and a single cancellation request

- **Status:** Accepted
- **Date:** 2026-10-17
- **Decided by:** Diego

## Context — why this became a decision

Orders move `RECEIVED → PREPARING → OUT_FOR_DELIVERY → DELIVERED` or end `CANCELLED`, and two
audiences act on them: staff advance and cancel; customers cancel only before the kitchen starts and
must ask once it has. What is allowed depends on the status, on who acts and on whether a customer
request is waiting. Staff boards and customer screens poll, so two people routinely act on stale
copies of the same order, and the front must not keep its own copy of the rules to draw its buttons.

## Decision

- `OrderLifecyclePolicy` (`order.service`) is the only place that decides; its tables live in
  `OrderConstants` and it is unit-tested over every status × side × request state:

  | Status | Staff advance to | Staff cancel reasons | Customer cancels | Customer requests |
  |---|---|---|---|---|
  | `RECEIVED` | `PREPARING` | `ITEM_UNAVAILABLE`, `STORE_CLOSED`, `CUSTOMER_REQUEST`, `OTHER` | yes, customer reasons | no |
  | `PREPARING` | `OUT_FOR_DELIVERY` | the same | no | yes, if never requested |
  | `OUT_FOR_DELIVERY` | `DELIVERED` | `DELIVERY_FAILED` | no | no |
  | `DELIVERED`, `CANCELLED` | — | — | no | no |

  A `PENDING` request blocks staff advance and staff cancel until it is decided. `OTHER` needs a note.
- Per-side endpoints: staff `PATCH /orders/{id}/status`, `POST /orders/{id}/cancel` and
  `POST /orders/{id}/cancellation-request/approve|decline`; customers `POST /my-orders/{id}/cancel`
  and `POST /my-orders/{id}/cancellation-request`. The endpoint sets the `ActorSide`, never the role.
- The PATCH carries the target status: asking for a status the order already passed answers 422
  `INVALID_STATUS_TRANSITION` instead of moving it twice.
- Every command loads the order through `OrderRepository.lockById`
  (`OPTIMISTIC_FORCE_INCREMENT`), so creating or deciding a request bumps the order's version too and
  any two racing commands end in 409 `CONCURRENT_UPDATE`.
- One `cancellation_requests` row per order, ever (unique `order_id`); a declined request still
  counts. Approving cancels with `cancelledBy = CUSTOMER` and the request's reason; the history line
  records the approving staff member on the restaurant side.
- Responses carry the action fields — `allowedTransitions`, `canCancel`, `canRequestCancellation`,
  `cancellationReasons`, `hasPendingCancellationRequest`, `cancellationRequestStatus` — computed by the
  same policy. A list loads its request flags with one query per page.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| A state-machine library | A dependency and its persistence for five states; the side and reason rules would still be ours |
| One generic `PATCH {status}` for both sides | The role would pick the side, and reasons, requests and decisions do not fit a status field |
| Pessimistic locks (`SELECT … FOR UPDATE`) | Holds row locks for the whole request; the version column already detects the race and answers 409 |
| Several requests per order | Turns one decision into a negotiation loop; the screens show one request and one answer |
| Rules in the front, the API only validating | Two copies drift, and the front shows buttons the API refuses |

## Pros

- One tested class answers "can this side do this now?"; a new rule is one table change.
- A stale screen or a double click cannot move an order twice: a passed target is a 422, a lost race
  a 409.
- The front draws buttons and reason pickers from the response, never from its own rules.
- Every status change keeps who acted, from which side, with reason and note.

## Cons

- Polling clients will meet 409 and 422 on stale screens and must reload; the front needs one
  handler for both.
- Every command writes one more `UPDATE orders` for the version, even when only the request changes.
- A list page costs one more query for the request flags.
- The tables and their tests must change with any new status or reason.
- An approved request reads `cancelledBy = CUSTOMER` while its history line is on the restaurant
  side; reports must know which field answers which question.
