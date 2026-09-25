# Tech Stack — To-Do List App

## Backend

- **Java Spring Boot** — REST API backend for the to-do list app, serving an online website (frontend/client not yet decided).

## Database

- **PostgreSQL** — relational database. Fits the entity model in [class-diagram.md](./class-diagram.md), which has clear relations (Task ↔ Project, Task ↔ Tag) rather than loosely structured documents.
- **Spring Data JPA** — data access layer on top of PostgreSQL, mapping entities (`Task`, `Project`, `Tag`) to tables.

## Build tool

- **Gradle**

## Database migration

- **Flyway** — chosen because the project only targets a single database (PostgreSQL), plain SQL migration files are more readable than Liquibase's declarative format, and the developer already knows SQL.

## Deployment

- **PaaS (Platform as a Service)** — chosen because the developer wants to focus on development for now, not ops/infrastructure management.
- **Railway** — specific PaaS chosen: deploys straight from the GitHub repo with auto-build, and provides a managed PostgreSQL add-on with no separate setup.
