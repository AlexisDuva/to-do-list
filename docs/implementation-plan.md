# Implementation Plan — To-Do List App

Goal: build this step by step so the developer can read and understand each part, rather than getting a fully generated app at once. **Stop after each step** and wait for confirmation before moving to the next one.

Reference docs: [functional-requirements.md](./functional-requirements.md), [class-diagram.md](./class-diagram.md), [api-documentation.md](./api-documentation.md), [tech-stack.md](./tech-stack.md).

## Steps

1. **Project scaffolding** *(done by the developer via Spring Initializr)*
   Developer generates the project at [start.spring.io](https://start.spring.io) — Gradle + Java Spring Boot, with dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway Migration — and adds the generated files to the repo. Agent then reviews the generated structure, confirms it matches the tech-stack decisions, and notes anything to adjust before moving to step 2.

2. **Database connection + first migration**
   Configure `application.properties` for local PostgreSQL, add the first Flyway migration (`V1__create_initial_schema.sql`) creating `project`, `task`, `tag`, `task_tag` tables. Verify the app starts and Flyway applies the migration.

3. **Entities**
   JPA entity classes: `Task`, `Project`, `Tag`, `Priority` enum, mapped to the schema from step 2, matching class-diagram.md.

4. **Repositories**
   Spring Data JPA repository interfaces for `Task`, `Project`, `Tag` (basic CRUD, plus query methods needed for filtering: by status, project, tag, priority, text search).

5. **DTOs**
   Request/response DTO classes for `Task`, `Project`, `Tag` matching the shapes in api-documentation.md, and mapping between entities and DTOs.

6. **Service layer**
   Business logic: `TaskService`, `ProjectService`, `TagService` — create/read/update/delete, complete/incomplete, filtering/sorting/search logic.

7. **Controllers**
   REST controllers implementing every endpoint from api-documentation.md, wired to the services.

8. **Error handling**
   Global exception handler (`@ControllerAdvice`) producing the error response shape from api-documentation.md (404s, validation errors, etc.).

9. **Testing**
   Unit tests for services, integration tests for controllers (e.g. using an in-memory or test PostgreSQL/Testcontainers setup).

10. **Manual verification**
    Run the app locally, exercise the API end-to-end (e.g. via curl/Postman) against the requirements doc to confirm behavior matches spec.

Deployment to Railway is a later, separate phase once the app works locally end-to-end.

## Working agreement

- Implement **one step at a time**.
- After finishing a step, stop, summarize what was created/changed and why, and wait for the developer to review before continuing.
- Keep code readable and avoid skipping ahead into later steps.
