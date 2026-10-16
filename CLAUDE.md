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
throw new RestaurantNotFoundException(Messages.RESTAURANT_NOT_FOUND.text() + ": " + id);
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

### R10 — No `System.out` / `System.err` / `printStackTrace()`

Use the SLF4J logger (`@Slf4j`).

### R11 — No fire-and-forget async

Every `CompletableFuture` is joined, returned, or has an explicit `.exceptionally(...)` /
`.whenComplete(...)`. An `@Async` method returns `CompletableFuture`, not `void`. In schedulers
and listeners this is what separates a logged error from an error that vanishes.

### R12 — AAA tests naming the acceptance criterion

Arrange, Act, Assert in that order, never interleaved. The **name states the acceptance
criterion** in plain language (`@DisplayName` or the method name). Every behaviour gets a happy
path **and** an error/edge path.

### R13 — Errors end in the global handler

Exceptions become HTTP responses in **one** `@RestControllerAdvice`
(`com.foody.api.shared.exception.GlobalExceptionHandler`, created with the first one). A `catch`
exists only to translate into a domain exception **keeping the cause** — never swallow the
error, never an empty `catch`, never `catch (Exception e) { log; return null; }`.

```java
} catch (DataAccessException exception) {
    throw new OrderPersistenceException(Messages.ORDER_SAVE_FAILED.text(), exception);
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

Check before handing off — anything printed that is not inside a string literal is a comment to
remove. `BASE` is the branch the work started from, so files already committed on the branch are
covered too. Comments at the end of a line print as well; URLs inside strings also print — those
are not comments.

```bash
BASE=main
{ git diff --name-only --diff-filter=d "$BASE"...HEAD; git diff --name-only --diff-filter=d HEAD; \
  git ls-files --others --exclude-standard; } | sort -u | grep -E '\.java$' \
  | xargs grep -nE '//|/\*|^\s*\*' | grep -vE '@formatter:'
```

### R15 — Always explicit types

No `var` — local variables are declared with their type. Inference is not a readable contract.

### R16 — One package per role

Don't duplicate packages with the same role inside a domain (`dto` **or** `dtos`, not both).

## Before handing off

`./mvnw verify` — compiles, runs the tests and must end in `BUILD SUCCESS`.

**Self-review against what reviewers keep flagging** — run over the files you touched and fix
every hit before handing off:

- `grep -nE 'throw new \w+\(\s*"'` finds a `throw` with a literal message → R7 (multi-line
  `throw`s need a look at the next line too).
- `grep -nE 'System\.(out|err)|printStackTrace'` → R10.
- `grep -nwE 'var'` → R15.
- a `private static final` constant or a generic `private static` helper at the top of a
  service/controller → R3 or R4.
- the same literal or the same set of values repeated in two files → R3 (one enum/constant,
  imported).

If your change surfaces on a screen, the E2E spec for it lives in the **front** repo and is
mandatory there — see `foody-app/CLAUDE.md`, standing rule 7.

## When to open another file

Writing code needs none — what's above is enough. Recording an architectural decision
(standing rule 6) → [`docs/adr/README.md`](./docs/adr/README.md).
