# ADR-0004 — Ship a jlink-trimmed JRE on Alpine, with separate local and deploy compose files

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

The API needs a container image to deploy and a PostgreSQL to develop against (ADR-0003). Image
size is paid on every pull, and everything in the image is surface that runs in production; a
stock JDK image ships a compiler, tools and modules the API never loads. Local development and
deployment need opposite things from compose: a database reachable from the host versus a
database reachable only by the API.

## Decision

- **Build stage** (`eclipse-temurin:21-jdk-alpine-3.24`): packages through the wrapper with a
  Maven cache mount, extracts the Spring Boot jar in layers, lists the JDK modules with `jdeps`
  plus the ones loaded reflectively, and builds a JRE with `jlink` (debug info stripped, modules
  compressed, `en`/`pt-BR` locales only), then strips its native libraries.
- **Runtime stage** (`alpine:3.24`): that JRE plus the layered application, run by a non-root
  user, with no build tools. Dependencies sit in their own layer, so a code change ships only the
  application layer.
- **`compose.yaml`** is local: only PostgreSQL, bound to `127.0.0.1`, started by
  `./mvnw spring-boot:run` through Spring Boot's Docker Compose support.
- **`compose.deploy.yaml`** runs the API image and PostgreSQL: database not published, health
  checks, read-only root filesystem, `no-new-privileges`, restart policy.
- Secrets live only in `.env` (git-ignored); `.env.example` lists the variables.
- **Memory:** each deploy container has a limit (`API_MEMORY_LIMIT`, `POSTGRES_MEMORY_LIMIT`,
  512 MB by default). The JVM takes its heap as 50% of the container limit
  (`-XX:MaxRAMPercentage=50`), never a fixed `-Xmx`; PostgreSQL keeps its default 128 MB
  `shared_buffers`, 25% of its limit, with `shm_size` at 128 MB.

Measured on 2026-10-16: image 123 MB on disk, 88 MB compressed. Under a 512 MB limit and 200
parallel requests the API holds about 110 MB of non-heap plus 70–120 MB of threads, GC and
native memory: 286 MB resident in normal use, 462 MB with the whole heap pre-touched.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| `eclipse-temurin:21-jre-alpine` as runtime | Ships every JRE module, loaded or not |
| Distroless Java image | Full JRE as well, and no shell or `wget` for the health check |
| GraalVM native image | Smaller and faster to start, but minutes of build, heavy memory, and native hints for reflection-heavy Hibernate; revisit if size or startup becomes the bottleneck |
| Buildpacks (`spring-boot:build-image`) | Convenient, but a larger Ubuntu-based image with less control over the runtime |
| One compose for both uses | Local wants the database on the host and no API container; deploy wants the opposite |
| Heap at 75% of the limit | With the heap full, 505 of 512 MB resident: any non-heap growth gets the container killed by the kernel, with no `OutOfMemoryError` to diagnose |
| No memory limit | The JVM sizes its heap from the host's RAM and can starve PostgreSQL on the same machine |

## Pros

- The image carries only the JVM modules the API uses, plus a 9 MB Alpine base.
- Non-root, read-only, no build tools in production.
- A code-only change pushes a layer of kilobytes, not the dependencies again.
- The same PostgreSQL image runs locally, in deploy and in Testcontainers.

## Cons

- A JDK module loaded only by reflection is invisible to `jdeps`: it fails at runtime and must be
  added to the `jlink` list by hand — running the image is part of the check.
- musl instead of glibc: a dependency shipping glibc-only native code will not load.
- Compressed `jlink` modules barely shrink further over the wire: disk gains exceed pull gains.
- PostgreSQL in the deploy compose is a single node with no backups; a managed database replaces
  it before real production traffic.
- Half of the API's memory goes to things other than the heap, and that share grows with every
  entity, bean and Tomcat thread (200 threads cost about 47 MB). The 50 MB of worst-case headroom
  must be measured again as the API grows — raise `API_MEMORY_LIMIT`, not the percentage.
