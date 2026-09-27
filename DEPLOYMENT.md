# Deployment Guide

This project has three deploy pieces:

- Spring Boot backend
- React frontend
- PostgreSQL database

Render is the simplest first hosting target for the backend and frontend. For PostgreSQL, use Neon so the free database does not expire after 30 days.

## Backend Service

Create a Render Web Service from this repository.

Use the repository root as the service root.

If Render offers Docker for the service runtime, choose Docker. The repository includes a `Dockerfile` that builds and runs the Spring Boot backend.

If using a non-Docker Java runtime elsewhere, use this build command:

```bash
chmod +x mvnw && ./mvnw clean package -DskipTests
```

If you are building locally on Windows instead, use:

```powershell
.\mvnw.cmd clean package -DskipTests
```

And this start command:

```bash
java -jar target/collab-backend-0.0.1-SNAPSHOT.jar
```

Environment variables:

```text
DATABASE_URL=postgresql://<user>:<password>@<neon-host>/<database>?sslmode=require
JWT_SECRET=<long-random-secret>
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=https://<frontend-domain>
```

Render provides the runtime port through `PORT`; the backend reads it automatically.

The backend also still supports traditional JDBC-style variables:

```text
DB_URL=jdbc:postgresql://<host>:<port>/<database>
DB_USERNAME=<database-user>
DB_PASSWORD=<database-password>
```

## PostgreSQL

Create a PostgreSQL database in Neon.

Use the Neon connection string for `DATABASE_URL`. The backend converts Neon/Render-style `postgresql://...` URLs into Spring Boot JDBC datasource settings at startup.

Flyway migrations run automatically when the backend starts, so the database can be empty on first deploy.

## Frontend Static Site

Create a Render Static Site from this repository.

Root directory:

```text
frontend
```

Build command:

```bash
npm install && npm run build
```

Publish directory:

```text
build
```

Environment variables:

```text
REACT_APP_API_BASE_URL=https://<backend-domain>
REACT_APP_WS_URL=https://<backend-domain>/ws
```

Important: Create React App reads `REACT_APP_*` values at build time. If you change these values later, rebuild/redeploy the frontend.

## Local Run

Backend:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/collabdb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_postgres_password"
$env:JWT_SECRET="replace-with-a-long-random-secret-at-least-32-characters"
$env:CORS_ALLOWED_ORIGINS="http://localhost:3000"
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm start
```
