# ADR-0028 — One command runs a seeded demo stack, separate from the production image

- **Status:** Accepted
- **Date:** 2026-10-18
- **Decided by:** Diego

## Context — why this became a decision

Foody is evaluated by people who clone the repository on a machine with Git and Docker and nothing
else. Seeing the API with data took a JDK 21, a `.env` with five hand-picked values, bash and
OpenSSL for `scripts/generate-jwt-keys.sh`, then `./mvnw spring-boot:test-run` — four places to fail
before the first request. `compose.yaml` only held the development database (ADR-0004), the seed
lives on the test classpath (ADR-0007), and the production image must never carry it.

## Decision

- **`compose.yaml` is the demo stack** (project `foody-demo`), started by
  `docker compose up -d --build --wait`:
  - `keys`: a one-shot `alpine` container writes an RSA 2048 PKCS#8 pair, world-readable like
    `generate-jwt-keys.sh` writes it, into the `jwt-keys` volume when it is not there yet, and exits;
  - `postgres`: PostgreSQL 18 with its own volume, not published to the host;
  - `api`: built from its own `Dockerfile.demo` — main and test code compiled with the Maven cache
    mount, the test classpath without DevTools and Lombok, on the Temurin 21 JRE as a non-root user —
    running `TestFoodyApiApplication`, the entry point of `spring-boot:test-run`: seed profile,
    generous rate limits, Swagger UI. Only `API_PORT` (8080) is published, and the health check turns
    healthy once the seeders are done, so `--wait` returns with the data in place.
- **Every value has a demo default in `compose.yaml`**, and a `.env` or the environment overrides it:
  `admin@foody.local`, `equipe@foody.local` and `cliente@foody.local` with `Foody#2026`, `SEED_SCALE`
  `SMALL`, a database password and the CORS origins of `application.properties`. These are the only
  credentials committed to the repository (R17, R20), valid for this local stack only — never in
  `compose.deploy.yaml`, `compose.dev.yaml` or code.
- `docker compose down` stops the demo; `docker compose down -v` wipes its database and keys, and the
  next `up` seeds again.
- **Local development moves to `compose.dev.yaml`** — same project (`foody-local`), service and volume,
  so existing data survives — and `spring.docker.compose.file` points Spring Boot's Docker Compose
  support at it. This supersedes ADR-0004's "`compose.yaml` is local" bullet.
- **The demo has its own Dockerfile.** The production `Dockerfile` stays exactly as it was, and its
  `.dockerignore` keeps `src/test` out of the production build context; `Dockerfile.demo` has a
  `Dockerfile.demo.dockerignore` that lets `src/test` in. Each file reads on its own, and there is no
  stage order to protect: `docker build .` and `compose.deploy.yaml` can only build production.
- CI brings the demo stack up, logs in as the demo admin and expects `GET /me` and `GET /dashboard`
  to answer 200.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Bash and PowerShell wrapper scripts around `spring-boot:test-run` | Still need a JDK 21 on the evaluator's machine, and two scripts to keep in step |
| The seed inside the production image, behind a profile | Ships seed code and Datafaker to production; a wrong profile seeds it (ADR-0007) |
| A `.env` with no defaults | One more manual step, and the one most often done wrong |
| Random passwords printed in the logs | The evaluator digs through logs, and the README cannot name the accounts |
| A prebuilt demo image on a registry | No registry or publishing pipeline yet, and building from source also proves the code builds |

## Pros

- Git and Docker are enough — no JDK, no bash, no `.env` — with one command that is the same on
  Windows, macOS and Linux.
- The demo runs the same entry point, seeders and settings as `spring-boot:test-run`: it cannot drift
  from what developers see.
- The production image does not change; the keys are generated per machine and never committed.
- The demo's database is not published, so it never fights a local PostgreSQL over port 5432.
- CI proves the one command works on every change.

## Cons

- Demo credentials live in a versioned file. Acceptable only because the stack runs locally with an
  unpublished database; the exception must not spread to deploy files or code.
- The first build takes minutes: it downloads the Maven dependencies and compiles main and test code.
- The Maven bootstrap (JDK base image, wrapper, `pom.xml`, dependency download) is written in both
  Dockerfiles: a Java version bump changes both. The BuildKit cache mount for `~/.m2` is shared, so
  the dependencies download once.
- The demo image carries the test classpath (Testcontainers, ArchUnit, JUnit) and a full JRE —
  493 MB against 229 MB for production. It is never pushed or deployed.
- Two compose files to keep straight: a bare `docker compose` now means the demo, and commands on the
  development database need `-f compose.dev.yaml`.
- The demo and `spring-boot:test-run` both want port 8080 — one at a time, or move `API_PORT`.
- The health check allows 30 minutes to start, because `LARGE` takes about 15 to seed (measured
  2026-10-18, one core busy): a demo that hangs instead of failing is noticed only then.
- `keys` installs OpenSSL from Alpine's mirror the first time it runs, and the seeded "orders right
  now" age until the next `down -v`.
