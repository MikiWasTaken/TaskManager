# Task Manager API

A REST API backend for project and task management. It supports multiple users working across multiple projects, with role-based access control, file attachments, and actions logging.


## How to run

```postman_collection.json``` contains examples of the possible requests in this project.

### Docker
- Make sure you have docker installed first
- Run ```docker compose up --build```


### Native

#### Prerequisites
Make sure the following are installed before you begin:
- [Java 21](https://adoptium.net/)
- [Maven 3.9+](https://maven.apache.org/download.cgi) (or use the `./mvnw` wrapper included in the project)
- [PostgreSQL 14+](https://www.postgresql.org/download/)
- [MinIO](https://min.io/download)
- [Git](https://git-scm.com/)

#### 1. Clone the repository

```bash
git clone <your-repository-url>
cd taskmanager
```

#### 2. Set up PostgreSQL

Open the PostgreSQL interactive terminal (psql) and create the database:

```sql
CREATE DATABASE taskmanager;
```

The application expects the default username `postgres` and password `postgres` on port `5432`. If your setup differs, update `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskmanager
spring.datasource.username=your_username
spring.datasource.password=your_password
```

#### 3. Set up MinIO

Start the MinIO server, pointing it at a local folder where it will store files:

```bash
# macOS / Linux
minio server ~/minio-data --console-address ":9001"

# Windows
minio.exe server C:\minio-data --console-address ":9001"
```

MinIO will start on port `9000` with the default credentials `minioadmin` / `minioadmin`. Leave this terminal running.

#### 4. Run the application

In a new terminal, from the project root:

```bash
./mvnw spring-boot:run
```

Or if you have Maven installed globally:

```bash
mvn spring-boot:run
```

On first startup, Flyway will automatically create all database tables and load demo data. The application will be available at **`http://localhost:8080`**.



## What it does

### Authentication
Authentication is done using JWT tokens.

### User 
 - Register 
 - Login
 - View own profile
 - Update own profile (including password change, if they provide their current password)
 - View the projects they are part of
 - Deactivate themselves (but they will lock themselves out)

### Admin
- View all users/projects/tasks in the system (including soft deleted ones)
- Modify user roles (if they are the only admin, they cannot set themselves to basic user)
- Deactivate users  (if they are the only admin, they cannot deactivate themselves)
- Update user profiles (but they cannot change other user's passwords)
- View audit logs

### Projects 
- Create project - the user that creates it automatically becomes the owner and the first member
- Update project - name, description, and project status (can only be done by the owner or the admin)
- Delete project (soft) - can only be done by the owner or the admin
- Add members - users can only invite members to a project if they are a member themselves first (or admin)
- Change ownership - can only be done by current owner/admin. The new owner must be an existing member of the project
- Projects marked with status "COMPLETE" cannot be updated, and don't support adding or removing members. To make modifications 
available again, the owner or admin must do an update where they specify the status of the project as "ACTIVE"

### Tasks
- View tasks - they must be a member (or admin) of the project they are viewing the tasks on (the admin can see the deleted tasks as well)
- Filter by status, priority, creation date and deadline date
- Create tasks - they must be a member (or admin) of the project they are creating the task on
- Update tasks - (name, description, status, priority, assignee). Any member (or admin) of the project can modify tasks.
An assignee must be a member of the project first
- Delete tasks (soft) - Any member (or admin) of the project can delete tasks
- Updating, adding or deleting tasks are only available on projects that are not marked as "COMPLETE"

### Documents
- Upload, view, download and delete files
- All these actions are permitted only to members (or admins) of the project the document is in


### Audit logs
- all actions are recorded and can be viewed by the admin
- 
- every significant action (login, register, document upload/delete, project delete) is recorded in the database and
can be viewed by the admin.

  
## Tech stack
- Language: Java 21
- Framework: Spring Boot 3.4
- Security: Spring Security 6 + JWT
- PostgreSQL + SpringData JPA + Hibernate
- Migrations: Flyway
- Object storage: MinIO
- Build tool: Maven



---

## Seed data

### Users

All passwords are: **`password`**

| Email | Role | Status | Notes |
|---|---|---|---|
| `alice@example.com` | ADMIN | Active | Admin user, owns projects Apollo and Gamma |
| `bob@example.com` | USER | Active | Member of Apollo and Beta, owns Beta |
| `carol@example.com` | USER | Active | Member of Apollo and Beta |
| `dave@example.com` | USER | Active | Not a member of any project (outsider) |
| `eve@example.com` | USER | **Inactive** | Login returns 401 |

### Projects

| Name | Status | Owner | Members |
|---|---|---|---|
| Apollo | ACTIVE | Alice | Alice, Bob, Carol |
| Beta | COMPLETE | Bob | Bob, Carol |
| Gamma | ACTIVE (soft-deleted) | Alice | Alice |

---

## API overview
---

## Authentication

### Register a new user

```http
POST /api/auth/register
```

```json
{
  "email": "frank@example.com",
  "password": "password123",
  "firstName": "Frank",
  "lastName": "Test"
}
```

---

### Login

Returns a JWT — use the token in all subsequent requests.

```http
POST /api/auth/login
```

**Admin user:**

```json
{
  "email": "alice@example.com",
  "password": "password"
}
```

**Regular users (Apollo + Beta members):**

```json
{ "email": "bob@example.com",   "password": "password" }
```
```json
{ "email": "carol@example.com", "password": "password" }
```

---

### Error cases

| Scenario               | Request                                           | Response | Message                                |
| ---------------------- | ------------------------------------------------- | -------- | -------------------------------------- |
| Wrong password         | `POST /api/auth/login` with `"password": "wrong"` | `400`    | Invalid credentials                    |
| Inactive account (Eve) | `POST /api/auth/login` as `eve@example.com`       | `403`    | This account is deactivated            |
| No token provided      | Any protected endpoint                            | `401`    | Authentication required, please log in |

---

## Users

### View own profile

```http
GET /api/users/me
```

> Any authenticated user.

---

### Update own profile

```http
PATCH /api/users/me
```

> Supports updating name or password (to change the password, you must provide the current one)

```json
{"firstName": "Bobby",
 "currentPassword": "password",
 "newPassword": "newPassword"}
```

---

### List own projects

```http
GET /api/users/me/projects
```

---

### `ADMIN` List all users

```http
GET /api/users
```

---

### `ADMIN` Update another user's role or active status

```http
PATCH /api/users/4ef21d5e-17d4-4b0b-be0d-c5888b19aacc
```

```json
{ "role": "ADMIN" }
```

```json
{ "isActive": false }
```

---

### Error cases

| Scenario                  | Request                 | Response | Message       |
| ------------------------- | ----------------------- | -------- | ------------- |
| Non-admin lists all users | `GET /api/users` as Bob | `403`    | Access denied |

---

## Projects

### `ADMIN` List all projects

Includes soft-deleted projects (e.g. Gamma).

```http
GET /api/projects
```

---

### Get project by ID

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970
```

> Apollo project — requires membership (e.g. Bob).

---

### Create a project

```http
POST /api/projects
```

```json
{ "name": "Delta", "description": "New project" }
```

---

### Update a project

```http
PATCH /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970
```

```json
{ "description": "Updated description" }
```

---

### Soft-delete a project

```http
DELETE /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970
```

> Admin or project owner only.

---

### Error cases

| Scenario                         | Request                                 | Response |                                                                                                     |
| -------------------------------- | --------------------------------------- | -------- | --------------------------------------------------------------------------------------------------- |
| Non-admin lists all projects     | `GET /api/projects` as Bob              | `403`    | Access denied                                                                                       |
| Outsider (Dave) accesses Apollo  | `GET /api/projects/7be1ce72...` as Dave | `403`    | Unauthorized access                                                                                 |
| Update a COMPLETE project (Beta) | `PATCH /api/projects/3360799c...`       | `400`    | You cannot update completed projects. If you're the owner/admin, switch the project to active first |

---

## Project Members

### List members of Apollo

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/members
```

---

### Add a member

```http
POST /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/members/7b0bd519-2b30-41a3-b7cf-3ed18f01959b
```

> Adds Dave (outsider) to Apollo.

---

### Remove a member

```http
DELETE /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/members/7b0bd519-2b30-41a3-b7cf-3ed18f01959b
```

---

### Error cases

| Scenario                              | Request                                              | Response | Message                                            |
| ------------------------------------- | ---------------------------------------------------- | -------- | -------------------------------------------------- |
| Add member to COMPLETE project (Beta) | `POST /api/projects/3360799c.../members/7b0bd519...` | `400`    | You are not allowed to add members to this project |

---

## Tasks

### List all tasks in Apollo

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks
```

**Filter by status:**

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks?taskStatus=TODO
```

**Filter by priority:**

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks?taskPriority=HIGH
```

**Filter by both:**

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks?taskStatus=TODO&taskPriority=HIGH
```

---

### Get a single task

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks/eb5a0b7b-bc12-4fa2-842d-0abd8189c058
```

---

### `ADMIN` Get a soft-deleted task

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks/7c3f1525-4049-4a3a-b93b-e55a30b2ee01
```

---

### `ADMIN` Get a task in a deleted project (Gamma)

```http
GET /api/projects/0a5a7f17-c24d-49af-84aa-bb933c1fd799/tasks/faa26561-cb3d-4190-a479-748950bb770f
```

---

### Create a task

```http
POST /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks
```

```json
{
  "title": "Write API docs",
  "priority": "LOW",
  "status": "TODO",
  "assigneeId": "4c0f7f22-fedc-44f1-afb6-75a469ba45a9"
}
```

> Assigned to Carol (a project member).

---

### Update a task

```http
PATCH /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks/eb5a0b7b-bc12-4fa2-842d-0abd8189c058
```

```json
{ "status": "IN_PROGRESS" }
```

> Supports updating status, priority, or reassigning.

---

### Soft-delete a task

```http
DELETE /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/tasks/eb5a0b7b-bc12-4fa2-842d-0abd8189c058
```

---

### Error cases

| Scenario                               | Request                                             | Response | Message                                              |
| -------------------------------------- | --------------------------------------------------- | -------- | ---------------------------------------------------- |
| Outsider (Dave) lists tasks            | `GET /api/projects/7be1ce72.../tasks` as Dave       | `403`    | You are not allowed to view tasks in this project    |
| Assign task to non-member (Dave)       | `POST` with `"assigneeId": "7b0bd519..."`           | `403`    | Only members of the project can be assigned to tasks |
| Create task in COMPLETE project (Beta) | `POST /api/projects/3360799c.../tasks`              | `400`    | Cannot add tasks to completed projects               |
| Update task in COMPLETE project (Beta) | `PATCH /api/projects/3360799c.../tasks/be78caa8...` | `400`    | Cannot update tasks in completed projects            |

---

## Documents

### List documents in Apollo

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/documents
```

---

### Upload a file

```http
POST /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/documents
Content-Type: multipart/form-data
```

```
file=@yourfile.pdf
```

---

### Download a document

```http
GET /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/documents/{documentId}/download
```

> Note: seed documents have no real file stored in MinIO.

---

### Delete a document

```http
DELETE /api/projects/7be1ce72-9da1-435c-84ba-d61d9e732970/documents/{documentId}
```

---

## Admin — Audit Logs

### `ADMIN` List all audit log entries

```http
GET /api/admin/audit-logs
```

---

### Error cases

| Scenario                      | Request                            | Response      |
| ----------------------------- | ---------------------------------- | ------------- |
| Non-admin accesses audit logs | `GET /api/admin/audit-logs` as Bob | Access denied |