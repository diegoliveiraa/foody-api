# ADR-0001 — Navigation and permissions are served by GET /me

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

foody-app renders its sidebar and guards its screens from a single response (foody-app ADR-0007).
Each user reaches only part of the app, and the rules that decide what they reach must be the
same ones that authorize their requests — otherwise the menu offers screens the API refuses, or
hides screens the API would allow.

## Decision

`GET /me` returns the authenticated user and the navigation they are allowed, already filtered
and ordered:

```json
{
  "user": { "id": "7f3c2a10", "name": "Ana Souza", "email": "ana@foody.dev" },
  "modules": [
    {
      "key": "orders",
      "label": "Pedidos",
      "screens": [
        { "key": "orders.order-list", "label": "Todos os pedidos", "actions": ["CREATE", "CANCEL"] }
      ]
    }
  ]
}
```

- A module or screen the user cannot reach is **absent** — never sent with a flag.
- Array order is display order; `label` is display text.
- A screen's presence grants viewing it; `actions` lists what else the user may do there
  (`CREATE`, `UPDATE`, `DELETE` or a domain verb such as `CANCEL`).
- Keys are `<area>` and `<area>.<screen>` — foody-app's folder names (its ADR-0005) — and never
  change once published.
- The grants that build `/me` are the same that authorize every endpoint behind those screens.
- `/me` carries no credentials or internal fields: no tokens, password data, login attempts or IPs.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Return the user's roles and let the front map them to screens | Every client re-implements the role → screen mapping, and it drifts from the API's |
| A separate `/menu` endpoint | Two requests and two caches answering the same question |
| Paths and icon names in the response | Routes and icons are front code; the API would have to track the bundle |

## Pros

- One source of truth: the grants that authorize requests are the ones that draw the menu.
- Reordering, renaming or granting a screen needs no front deploy.
- The response never tells a client about modules or screens it cannot use.

## Cons

- The API owns display text and order, so menu copy changes are backend changes.
- Keys are a contract between two repos: renaming one breaks the sidebar until both change.
- How grants are stored (tables, roles, assignment UI) is still open — decided with the first
  implementation, in its own ADR.
- `/me` is computed per user on every call; it may need caching as grants grow.
