# foody-api — working rules

Spring Boot 4.1 · Java 21 · Maven (always through the wrapper, `./mvnw`). Base package
`com.foody.api`. Serves `foody-app`. This file is the **single source of truth for this repo's
rules** — to change a rule, edit it here.

## Standing rules

These apply to every task, no exceptions, without needing to be restated.

**1. Only a local database may be written to.** No `INSERT`/`UPDATE`/`DELETE`/DDL/migration
against any database whose host is not **provably** loopback (`localhost`, `127.0.0.1`, a local
container) without explicit per-case authorization from the user. Remote, undetermined or
ambiguous host ⇒ blocked.

**2. Never commit or publish on your own.** No `commit`, `push`, `merge`, `rebase`, `tag`,
`gh pr create` — on any branch. Leave the change in the working tree, describe what would be
committed, and wait.

**3. The branch is chosen by the owner.** Never create or switch branches on your own
initiative.

**4. Never loosen a check to make a rule pass.** If the compiler, a test or a linter flags a
violation, fix the code — never the build config, and never a `@SuppressWarnings` to make the
warning go away.

**5. "Local" does not mean isolated.** Before running anything that writes or fires (e-mail,
payments, third-party APIs), check what the active profile's `application*.properties` points at.

**6. Every new architectural decision becomes a versioned ADR.** Chose between alternatives,
established a pattern everyone must follow, or decided **not** to do something? Write
`docs/adr/NNNN-title.md` with **the why, the pros and the cons**, and commit it **in the same
PR as the change** — a decision that lives only in the author's head becomes rework next
semester. Format and criteria: [`docs/adr/README.md`](./docs/adr/README.md).

## Code rules

They apply to the **file you touched**. Legacy code is not swept or migrated in bulk — except
by R14: a touched file loses every comment.

### R1 — No nesting

- **Guard clause / early return.** Handle the exit first and `return`; the happy path stays
  unindented.
- **No `else` after `return`.** If the `if` returns, the `else` is noise.
- **No `if / else if / else` chains** → use a `Map` or an enum that carries the behaviour (R3).
- **`switch` is forbidden** — statement or expression, `default` included — always a `Map` or
  an enum.
- **No nested ternaries** to pick a value.
- Keep depth shallow: `if` inside `if` inside `for` means extract a method.

```java
// bad
if (a) { ... } else { if (b) { ... } else { ... } }
switch (type) { case X -> fx(); default -> fy(); }
String label = a ? "A" : b ? "B" : "C";

// good
if (!a) return handleA();
if (!b) return handleB();
return happy();

private final Map<Type, Supplier<Result>> handlers = Map.of(Type.X, this::fx, Type.Y, this::fy);
return handlers.getOrDefault(type, this::fy).get();
```

### R2 — Files ≤ 800 lines

Past 800, **split by responsibility**, not by line count. Fat service → thin orchestrator +
cohesive sub-services (validation, sync, notification). Fat repository → repository +
query/specification builder.

### R3 — No magic strings or numbers

- Domain values **always** live in an enum. Look for the existing enum before creating one.
- Domain lists are **derived from the enum** (`EnumSet.allOf(OrderStatus.class)`), never
  retyped.
- Magic numbers become named constants: `MAX_UPLOAD_BYTES`, not `5 * 1024 * 1024`.
- **Queries live in the repository** — `@Query` or a named constant there, never a JPQL/SQL
  string built in the middle of a service method. Repeated table/column names become constants.
- Error/response messages go through the message catalog (R7), never a literal repeated across
  files.
- **Domain-level constants live in `<Domain>Constants` (a `final` class with a private
  constructor) in the domain package, never as loose `private static final` fields at the top
  of a service, controller or repository.** That includes index/column maps, lookup tables,
  fixed protocol values and regexes.

### R4 — Reuse before you create

Search `com.foody.api.shared` **before** writing — one `grep` costs less than a duplicated
helper. New code is born **in the domain package** that uses it; only promote to `shared` when a
second domain needs it. Generic utils are never copied per domain.

**No generic `private static` helpers inside a service/repository.** A pure function (escape,
format, parse) goes, in this order:

1. already exists in `com.foody.api.shared` → use it;
2. generic (CSV, dates, strings) → `com.foody.api.shared.util.<Name>Utils` + its test;
3. only makes sense for this domain → `com.foody.api.<domain>.util`.

### R5 — `controller → service → repository`

- **Controller** holds no business logic — validates input (`@Valid`), delegates, returns.
- **Service** orchestrates and **never touches the database directly** — no `EntityManager`,
  `JdbcTemplate` or `DataSource` in a service.
- **Repository** is the only layer that talks to the database.

### R6 — TDD: the test fails first

Write the failing test, run it, and confirm it failed **for the right reason** (broken
assertion, not a compilation error). Then the minimum code that passes. Then refactor on green.

### R7 — Messages via the message catalog

Error and response messages are never loose strings — **in any layer**: controller, service,
repository, scheduler, util. They live in one catalog, `com.foody.api.shared.message.Messages`
(create it with the first message). This covers **every** `throw` — JDK exceptions,
`ResponseStatusException` and the domain's own exceptions. A variable detail is appended to the
catalog text, never baked into a literal:

```java
// bad
throw new IllegalStateException("Restaurante não encontrado: " + id);

// good
public class RestaurantNotFoundException extends NotFoundException {
    public RestaurantNotFoundException(UUID id) {
        super(Messages.RESTAURANT_NOT_FOUND, id.toString());
    }
}
```

Before creating a key, `grep` the catalog — the one you need may already exist.

### R8 — No escape-hatch types

No raw types (`List` instead of `List<Order>`), no `Object` standing in for a type you didn't
model, no unchecked casts. Don't know the shape? Model it — a `record`, a `sealed` interface or
a generic.

### R9 — Cohesive modules

Package by feature: one package per domain (`com.foody.api.<domain>`), with
`controller`/`service`/`repository`/`dto` separated inside it. A growing service becomes an
orchestrator plus sub-services; it does not become a 40-method class.

A feature that must react to another one's change listens to a domain event the other publishes
(ADR-0019); a feature that must ask another one a question declares it as an interface in its own
package, and the feature that owns the data implements it (ADR-0023). Feature packages never depend
on each other in both directions — `ArchitectureTest` checks it.

### R10 — No `System.out` / `System.err` / `printStackTrace()`

Use the SLF4J logger (`@Slf4j`), with `{}` placeholders — never a message built by concatenation.
Every line inside a request already carries its `requestId` (ADR-0010) — never add it by hand.
Work handed to another thread (`@Async`, schedulers) copies the MDC on purpose or loses the id. A
log line never holds personal data beyond ids, nor any secret (R17).

### R11 — No fire-and-forget async

Every `CompletableFuture` is joined, returned, or has an explicit `.exceptionally(...)` /
`.whenComplete(...)`. An `@Async` method returns `CompletableFuture`, not `void`. In schedulers
and listeners this is what separates a logged error from an error that vanishes.

### R12 — AAA tests naming the acceptance criterion

Arrange, Act, Assert in that order, never interleaved. The **name states the acceptance
criterion** in plain language (`@DisplayName` or the method name). Every behaviour gets a happy
path **and** an error/edge path.

### R13 — Errors end in the global handler

Exceptions become HTTP responses in **one** `@RestControllerAdvice`,
`com.foody.api.shared.exception.GlobalExceptionHandler`, as Problem Details (R19). A domain
exception extends one of `InvalidRequestException` (400), `UnauthorizedException` (401),
`NotFoundException` (404), `ConflictException` (409), `BusinessRuleException` (422) or
`TooManyRequestsException` (429) — never a new `@ExceptionHandler` per exception. A header the
error needs (`WWW-Authenticate`, `Retry-After`) comes from the base's `headers()`, which the handler
applies; Spring Security's 401 and 403 reach the same handler (ADR-0014). A `catch` exists only to
translate into a domain exception **keeping the cause** —
never swallow the error, never an empty `catch`, never `catch (Exception e) { log; return null; }`.

```java
} catch (DataAccessException exception) {
    throw new OrderPersistenceException(exception);
}
```

### R14 — Zero comments: a touched file leaves without any

No comment of any kind — `//`, `/* */` or Javadoc — in code **or in tests** (no `// Arrange` /
`// Act` / `// Assert`). The name says what the code does; the why goes in the PR description and
in the commit message.

**A file you touch leaves with zero comments, including the ones that were already there.**
Remove them in the same change. This is the one rule that does sweep legacy code — file by file,
only the files the change touches.

Not comments, and therefore kept: tool directives that change behaviour (`// @formatter:off`),
and only where removing them breaks the build. Never add one to dodge a rule (standing rule 4).

Check with `bash scripts/check-comments.sh` (CI runs it too): it reads every Java file in the repo,
ignores what sits inside string and char literals — a URL is not a comment — and prints each line
that still carries one.

### R15 — Always explicit types

No `var` — local variables are declared with their type. Inference is not a readable contract.

### R16 — One package per role

Don't duplicate packages with the same role inside a domain (`dto` **or** `dtos`, not both).

### R17 — Authentication and secrets

Authentication follows ADR-0002 and ADR-0013:

- Every endpoint requires a valid access token. The public ones — `POST /auth/register`,
  `/auth/login`, `/auth/refresh`, `/auth/logout` and `GET /actuator/health` — are listed in one
  place, the security configuration; no `permitAll` anywhere else.
- Every other controller method declares its access with `@RequiresAuthentication`,
  `@RequiresScreen` or `@RequiresGrant` (ADR-0012); `ArchitectureTest` fails the build otherwise.
  Grants live in `RoleGrants`, never as a role check in a service.
- Tokens, passwords, password hashes and the `Authorization` header never reach a log, an
  exception message or a response body — except the access token that `/auth/register`,
  `/auth/login` and `/auth/refresh` return.
- Refresh tokens are stored only as hashes. Passwords go through Spring Security's
  `PasswordEncoder`, never a hand-rolled hash.
- Rate limits are declared in `foody.rate-limit.*` and enforced by the filter in `shared.ratelimit`
  (ADR-0017); a new endpoint that needs its own limit adds a policy there.
- Signing keys and secrets come from configuration outside the repository — nothing secret is
  committed, not even for local use.

### R18 — Persistence

PostgreSQL through Spring Data JPA, schema through Flyway (ADR-0003), data conventions in
ADR-0005:

- **Every entity extends `BaseEntity`** (`com.foody.api.shared.persistence`): UUID v7 `id`,
  `createdAt`, `updatedAt` and `version`. Its table has `id uuid primary key`,
  `created_at timestamptz not null`, `updated_at timestamptz not null` and
  `version bigint not null`.
- **Switched off, not deleted** (ADR-0006): an entity that is disabled and enabled again
  (product, customer, supplier) extends `ActivatableEntity` — `disabled_at timestamptz`, `null`
  while active. Every query on it decides explicitly whether it keeps only active rows; a
  customer-facing one always does.
- **Delete removes the row.** Deleting something still referenced is refused in the service with
  a `ConflictException` that points to disabling. Soft delete is the exception, per entity:
  `@SoftDelete(strategy = TIMESTAMP, columnName = PersistenceConstants.DELETED_AT_COLUMN)` plus
  partial unique indexes (`… where deleted_at is null`).
- **Instants are `Instant` / `timestamptz`; money is `BigDecimal` / `numeric(12,2)`** — never
  `double`, `float`, `LocalDateTime` or `timestamp` without time zone. The current instant comes
  from the injected `Clock` (ADR-0016), never `Instant.now()`.

- **The schema changes only through a new migration**,
  `src/main/resources/db/migration/V<n>__<description>.sql`. An applied migration is never
  edited — the fix is another migration. `ddl-auto` is `validate`, never anything else.
- **Entities never leave the service.** Controllers receive and return DTO `record`s from the
  domain's `dto` package (R9).
- **Transactions are declared on service methods** — `@Transactional`, with `readOnly = true` for
  reads — never on controllers.
- **A list that needs a relation loads it in the repository query** (fetch join or
  `@EntityGraph`), never by touching a lazy association inside a loop (N+1).
- **Database tests run against PostgreSQL in Testcontainers** — no H2, no mocked repository
  standing in for a query under test.

### R19 — One HTTP contract

Every endpoint follows ADR-0005:

- **Errors are Problem Details** with a `code` (the `Messages` key) and, on validation,
  `errors[]`. A controller never builds an error body — it throws (R13).
- **A list endpoint is paginated on the server:** it takes a `Pageable`, passes it through
  `SortGuard.requireAllowed(pageable, <the endpoint's sortable properties>)` and returns
  `PageResponse.of(page)`. Page sizes are 10, 25, 50 or 100 (`PaginationConstants`); never return
  an unpaginated list of a table that grows.
- **Instants go out as ISO-8601 UTC, ids as UUID strings, money as decimal numbers.**
- A DTO component that may be absent is annotated `@Nullable` (JSpecify); everything else is
  required in the contract (ADR-0018).
- Every operation is published as `<controller><Method>` (`adminUserList`, `menuQuote`) and must be
  unique — `OpenApiContractTest` fails on a duplicate (ADR-0021). Renaming a controller method
  renames its operation in the front's generated types.
- **Every response carries `X-Request-Id`, and every error body its `requestId`** (ADR-0010).
- **`docs/openapi.json` is the published contract** (ADR-0008): `OpenApiContractTest` rewrites it
  on every test run. Commit it with the change that alters it; never edit it by hand.

### R20 — Test data comes from factories; local data from seeders

Seed code is test code, in `src/test/java/com/foody/seed/<domain>/` (ADR-0007):

- **Every entity has a factory:** `<Entity>Factory extends EntityFactory<Entity>`, whose `build()`
  returns a valid entity filled by Datafaker (pt-BR). Integration tests create their data through
  factories — `factory.create()`, `factory.create(Entity::disable)`, `factory.createMany(n)` — not
  by setting fields by hand.
- **A domain worth seeing locally has a seeder:** `<Domain>Seeder implements Seeder`, idempotent
  through `alreadySeeded()`, its volume as `scale.of(<base count>)`, with `@Order` when it needs
  another seeder's data.
- **Test classes in `com.foody.seed` are named `*Test`** — the seed scan skips them and their fakes.
- **Test-only entities and migrations go in `com.foody.testsupport` and `db/testsupport`**, never in
  `com.foody.api` or `db/migration` — those reach the local database through `test-run`.
- Fixed credentials a seeder creates come from `.env`, never from code.

## Environment

- **Local database:** copy `.env.example` to `.env` and set `POSTGRES_PASSWORD`.
  `./mvnw spring-boot:run` starts `compose.yaml` (PostgreSQL on `127.0.0.1:5432`) through Spring
  Boot's Docker Compose support — no datasource properties to set.
- **JWT keys:** run `bash scripts/generate-jwt-keys.sh` once. It writes `.secrets/jwt-private.pem`
  and `.secrets/jwt-public.pem` (git-ignored) and refuses to overwrite them without `--force`; the
  API does not start without them (ADR-0015). Tests generate their own pair.
- **Local data:** `./mvnw spring-boot:test-run` does the same and runs the seeders (R20); add
  `-Dspring-boot.run.arguments=--foody.seed.scale=LARGE` for volume. The seed reads its accounts
  from `.env`: `SEED_ADMIN_EMAIL` and `SEED_ADMIN_PASSWORD` for the admin, `SEED_STAFF_EMAIL` and
  `SEED_CUSTOMER_EMAIL` for one staff and one customer, and `SEED_DEFAULT_PASSWORD` for everyone but
  the admin — 4 more staff and 50 customers per scale unit. Without them `test-run` refuses to
  start. Seeders skip tables that already have data — for a fresh start, `docker compose down` and
  remove the `foody-local_postgres-data` volume. The seed profile raises every rate limit
  (`rate-limit-generous.properties`, also used by `@ApiIntegrationTest`), so local exploration and
  E2E runs are never throttled. Swagger UI is there too, at
  `http://localhost:8080/swagger-ui.html` — and only there: springdoc is a test dependency.
- **Image:** `docker build -t foody-api .` (ADR-0004). **Deploy:**
  `docker compose -f compose.deploy.yaml up -d`, with the `.env` on the host and the key pair in
  `.secrets/` (or wherever `JWT_PRIVATE_KEY_FILE` and `JWT_PUBLIC_KEY_FILE` point), mounted as
  compose secrets. `FOODY_WEB_ALLOWEDORIGINS` lists the front's origins. Nothing secret lives in
  the compose files or the image.
- **Memory:** the heap is 50% of the container limit. To give the API more memory, raise
  `API_MEMORY_LIMIT` — never add `-Xmx` or raise the percentage: the other half is non-heap,
  threads and native memory, and running short of it gets the container killed (ADR-0004).

## Before handing off

`./mvnw verify` · `bash scripts/check-comments.sh` — the same gates CI runs (ADR-0009). `verify`
must end in `BUILD SUCCESS`; it checks the format (fix it with `./mvnw spring-javaformat:apply`),
runs the tests and the architecture rules in `ArchitectureTest` (layers, packages, entities,
exception handling, protected controllers, package cycles, standard streams, field injection) and rewrites
`docs/openapi.json`. Docker
must be running: database tests use Testcontainers (R18). Changed a dependency or the `Dockerfile`?
Also bring up `compose.deploy.yaml` and wait for the API to report healthy — a JDK module that
`jdeps` cannot see fails only when the image runs (ADR-0004). A rule that blocks you is fixed in the
code, never by loosening `ArchitectureTest` or the build (standing rule 4).

**Self-review against what reviewers keep flagging** — run over the files you touched and fix
every hit before handing off:

- `grep -nE 'throw new \w+\(\s*"'` finds a `throw` with a literal message → R7 (multi-line
  `throw`s need a look at the next line too).
- `grep -nwE 'var'` → R15.
- `grep -niE 'log\.\w+\(.*(token|password|authorization)'` → R17.
- a `private static final` constant or a generic `private static` helper at the top of a
  service/controller → R3 or R4.
- the same literal or the same set of values repeated in two files → R3 (one enum/constant,
  imported).

If your change surfaces on a screen, the E2E spec for it lives in the **front** repo and is
mandatory there — see `foody-app/CLAUDE.md`, standing rule 7.

## When to open another file

Writing code needs none — what's above is enough. Recording an architectural decision
(standing rule 6) → [`docs/adr/README.md`](./docs/adr/README.md).
