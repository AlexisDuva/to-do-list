# Difficulties Encountered

Log of technical difficulties hit during implementation, ordered by step. See [implementation-plan.md](./implementation-plan.md) for what each step covers. Steps with no entry had no difficulties.

## Step 6 — Service layer

**1. `Specification.where(null)` was ambiguous**
Spring Data JPA (as shipped with Spring Boot 4.1.1) added a second `where(PredicateSpecification<T>)` overload alongside the original `where(Specification<T>)`. Passing `null` as the starting "no filter yet" specification couldn't resolve which overload was meant — even with an explicit `Specification.<Task>where(null)` type witness, compilation failed with "reference to where is ambiguous".

Fix: start from an always-true predicate instead of `null`:
```java
Specification<Task> spec = (root, query, cb) -> cb.conjunction();
```
Functionally identical (no filter applied yet), but avoids the ambiguous overload entirely.

**2. `LazyInitializationException` on `Task.tags`**
`@ManyToMany` fields default to `FetchType.LAZY` — Hibernate only loads `tags` from the DB when something calls `.getTags()`. Spring Data repository methods open and close their own transaction/session by the time they return. So `TaskResponse.fromEntity(task)` failed when reading `task.getTags()` for entities returned by `taskRepository.findAll(spec, sort)`, since the Hibernate session was already closed by the time the mapping code ran in `TaskService`.

Fix: added `@Transactional` to the `TaskService` class, keeping the session open for the whole service method — including the entity-to-DTO mapping that reads the lazy `tags` collection.

## Step 9 — Testing

**1. Testcontainers artifacts had no resolvable version**
Adding `testImplementation 'org.testcontainers:junit-jupiter'` / `'org.testcontainers:postgresql'` with no version failed ("Could not find org.testcontainers:junit-jupiter:."). Spring Boot's dependency management didn't pin a concrete version automatically the way it does for Spring's own starters.

Fix: explicitly import the Testcontainers BOM, `platform('org.testcontainers:testcontainers-bom:2.0.5')`, which pins consistent versions for all `org.testcontainers:*` modules at once.

**2. `TestRestTemplate` wasn't on the classpath**
Spring Boot 4 removed the old monolithic `spring-boot-starter-test` in favor of granular test starters, and `TestRestTemplate` isn't where Boot 3-era guides say it is. The actual artifact is `spring-boot-resttestclient`, and the class itself moved package, from `org.springframework.boot.test.web.client.TestRestTemplate` to `org.springframework.boot.resttestclient.TestRestTemplate`. Found by grepping the actual jar contents in the Gradle cache rather than guessing further.

**3. `TestRestTemplate` still wasn't auto-configured**
Even with the right dependency, `@SpringBootTest(webEnvironment = RANDOM_PORT)` no longer auto-configures a `TestRestTemplate` bean by itself (it did in Boot 3). Boot 4 requires an explicit `@AutoConfigureTestRestTemplate` annotation on the test class. Adding it then surfaced `ClassNotFoundException: org.springframework.boot.restclient.RestTemplateBuilder`, fixed by also adding the `spring-boot-restclient` dependency.

**4. Testcontainers 2.x renamed both artifact coordinates and packages**
Testcontainers 2.0 prefixed all module artifact ids (`org.testcontainers:junit-jupiter` → `org.testcontainers:testcontainers-junit-jupiter`, same for `postgresql`) — old and new names/versions can't be mixed (e.g. requesting the new artifact name at an old 1.x version resolves to nothing). Separately, `org.testcontainers.containers.PostgreSQLContainer` is deprecated in favor of `org.testcontainers.postgresql.PostgreSQLContainer`, which also dropped its self-typing generic (`PostgreSQLContainer<SELF extends ...>` → plain `PostgreSQLContainer`).

Fix: aligned on the 2.x naming throughout (`testcontainers-junit-jupiter`, `testcontainers-postgresql`, BOM version `2.0.5`), and switched the import/field type to the non-deprecated `org.testcontainers.postgresql.PostgreSQLContainer` (no generic parameter).

**5. `@Container` broke Testcontainers sharing across test classes**
After adding `spring-boot-testcontainers` and refactoring `AbstractIntegrationTest` to use `@Container @ServiceConnection` (replacing the manual `static { POSTGRES.start(); }` + `@DynamicPropertySource` wiring), 6 of 22 tests started failing with `ConnectException`/`CannotCreateTransactionException` and a full run went from ~12s to over 5 minutes.

Cause: `@Container` tells the JUnit 5 Testcontainers extension to manage that field's lifecycle, **stopping the container after the last test in each test class**. `POSTGRES` is a `static` field inherited from `AbstractIntegrationTest` and shared (same instance) across `ProjectControllerIT`, `TagControllerIT`, `TaskControllerIT`. As soon as the first class's tests finished, `@Container` stopped the shared container — every subsequent class then hit a dead database (the 5-minute runtime was HikariCP retrying the now-refused connection before giving up).

Fix: kept `@ServiceConnection` (it detects the annotated field independently of container lifecycle management, so it doesn't need `@Container` to work), but reverted to the manual `static { POSTGRES.start(); }` (no matching `stop()`, relying on Testcontainers' Ryuk reaper to clean it up at JVM shutdown) — preserving true cross-class sharing while still avoiding the manual `@DynamicPropertySource` boilerplate.

## Step 10 — Manual verification

A checklist-driven pass against `functional-requirements.md` (running app, real dev Postgres) surfaced 3 issues the automated test suite hadn't caught:

**1. "See overdue tasks highlighted or grouped separately" was never implemented**
Grepping the codebase and `api-documentation.md` for "overdue" turned up nothing. The requirement exists in `functional-requirements.md` (step 1) but was silently dropped by the time the API was designed (step 2) and never caught since — nothing in steps 2–9 would have surfaced it, since none of them re-checked the original requirements doc line by line.

**2. `PUT /api/tasks/{id}` threw `500` when `tagIds` was omitted**
See the "Fix ..." commit for root cause (`List.of()` immutability vs. Hibernate's merge-time `clear()`). Notable because step 9's integration tests exercised `PUT`-adjacent paths (create, complete, delete) but never an update omitting `tagIds` specifically — a gap in test coverage, not just application code.

**3. Sorting by priority was alphabetical, not semantically ranked**
`priority` is stored via `@Enumerated(EnumType.STRING)`, so `ORDER BY priority` sorts the text values (`HIGH < LOW < MEDIUM`) rather than logical priority rank. `sortDir=desc` returned `MEDIUM, MEDIUM, LOW, HIGH` — the opposite of what "highest priority first" should mean. Not caught earlier because no automated test asserted the actual *order* of a priority-sorted result, only that sorting didn't error.
