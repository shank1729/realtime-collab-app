# Realtime Collab App

A full-stack real-time collaborative document editor with a Spring Boot backend and React frontend.

The backend lives at the repository root. The React frontend lives in [`frontend/`](frontend/).

## Full-Stack Features

- JWT authentication for registered users
- Document CRUD APIs
- Role-based document sharing with Owner, Editor, and Viewer access
- STOMP-over-WebSocket live document editing
- Active collaborator presence over WebSocket
- Document version history
- PostgreSQL persistence with Flyway migrations

## Running Locally

Start PostgreSQL with a `collabdb` database, then run the backend:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/collabdb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_postgres_password"
.\mvnw spring-boot:run
```

In another terminal, run the frontend:

```powershell
cd frontend
npm install
npm start
```

The frontend defaults to:

```text
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_WS_URL=http://localhost:8080/ws
```

Create a local `frontend/.env` only if you need to override those values.

## Backend

`collab-backend` is a Spring Boot service for a simple real-time collaborative document editor. It provides:

- REST APIs to create, read, update, patch, and delete documents
- PostgreSQL persistence through Spring Data JPA
- STOMP-over-WebSocket messaging for broadcasting live document edits

The codebase is intentionally small, which makes it a good starter backend for a collaborative editing app or a learning project for Spring Boot + WebSocket integration.

## Tech Stack

- Java 17
- Spring Boot 3.5.5
- Spring Web
- Spring Data JPA
- Spring Security
- Spring WebSocket with STOMP + SockJS
- PostgreSQL
- Flyway database migrations
- Maven

## Project Structure

```text
src/
  main/
    java/com/realtimecollab/
      CollabBackendApplication.java        Application entry point
      SecurityConfig.java                  Security configuration
      config/WebSocketConfig.java          STOMP/WebSocket setup
      controller/DocumentController.java   REST API for documents
      controller/DocumentSocketController.java
                                          WebSocket message handler
      entity/Document.java                 JPA entity
      repository/DocumentRepository.java   Data access layer
      service/DocumentService.java         Business logic
    resources/
      application.properties               App and database configuration
      db/migration/                        Versioned database migrations
  test/
    java/com/realtimecollab/collab_backend/
      CollabBackendApplicationTests.java   Basic Spring context test
```

## How It Works

The backend has two ways to interact with documents:

### 1. REST API

The REST API is used for standard CRUD operations:

- list all documents
- fetch a single document
- create a document
- fully update a document
- partially update a document
- delete a document

### 2. WebSocket Messaging

Clients can also connect over WebSocket and send edit events. When a client publishes an updated document payload to the application destination, the backend:

1. updates the stored document content in PostgreSQL
2. broadcasts the update to all subscribers on a topic

This gives connected clients a simple real-time sync channel.

## Data Model

The application stores three main entities: `User`, `Document`, and `DocumentVersion`.

| Field | Type | Notes |
| --- | --- | --- |
| `id` | `Long` | Primary key, auto-generated |
| `title` | `String` | Document title |
| `content` | `String` | Stored as PostgreSQL `TEXT` |
| `createdAt` | `LocalDateTime` | Set automatically on insert |
| `updatedAt` | `LocalDateTime` | Updated automatically on insert and update |

Entity lifecycle hooks are used to manage timestamps:

- `@PrePersist` sets `createdAt` and `updatedAt`
- `@PreUpdate` refreshes `updatedAt`

Documents also support role-based sharing:

| Role | Permissions |
| --- | --- |
| Owner | Read, edit, delete, and share the document |
| Editor | Read and edit the shared document |
| Viewer | Read the shared document only |

Sharing a document with an existing collaborator updates that collaborator's role instead of creating a duplicate access row.

## Architecture Flow

For REST requests:

`DocumentController -> DocumentService -> DocumentRepository -> PostgreSQL`

For WebSocket document edits:

`WebSocket client -> /app/editDocument -> DocumentSocketController -> DocumentService -> PostgreSQL -> /topic/documents/{documentId}`

## Configuration

The runtime configuration currently lives in [`src/main/resources/application.properties`](src/main/resources/application.properties).

Current settings:

- application name: `collab-backend`
- PostgreSQL URL: `DB_URL`, defaulting to `jdbc:postgresql://localhost:5432/collabdb`
- username: `DB_USERNAME`, defaulting to `postgres`
- password: `DB_PASSWORD`, defaulting to `postgres` for local development
- JWT secret: `JWT_SECRET`, with a local development fallback
- JWT expiry: `JWT_EXPIRATION_MS`, defaulting to 24 hours
- schema migrations: Flyway
- Hibernate DDL mode: `validate`
- SQL logging: enabled

### Local Environment Variables

On Windows PowerShell, you can run the backend with your local database credentials like this:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/collabdb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your-local-postgres-password"
$env:JWT_SECRET="use-a-long-random-secret-for-local-development"
.\mvnw.cmd spring-boot:run
```

## Database Migrations

Schema changes are managed with Flyway. Migration files live in:

```text
src/main/resources/db/migration
```

The first migration, `V1__initialize_collaboration_schema.sql`, creates the core tables:

- `users`
- `document`
- `document_access`
- `document_version`

Flyway runs before Hibernate starts. Hibernate is configured with `ddl-auto=validate`, so it checks that the database schema matches the JPA entities but does not silently create or modify tables.

## Running the Project

### Prerequisites

- Java 17 installed
- Maven available, or use the Maven wrapper included in the repository
- PostgreSQL running locally
- A database named `collabdb`

### Start the application

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

By default, Spring Boot serves the app on:

```text
http://localhost:8080
```

## REST API

Base path:

```text
/documents
```

### 1. Get all documents

```http
GET /documents
```

Response:

```json
[
  {
    "id": 1,
    "title": "Project Notes",
    "content": "Initial draft",
    "createdAt": "2026-04-06T10:00:00",
    "updatedAt": "2026-04-06T10:00:00"
  }
]
```

### 2. Get one document

```http
GET /documents/{id}
```

Example:

```http
GET /documents/1
```

### 3. Create a document

```http
POST /documents
Content-Type: application/json
```

Request body:

```json
{
  "title": "Project Notes",
  "content": "Initial draft"
}
```

### 4. Replace a document

```http
PUT /documents/{id}
Content-Type: application/json
```

Request body:

```json
{
  "title": "Updated Title",
  "content": "Full replacement content"
}
```

### 5. Partially update a document

```http
PATCH /documents/{id}
Content-Type: application/json
```

You can send only the fields you want to change:

```json
{
  "content": "Only this field changes"
}
```

### 6. Delete a document

```http
DELETE /documents/{id}
```

## WebSocket API

The application uses Spring's STOMP message broker with SockJS support.

### Connection endpoint

```text
/ws
```

The endpoint is registered with SockJS enabled and currently allows all origins.

### Broker configuration

- application destination prefix: `/app`
- subscription topic prefix: `/topic`

### Send edits

Clients send document edit messages to:

```text
/app/editDocument
```

The payload should match the `Document` structure closely enough for the backend to read `id` and `content`:

```json
{
  "id": 1,
  "content": "Live edited text"
}
```

The STOMP connection should include the same JWT used for REST calls:

```text
Authorization: Bearer <jwt>
```

When the backend receives this message, it derives the editor from the authenticated WebSocket session, updates the stored document content, and publishes the saved update to:

```text
/topic/documents/{documentId}
```

### Presence

Clients can announce that they are actively viewing or typing in a document:

```text
/app/documents/{documentId}/presence/join
/app/documents/{documentId}/presence/typing
/app/documents/{documentId}/presence/viewing
```

Presence updates are broadcast to:

```text
/topic/documents/{documentId}/presence
```

The presence payload contains the active users for that document, including their name, email, status, join time, and last-seen time. Presence is kept in memory because it is runtime state, not permanent document data.

### Example frontend flow

1. Connect a SockJS/STOMP client to `/ws`
2. Subscribe to `/topic/documents/{documentId}`
3. Subscribe to `/topic/documents/{documentId}/presence`
3. Send edit events to `/app/editDocument`
4. Send presence events to `/app/documents/{documentId}/presence/join`
5. Apply incoming broadcasts to keep clients in sync

## Security

Security is configured in [`src/main/java/com/realtimecollab/SecurityConfig.java`](src/main/java/com/realtimecollab/SecurityConfig.java).

Current behavior:

- CSRF is disabled
- `/auth/**`, `/ws/**`, Swagger, and basic actuator health/info are public
- document APIs require a valid JWT bearer token
- passwords are stored as BCrypt hashes
- the authenticated user's email is used to enforce document access rules
- document roles separate read-only collaborators from editors

WebSocket edits also derive the editor from the authenticated connection instead of trusting an email in the message body.

## Persistence Layer

[`DocumentRepository`](src/main/java/com/realtimecollab/repository/DocumentRepository.java) extends `JpaRepository<Document, Long>`, so the app inherits standard CRUD database operations from Spring Data JPA.

Most business logic lives in [`DocumentService`](src/main/java/com/realtimecollab/service/DocumentService.java), including:

- full update behavior
- partial update behavior
- content-only update for WebSocket edits
- delete behavior

## Testing

Run tests with:

```powershell
.\mvnw.cmd test
```

Tests run with the `test` Spring profile and use an in-memory H2 database, so they do not depend on your local PostgreSQL password or data. Flyway migrations run against H2 during tests too, which verifies that the schema scripts can boot the application from an empty database.

The backend includes service-level integration tests for:

- registration, login, password hashing, and duplicate email rejection
- document ownership and access rules
- sharing rules
- editor/viewer role permissions
- version history snapshots
- in-memory document presence state

The backend also includes controller/API integration tests for:

- auth endpoint status codes and JSON responses
- request validation errors
- authenticated document creation
- role-based document access over HTTP
- sharing, updating, deleting, and history endpoints

## Observability

The backend now includes Spring Boot Actuator for runtime visibility.

Useful endpoints:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

Notes:

- `health` and `info` are public for local/dev diagnostics
- `metrics` and `prometheus` still require authentication under the current security setup
- the Maven build now generates build metadata for the `info` endpoint

## Current Limitations

Based on the current implementation:

- WebSocket updates only persist `content`, even if other fields are present
- conflict resolution and operational transforms/CRDT logic are not implemented
- automated test coverage is still minimal

## Suggested Next Improvements

- add typing timeout handling
- add integration tests for REST and WebSocket flows
- add Docker Compose for backend, frontend, and PostgreSQL

## Useful Commands

Build the project:

```powershell
.\mvnw.cmd clean package
```

Run the test suite:

```powershell
.\mvnw.cmd test
```

Start the app:

```powershell
.\mvnw.cmd spring-boot:run
```

## Summary

This backend is a straightforward real-time document service:

- PostgreSQL stores the source of truth
- REST handles CRUD
- WebSocket broadcasts live edits

If you are pairing this backend with a frontend editor, the main integration points are:

- REST at `/documents`
- WebSocket handshake at `/ws`
- STOMP send destination `/app/editDocument`
- STOMP subscription topic `/topic/documents/{documentId}`
- presence subscription topic `/topic/documents/{documentId}/presence`
