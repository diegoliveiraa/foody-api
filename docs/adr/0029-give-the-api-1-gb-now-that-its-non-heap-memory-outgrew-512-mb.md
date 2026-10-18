# ADR-0029 — Give the API 1 GB now that its non-heap memory outgrew 512 MB

- **Status:** Accepted
- **Date:** 2026-10-18
- **Decided by:** Diego

## Context — why this became a decision

ADR-0004 set the API container to 512 MB with the heap at 50% of it. It was measured on 2026-10-16,
when the API was only its base: a 123 MB image, 286 MB resident under 200 parallel requests, 462 MB
with the whole heap pre-touched — about 50 MB of headroom — and it asked for a new measurement as the
API grew. Since then it gained Spring Security with JWT, every JPA entity, Flyway V1–V8, rate
limiting, the order lifecycle, the dashboard and accent-insensitive search: the production image is
now 229 MB.

The measurement was repeated on 2026-10-18 with `scripts/load-test/run.sh`: the production image,
read-only and with its own JVM flags, on the demo database seeded at `MEDIUM` (1,000 orders, about
500 customers), driven by k6 2.3.0 — 160 customers (menu, quote, place an order in a quarter of the
iterations, their orders) and 40 staff (board, advance orders, dashboard, history, products) for six
minutes, then 1.5× that for a minute; rate limits lifted for the test; memory sampled every 4 s, the
heap read from the GC log. Below 2 GB the JVM's ergonomics pick the Serial collector.

| Limit | Heap | Mode | Peak resident | Result |
|---|---|---|---|---|
| 512 MB | up to 256 MB | normal | 448 MB (88%) | 0% errors; live heap 58–79 MB after each collection |
| 512 MB | 256 MB pre-touched | worst case | 506 MB | killed by the kernel (OOM) about 30 s into the ramp; 28% of requests failed |
| 768 MB | 384 MB pre-touched | worst case | 678 MB (88%) | no kill, 0% errors |
| 1024 MB | 512 MB pre-touched | worst case | 843 MB (82%) | no kill, 238,321 requests, p95 4.6 ms, p99 9.2 ms |

What grew is the memory outside the heap: about 290–335 MB under load (classes, compiled code, 200
Tomcat threads, the collector, native buffers), against 180–230 MB on 2026-10-16. The heap itself
is far from full: the live set stayed between 58 and 93 MB. PostgreSQL peaked at 145 MB (512 MB
limit) and the web container at 13 MB (64 MB limit).

## Decision

- `API_MEMORY_LIMIT` defaults to **1024M** in `compose.deploy.yaml` and `.env.example`. The heap
  stays at 50% of the limit (512 MB); nothing else in the JVM flags changes.
- PostgreSQL keeps 512 MB and the web container 64 MB: both stay far below their limits.
- The load test lives in the repository (`scripts/load-test/`, k6 pinned) and is re-run when the API
  grows. When the pre-touched peak leaves less than about 15% of the limit free, the limit goes up —
  never the heap percentage (ADR-0004).

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Keep 512 MB and cap Tomcat's threads | 200 threads cost about 47 MB (ADR-0004); even with fewer, the worst case stays within tens of MB of the kill, and the cap limits concurrency |
| 768 MB | Measured 90 MB (12%) of headroom in the worst case: the next features take it |
| A larger heap percentage | Leaves less room for the non-heap that just grew; past it the kernel kills the container with no `OutOfMemoryError` to diagnose (ADR-0004) |
| Capping metaspace and the code cache | Turns growth into runtime errors (`OutOfMemoryError: Metaspace`) instead of a sized container |
| G1 or ZGC | More native memory for the collector; Serial's pauses were 1–2 ms at this load |
| GraalVM native image | The reasons in ADR-0004 still hold |

## Pros

- 181 MB (18%) free at the worst case, with the heap fully touched and 300 simultaneous users.
- Latency and throughput unchanged by the larger heap (p95 under 5 ms, no errors).
- The measurement is reproducible from the repository with one script.

## Cons

- Each API instance now reserves 1 GB: API, PostgreSQL and web together need about 1.6 GB on the host.
- The heap (512 MB) is several times the live set (60–90 MB): memory held, not used, to keep the
  worst case inside the limit.
- One machine (12 cores, Docker Desktop on Windows) and a synthetic scenario with rate limits lifted:
  real traffic is lower, real payloads may differ.
- At 1792 MB or more, or with fewer than 2 CPUs, the JVM picks another collector; changing the limit
  that far means measuring again.
