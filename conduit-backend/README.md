# conduit-backend

Spring Boot 4 implementation of the RealWorld Conduit API 2.0.0.

## Run

    docker compose up -d
    mvn spring-boot:run

- API base: http://localhost:8080/api/v1
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Set JWT_SECRET (at least 32 bytes) and DB_URL, DB_USERNAME, DB_PASSWORD for non-local environments.

## Modules

- identity: users, authentication, profiles, follows
- article: articles, tags, favorites
- comment: article comments

Each module has domain, application, infrastructure, presentation and contract packages.
Modules talk to each other only through contract packages and events.
