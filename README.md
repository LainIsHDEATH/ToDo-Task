# ToDo Tasks Backend

Backend API for a ToDo task management application.

## Tech Stack

* Java 21
* Spring Boot 3.5.14
* Spring Web
* Spring Security
* Spring Data JPA
* Spring Modulith
* PostgreSQL
* Liquibase
* MapStruct
* Maven
* Springdoc OpenAPI / Swagger UI

---

## DB Schema

![db\_schema.png](db_schema.png)

---

## Backend Setup

### Prerequisites

Install:

* Java 21
* Maven
* PostgreSQL 16 or Docker

---

### Start PostgreSQL with Docker

The default development profile expects PostgreSQL on port `5431`.

```bash
docker run --name todo-tasks-postgres \
  -e POSTGRES_DB=todo_tasks_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5431:5432 \
  -d postgres:16
```

or

```bash
docker compose up -d 
```

---

### Run the application

Use the `dev` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend starts on:

```text
http://localhost:8080
```

---

### Run tests

```bash
mvn test
```

---

### Build project

```bash
mvn clean package
```

---

## Configuration

The default `dev` profile uses these environment variables:

| Variable                 | Default value                                    |
| ------------------------ | ------------------------------------------------ |
| `DB_URL`                 | `jdbc:postgresql://localhost:5431/todo_tasks_db` |
| `DB_USERNAME`            | `postgres`                                       |
| `DB_PASSWORD`            | `postgres`                                       |
| `JWT_SECRET`             | `MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=`   |
| `JWT_EXPIRATION_MINUTES` | `60`                                             |
| `CORS_ALLOWED_ORIGINS`   | `http://localhost:3000,http://localhost:5173`    |

Example:

```bash
DB_URL=jdbc:postgresql://localhost:5431/todo_tasks_db \
DB_USERNAME=postgres \
DB_PASSWORD=postgres \
JWT_SECRET=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY= \
JWT_EXPIRATION_MINUTES=60 \
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## API Documentation

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

---

## Authentication

Public endpoints:

| Method | Endpoint             | Description                 |
| ------ | -------------------- | --------------------------- |
| `POST` | `/api/auth/register` | Register new user           |
| `POST` | `/api/auth/login`    | Login and receive JWT token |

All other user-facing endpoints require a Bearer token:

```http
Authorization: Bearer <accessToken>
```

Admin endpoints require `ADMIN` role.

Registered users are created with `USER` role by default.

---

## Pagination and Sorting

Paged endpoints return `PageResponse<T>`:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Common query parameters:

```text
?page=0&size=20&sort=id,asc
```

Allowed sort fields:

| Resource     | Allowed sort fields                            |
| ------------ | ---------------------------------------------- |
| User catalog | `id`, `firstName`, `lastName`, `email`         |
| Admin users  | `id`, `firstName`, `lastName`, `email`, `role` |
| Tasks        | `id`, `name`, `priority`, `status`             |

---

# Main Endpoints

## Auth API

### Register user

```http
POST /api/auth/register
```

Request:

```json
{
  "firstName": "Ivan",
  "lastName": "Shankin",
  "email": "ivan@example.com",
  "password": "password123"
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "firstName": "Ivan",
  "lastName": "Shankin",
  "email": "ivan@example.com",
  "role": "USER"
}
```

---

### Login

```http
POST /api/auth/login
```

Request:

```json
{
  "email": "ivan@example.com",
  "password": "password123"
}
```

Response `200 OK`:

```json
{
  "accessToken": "jwt-token-value",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600
}
```

---

## User API

User-facing endpoints are available for authenticated `USER` and `ADMIN` users.

### Get user catalog

```http
GET /api/users
```

Returns public user data for collaborator selection.

Response `200 OK`:

```json
{
  "content": [
    {
      "id": 1,
      "firstName": "Ivan",
      "lastName": "Shankin",
      "email": "ivan@example.com"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### Get current user profile

```http
GET /api/users/me
```

Response `200 OK`:

```json
{
  "id": 1,
  "firstName": "Ivan",
  "lastName": "Shankin",
  "email": "ivan@example.com",
  "role": "USER"
}
```

---

### Update current user profile

```http
PUT /api/users/me
```

Request:

```json
{
  "firstName": "Ivan",
  "lastName": "Updated",
  "email": "ivan.updated@example.com"
}
```

Response `200 OK`:

```json
{
  "id": 1,
  "firstName": "Ivan",
  "lastName": "Updated",
  "email": "ivan.updated@example.com",
  "role": "USER"
}
```

---

### Delete current user profile

```http
DELETE /api/users/me
```

Response:

```text
204 No Content
```

---

### Get public user by id

```http
GET /api/users/{id}
```

Response `200 OK`:

```json
{
  "id": 1,
  "firstName": "Ivan",
  "lastName": "Shankin",
  "email": "ivan@example.com"
}
```

---

## Current User Task API

Current-user task endpoints are available for authenticated `USER` and `ADMIN` users.

### Get current user's tasks

```http
GET /api/tasks
```

Response `200 OK`:

```json
{
  "content": [
    {
      "id": 1,
      "name": "Prepare report",
      "priority": "HIGH",
      "status": "TODO"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### Create current user's task

```http
POST /api/tasks
```

Request:

```json
{
  "name": "Prepare report",
  "priority": "HIGH",
  "collaboratorIds": [2, 3]
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "name": "Prepare report",
  "priority": "HIGH",
  "status": "TODO",
  "owner": {
    "id": 1,
    "firstName": "Ivan",
    "lastName": "Shankin",
    "email": "ivan@example.com"
  },
  "collaborators": [
    {
      "id": 2,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com"
    }
  ]
}
```

---

### Get current user's task by id

```http
GET /api/tasks/{taskId}
```

Response `200 OK`:

```json
{
  "id": 1,
  "name": "Prepare report",
  "priority": "HIGH",
  "status": "TODO",
  "owner": {
    "id": 1,
    "firstName": "Ivan",
    "lastName": "Shankin",
    "email": "ivan@example.com"
  },
  "collaborators": []
}
```

---

### Update current user's task

```http
PUT /api/tasks/{taskId}
```

Request:

```json
{
  "name": "Prepare final report",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS",
  "collaboratorIds": [2]
}
```

Response `200 OK`:

```json
{
  "id": 1,
  "name": "Prepare final report",
  "priority": "MEDIUM",
  "status": "IN_PROGRESS",
  "owner": {
    "id": 1,
    "firstName": "Ivan",
    "lastName": "Shankin",
    "email": "ivan@example.com"
  },
  "collaborators": [
    {
      "id": 2,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com"
    }
  ]
}
```

---

### Delete current user's task

```http
DELETE /api/tasks/{taskId}
```

Response:

```text
204 No Content
```

---

## Admin User API

Admin user endpoints require `ADMIN` role.

### Get all users

```http
GET /api/admin/users
```

Response `200 OK`:

```json
{
  "content": [
    {
      "id": 1,
      "firstName": "Ivan",
      "lastName": "Shankin",
      "email": "ivan@example.com",
      "role": "USER"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### Get user by id

```http
GET /api/admin/users/{id}
```

Response `200 OK`:

```json
{
  "id": 1,
  "firstName": "Ivan",
  "lastName": "Shankin",
  "email": "ivan@example.com",
  "role": "USER"
}
```

---

### Update user

```http
PUT /api/admin/users/{id}
```

Request:

```json
{
  "firstName": "Ivan",
  "lastName": "Admin Updated",
  "email": "ivan.admin@example.com",
  "role": "ADMIN"
}
```

Response `200 OK`:

```json
{
  "id": 1,
  "firstName": "Ivan",
  "lastName": "Admin Updated",
  "email": "ivan.admin@example.com",
  "role": "ADMIN"
}
```

---

### Delete user

```http
DELETE /api/admin/users/{id}
```

Response:

```text
204 No Content
```

---

## Admin Task API

Admin task endpoints require `ADMIN` role.

### Get all tasks

```http
GET /api/admin/tasks
```

Response `200 OK`:

```json
{
  "content": [
    {
      "id": 1,
      "name": "Prepare report",
      "priority": "HIGH",
      "status": "TODO"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### Get tasks by owner

```http
GET /api/admin/users/{userId}/tasks
```

Response `200 OK`:

```json
{
  "content": [
    {
      "id": 1,
      "name": "Prepare report",
      "priority": "HIGH",
      "status": "TODO"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### Create task for user

```http
POST /api/admin/users/{userId}/tasks
```

Request:

```json
{
  "name": "Admin-created task",
  "priority": "LOW",
  "collaboratorIds": [2, 3]
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "name": "Admin-created task",
  "priority": "LOW",
  "status": "TODO",
  "owner": {
    "id": 1,
    "firstName": "Ivan",
    "lastName": "Shankin",
    "email": "ivan@example.com"
  },
  "collaborators": [
    {
      "id": 2,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com"
    }
  ]
}
```

---

### Get task by id

```http
GET /api/admin/tasks/{taskId}
```

Response `200 OK`:

```json
{
  "id": 1,
  "name": "Admin-created task",
  "priority": "LOW",
  "status": "TODO",
  "owner": {
    "id": 1,
    "firstName": "Ivan",
    "lastName": "Shankin",
    "email": "ivan@example.com"
  },
  "collaborators": []
}
```

---

### Update task

```http
PUT /api/admin/tasks/{taskId}
```

Request:

```json
{
  "name": "Updated admin task",
  "priority": "MEDIUM",
  "status": "DONE",
  "collaboratorIds": [2]
}
```

Response `200 OK`:

```json
{
  "id": 1,
  "name": "Updated admin task",
  "priority": "MEDIUM",
  "status": "DONE",
  "owner": {
    "id": 1,
    "firstName": "Ivan",
    "lastName": "Shankin",
    "email": "ivan@example.com"
  },
  "collaborators": [
    {
      "id": 2,
      "firstName": "John",
      "lastName": "Doe",
      "email": "john@example.com"
    }
  ]
}
```

---

### Delete task

```http
DELETE /api/admin/tasks/{taskId}
```

Response:

```text
204 No Content
```

---