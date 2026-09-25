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
