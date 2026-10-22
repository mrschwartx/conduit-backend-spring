# Conduit Backend (Spring Boot)

Spring Boot 4 / Java 25 implementation of the Conduit (RealWorld) API. The contract is
[`conduit-openapi/openapi.yml`](conduit-openapi/openapi.yml); everything is served under
`/api/v1`, plus `GET /health` outside the prefix. See [NOTES.md](NOTES.md) for contract
ambiguities, design decisions and the current verification status.

## Prerequisites

- JDK 25
- Docker (runs PostgreSQL for development, and Testcontainers for the integration tests)
- Maven is optional: the project ships the Maven wrapper (`./mvnw`)

## 1. Start the database

```sh
docker compose -f conduit-docker/docker-compose.dev.yml up -d conduit-postgres
```

This gives PostgreSQL 18 on `localhost:5432` (database `conduit_dev`, user `dev_user`,
password `dev_pass`), which are also the application's development defaults.
The schema is created by Flyway migrations on startup (`src/main/resources/db/migration`).

## 2. Run the app

```sh
cd conduit-backend-spring
export JWT_SECRET="$(openssl rand -base64 48)"   # required, at least 32 characters
./mvnw spring-boot:run
```

The app listens on port 8080:

- API: `http://localhost:8080/api/v1`
- Health: `http://localhost:8080/health` (plain text `OK`)
- Swagger UI: `http://localhost:8080/swagger-ui.html` (a viewer for the real `openapi.yml`,
  not a second contract)

It refuses to start when `JWT_SECRET` is missing or shorter than 32 characters.

## 3. Run the tests

```sh
cd conduit-backend-spring
./mvnw test
```

Unit tests (slug generation, validation rules, JWT, pagination) need nothing. Integration
tests start a real PostgreSQL through Testcontainers, so Docker must be running.

## Configuration (environment variables)

Copy `conduit-backend-spring/.env.example` as a starting point; never commit a real `.env`.

| Variable               | Default                 | Meaning                                              |
|------------------------|-------------------------|------------------------------------------------------|
| `PORT`                 | `8080`                  | HTTP port                                            |
| `DATABASE_HOST`        | `localhost`             | PostgreSQL host                                      |
| `DATABASE_PORT`        | `5432`                  | PostgreSQL port                                      |
| `DATABASE_NAME`        | `conduit_dev`           | Database name                                        |
| `DATABASE_USER`        | `dev_user`              | Database user                                        |
| `DATABASE_PASSWORD`    | `dev_pass`              | Database password                                    |
| `JWT_SECRET`           | none (**required**)     | HS256 secret, minimum 32 characters                  |
| `JWT_TTL_MINUTES`      | `720`                   | Token lifetime                                       |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated list of allowed origins              |
| `LOG_LEVEL`            | `info`                  | Root log level                                       |

## Conventions worth knowing

- Authentication header is `Authorization: Token <jwt>` (not `Bearer`).
- Every error response is `{"errors": {"<field>": ["message", ...]}}`.
- Logs are JSON on stdout; every line carries the request id (`X-Request-Id` is accepted or
  generated, and echoed in the response). Passwords, hashes and tokens are never logged.
- Contract tests (if you have `conduit-contract-tests` checked out):
  `API_URL=http://localhost:8080/api/v1` against a running instance.
