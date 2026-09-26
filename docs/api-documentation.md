# REST API Documentation — To-Do List App

Based on [functional-requirements.md](./functional-requirements.md) and [class-diagram.md](./class-diagram.md).

All request/response bodies are JSON. Base path: `/api`.

## Tasks

### `GET /api/tasks`
List tasks.

**Query parameters** (all optional):
| Param | Type | Description |
|---|---|---|
| `status` | `completed` \| `incomplete` | Filter by completion status |
| `projectId` | number | Filter by project |
| `tagId` | number | Filter by tag |
| `priority` | `LOW` \| `MEDIUM` \| `HIGH` | Filter by priority |
| `search` | string | Search by text in title/description |
| `sortBy` | `dueDate` \| `priority` \| `createdAt` | Field to sort by |
| `sortDir` | `asc` \| `desc` | Sort direction (default `asc`) |

**Response** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Buy groceries",
    "description": "Milk, eggs, bread",
    "dueDate": "2026-09-30",
    "priority": "MEDIUM",
    "completed": false,
    "overdue": false,
    "createdAt": "2026-09-25T10:00:00",
    "projectId": 2,
    "tagIds": [1, 3]
  }
]
```
`overdue` is computed, read-only (`true` when `dueDate` is in the past and the task is not `completed`) — it's never sent in a request body, only returned in responses.

### `GET /api/tasks/{id}`
Get a single task.

**Response** `200 OK` — same shape as above single object.
**Response** `404 Not Found` — task does not exist.

### `POST /api/tasks`
Create a task.

**Request body**
```json
{
  "title": "Buy groceries",
  "description": "Milk, eggs, bread",
  "dueDate": "2026-09-30",
  "priority": "MEDIUM",
  "projectId": 2,
  "tagIds": [1, 3]
}
```
`title` is required. All other fields are optional.

**Response** `201 Created` — created task object.
**Response** `400 Bad Request` — `title` missing or blank.
**Response** `404 Not Found` — `projectId` given but no such project exists.

### `PUT /api/tasks/{id}`
Update a task's fields (title, description, due date, priority, project, tags).

**Request body** — same shape as `POST`.

**Response** `200 OK` — updated task object.
**Response** `400 Bad Request` — `title` missing or blank.
**Response** `404 Not Found` — task does not exist, or `projectId` given but no such project exists.

### `PATCH /api/tasks/{id}/complete`
Mark a task as complete.

**Response** `200 OK` — updated task object.

### `PATCH /api/tasks/{id}/incomplete`
Mark a task as incomplete.

**Response** `200 OK` — updated task object.

### `DELETE /api/tasks/{id}`
Delete a task.

**Response** `204 No Content`.
**Response** `404 Not Found`.

---

## Projects

### `GET /api/projects`
List all projects.

**Response** `200 OK`
```json
[
  { "id": 2, "name": "Work" }
]
```

### `GET /api/projects/{id}`
Get a single project.

**Response** `200 OK`.
**Response** `404 Not Found` — project does not exist.

### `POST /api/projects`
Create a project.

**Request body**
```json
{ "name": "Work" }
```

**Response** `201 Created`.
**Response** `400 Bad Request` — `name` missing or blank.

### `PUT /api/projects/{id}`
Rename a project.

**Response** `200 OK` — updated project object.
**Response** `400 Bad Request` — `name` missing or blank.
**Response** `404 Not Found` — project does not exist.

### `DELETE /api/projects/{id}`
Delete a project. Tasks belonging to it are unassigned (`projectId` set to `null`), not deleted.

**Response** `204 No Content`.
**Response** `404 Not Found` — project does not exist.

---

## Tags

### `GET /api/tags`
List all tags.

**Response** `200 OK`
```json
[
  { "id": 1, "name": "errand" }
]
```

### `POST /api/tags`
Create a tag.

**Request body**
```json
{ "name": "errand" }
```

**Response** `201 Created`.
**Response** `400 Bad Request` — `name` missing or blank.

### `PUT /api/tags/{id}`
Rename a tag.

**Response** `200 OK` — updated tag object.
**Response** `400 Bad Request` — `name` missing or blank.
**Response** `404 Not Found` — tag does not exist.

### `DELETE /api/tags/{id}`
Delete a tag. It is removed from any tasks it was attached to.

**Response** `204 No Content`.
**Response** `404 Not Found` — tag does not exist.

---

## Error responses

All errors follow this shape:
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Task with id 42 not found",
  "timestamp": "2026-09-25T10:00:00"
}
```

## Status

Implemented and deployed. Matches the app running on Railway.
