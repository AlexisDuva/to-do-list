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
