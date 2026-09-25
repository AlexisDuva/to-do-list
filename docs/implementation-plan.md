# Implementation Plan — To-Do List App

Goal: build this step by step so the developer can read and understand each part, rather than getting a fully generated app at once. **Stop after each step** and wait for confirmation before moving to the next one.

Reference docs: [functional-requirements.md](./functional-requirements.md), [class-diagram.md](./class-diagram.md), [api-documentation.md](./api-documentation.md), [tech-stack.md](./tech-stack.md).

## Steps

1. **Project scaffolding** *(done by the developer via Spring Initializr)*
   Developer generates the project at [start.spring.io](https://start.spring.io) — Gradle + Java Spring Boot, with dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Flyway Migration — and adds the generated files to the repo. Agent then reviews the generated structure, confirms it matches the tech-stack decisions, and notes anything to adjust before moving to step 2.

2. **Database connection + first migration**
   1. Run PostgreSQL via Docker locally (`docker run` with a `todolist` DB/user/password).
   2. Add datasource config to `application.properties` (URL, username, password) plus `spring.jpa.hibernate.ddl-auto=validate` — set to `validate` (not `update`/`create`) because Flyway owns the schema, not Hibernate, avoiding the two fighting each other.
   3. Add the first Flyway migration — `V1__create_initial_schema.sql` creating `project`, `task`, `tag`, and the `task_tag` join table, matching the class diagram.
   4. Verify by running `./gradlew bootRun` and confirming in the logs that Flyway applied `V1` successfully.
   5. Commit: `"Step 2: database connection and first Flyway migration"`.

3. **Entities**
   1. Create the `com.alex.todolist.entity` package.
   2. `Priority` enum (`LOW`, `MEDIUM`, `HIGH`).
   3. `Project` entity — `id`, `name`, inverse `tasks` (`@OneToMany(mappedBy = "project")`).
   4. `Tag` entity — `id`, unique `name`.
   5. `Task` entity — `id`, `title`, `description`, `dueDate`, `priority` (`@Enumerated(EnumType.STRING)` so the DB stores readable values, not ordinals), `completed`, `createdAt`, `project` (`@ManyToOne @JoinColumn(name = "project_id")`), `tags` (`@ManyToMany` via the existing `task_tag` join table).
   6. No Lombok — plain getters/setters/constructors, since `build.gradle` doesn't include it (keeps things explicit for a learning project).
   7. Verify: run `./gradlew bootRun` in the background and inspect the log. `ddl-auto=validate` (set in step 2) makes Hibernate compare the entities against the schema Flyway already created — a clean startup confirms the mapping is correct, while a `SchemaManagementException` names the mismatch to fix. No endpoints exist yet, so this structural check is the whole verification; stop the process afterward.
   8. Commit: `"Step 3: JPA entities (Task, Project, Tag, Priority)"`.

4. **Repositories**
   1. Create the `com.alex.todolist.repository` package.
   2. `ProjectRepository extends JpaRepository<Project, Long>` — basic CRUD only, no extra methods needed.
   3. `TagRepository extends JpaRepository<Tag, Long>` — basic CRUD only.
   4. `TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task>` — `JpaSpecificationExecutor` adds a `findAll(Specification<Task> spec)` method, which lets the service layer (step 6) dynamically combine the optional filters from `GET /api/tasks` (status, project, tag, priority, text search) into one query, instead of writing a derived-query-method for every possible filter combination.
   5. Sanity check: add a temporary `CommandLineRunner` bean that saves a test `Task` via `TaskRepository` and fetches it back, printing the result to the console — proves the repository actually works end-to-end against the real database. Remove this bean before committing.
   6. Verify: run `./gradlew bootRun`, confirm the sanity check prints the expected saved/fetched task with no errors, then remove the `CommandLineRunner` and confirm the app still boots cleanly.
   7. Commit: `"Step 4: Spring Data JPA repositories"`.

5. **DTOs**
   1. Create the `com.alex.todolist.dto` package.
   2. Use Java `record`s (not classes/Lombok) — immutable, boilerplate-free, fits Java 21.
   3. Response DTOs matching api-documentation.md's response shapes: `TaskResponse` (flattens `project`/`tags` into `projectId`/`tagIds`), `ProjectResponse`, `TagResponse` — each with a static `fromEntity(...)` factory method mapping the entity to the DTO.
   4. Request DTOs matching the `POST`/`PUT` bodies: `TaskRequest`, `ProjectRequest`, `TagRequest` — plain data holders, no mapping method. Building an entity from a request needs repository lookups (resolve `projectId`/`tagIds` into real managed entities), which is business logic deferred to the service layer (step 6).
   5. Validation annotations (e.g. `@NotBlank` on `title`/`name`) are **not** added yet — that needs the `spring-boot-starter-validation` dependency and ties into the error response shape, which belongs to step 8 (error handling).
   6. Verify: `./gradlew compileJava` — confirms the DTOs and mapping code compile cleanly. No runtime behavior yet since nothing is wired to controllers/services until steps 6–7.
   7. Commit: `"Step 5: DTOs and entity-to-DTO mapping"`.

6. **Service layer**
   1. Create the `com.alex.todolist.service` package.
   2. Add `com.alex.todolist.exception.ResourceNotFoundException` (unchecked) — a forward-reference to step 8: services throw it when an id doesn't exist, but nothing catches it into a proper `404` JSON response until step 8 adds the `@ControllerAdvice`.
   3. `ProjectService` — `getAll`, `getById` (throws `ResourceNotFoundException`), `create`, `update`, `delete`. `delete` must first unassign the project's tasks (fetch via a new `TaskRepository.findByProjectId(Long)`, set `project = null`, save each), then delete the project — required both by the API contract and by the `project_id` foreign key (no `ON DELETE` clause, so deleting a referenced project would otherwise violate the constraint).
   4. `TagService` — `getAll`, `getById`, `create`, `update`, `delete`. No extra unassignment logic needed: `task_tag` was created with `ON DELETE CASCADE` on both FKs (step 2's migration), so the database removes the join rows automatically when a tag is deleted.
   5. `TaskService` — the most involved:
      - `getAll(status, projectId, tagId, priority, search, sortBy, sortDir)`: builds a `Specification<Task>` dynamically, ANDing in only the filters actually provided (`tagId` needs `root.join("tags")` + `query.distinct(true)` to avoid duplicate rows from the many-to-many join); builds a `Sort` from `sortBy`/`sortDir`; calls `taskRepository.findAll(spec, sort)`; maps results to `TaskResponse`.
      - `getById(id)`.
      - `create(TaskRequest)`: resolves `projectId`/`tagIds` into real managed `Project`/`Tag` entities via their repositories (the mapping step 5 deferred), defaults `priority` to `MEDIUM` if absent, sets `completed = false`, `createdAt = now()`.
      - `update(id, TaskRequest)`: fetches the existing task (404 if missing), re-resolves project/tags, updates fields, saves.
      - `complete(id)` / `incomplete(id)`: fetch, flip `completed`, save.
      - `delete(id)`.
   6. Add `List<Task> findByProjectId(Long projectId)` to `TaskRepository` (needed by `ProjectService.delete`).
   7. Sanity check: temporary `CommandLineRunner` exercising `TaskService` specifically (create a task, `getAll()` with a filter, mark complete, delete) — the one service with real logic worth a live check; `ProjectService`/`TagService` are simple CRUD passthroughs covered properly in step 9. Remove the runner before committing.
   8. Verify: run `./gradlew bootRun`, confirm the sanity check output is correct, remove the runner, confirm the app still boots cleanly.
   9. Commit: `"Step 6: service layer (business logic)"`.

7. **Controllers**
   1. Create the `com.alex.todolist.controller` package.
   2. `ProjectController` (`@RequestMapping("/api/projects")`) — `GET ""`, `GET "/{id}"`, `POST ""` (201), `PUT "/{id}"`, `DELETE "/{id}"` (204), calling `ProjectService`.
   3. `TagController` — same shape at `/api/tags`, calling `TagService`.
   4. `TaskController` — `/api/tasks`: `GET ""` with all six filters as `@RequestParam(required = false)` (`status`, `projectId`, `tagId`, `priority`, `search`, `sortBy`, `sortDir`, passed straight into `TaskService.getAll`), `GET "/{id}"`, `POST ""` (201), `PUT "/{id}"`, `PATCH "/{id}/complete"`, `PATCH "/{id}/incomplete"`, `DELETE "/{id}"` (204).
   5. Not-found behavior is still incomplete at this point, expected rather than a bug: hitting a missing id currently returns a generic `500`, not the `404` from api-documentation.md, since nothing catches `ResourceNotFoundException` yet — that's step 8.
   6. Verify via **curl against a real running app** (first step where this is possible) instead of a `CommandLineRunner`: `./gradlew bootRun` on the default port, then POST a project, GET the list, POST a task referencing it, GET with a filter, PATCH complete, DELETE — confirming status codes and JSON shapes match api-documentation.md. Clean up any test rows created, then stop the app.
   7. Commit: `"Step 7: REST controllers"`.

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
