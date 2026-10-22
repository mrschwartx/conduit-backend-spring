# Notes

## Verification status (read this first)

The comments feature, the comments tests, this file and the README were written in a
sandbox where **Maven Central was blocked by the network policy** (HTTP 403 from the egress
proxy) and only JDK 21 and no Docker daemon were available. Nothing in the project could be
compiled or run there, so **the last commit has not been compiled and no test has been run**,
and I could not confirm that the earlier features were compiled in a build either.
Before relying on any of it, run on a machine with JDK 25, Docker and network access:

```sh
cd conduit-backend-spring && ./mvnw verify
```

Expect to fix small compile or Boot 4 API issues. If `conduit-contract-tests` is available,
run it against the running app as well; it has not been run.

## Contract ambiguities and defects in `openapi.yml`

`openapi.yml` was not edited. Assumption taken for each:

1. **Auth level of `GET /articles/{slug}`.** The spec says "Auth not required" and declares no
   401; the prompt says OPTIONAL (anonymous allowed, invalid token -> 401). Assumed OPTIONAL,
   because `favorited` and `following` depend on the caller.
2. **`409` on `PUT /user`.** Only `POST /users` and `POST /articles` declare 409, but changing
   email/username can collide. 409 is returned (per the prompt).
3. **Undeclared statuses.** 400 (malformed JSON), 413 (body over 1 MB), 415 and 500 are not in
   the spec. They are emitted in the same `{"errors": ...}` shape. 413 uses
   `errors.body = ["is too large"]` (not specified anywhere).
4. **Validation limits are not in the schemas.** No `minLength`/`maxLength`, no password
   minimum (also for `UpdateUser.password`), and `limit` has no maximum or clamp. The prompt's
   limits are used (password 8-72, limit clamped to 50, ...).
5. **`offset` has no default** in the spec; 0 is used.
6. **422 is declared on operations with nothing to validate** (`GET /user`, `GET /tags`, ...).
   It is only emitted where input exists.
7. **Non-integer comment id** (`DELETE .../comments/abc`): the spec types the id as integer
   but names no error key. Returns 422 with `errors.id = ["is invalid"]`, after the article
   lookup (a missing article is 404 first).
8. **Unknown paths and wrong methods.** The contract defines no shape for these. 404 uses
   `errors.path = ["not found"]`, 405 uses `errors.method = ["not allowed"]`.
9. **`NotFound`/`Forbidden` examples use the literal key `resource`.** The real keys are used
   (`article`, `comment`, `profile`), as the response descriptions say.
10. **`servers` includes `/api/v1`** while path keys do not, and `/health` is not in the spec.
    Paths are served under `/api/v1`; `/health` lives outside it.

## Design decisions

- **Prefix.** `/api/v1` is a constant (`Api.V1`) used in the controller mappings rather than
  `spring.mvc.servlet.path`, so `/health`, `/error` and Swagger UI stay outside it and Spring
  Security matchers stay plain paths.
- **JWT.** A small custom filter (`TokenAuthenticationFilter`) reads `Authorization: Token`
  and verifies HS256 signature and expiry with Nimbus (`nimbus-jose-jwt`). Spring Security's
  resource-server support assumes `Bearer`, and a custom token resolver plus the
  entry-point wiring needed to emit contract errors and the "present but invalid -> 401 even
  on optional endpoints" rule was no simpler than the filter.
- **Partial updates** use a plain `Map` request body (`Fields`) so absent and `null` stay
  distinguishable; no extra dependency (e.g. JsonNullable) was added.
- **Reads** (article lists, comments) are single SQL queries with joins/subqueries for author,
  `favoritesCount`, `favorited` and `following`; tags for a page of articles are loaded with
  one extra `in (...)` query. Writes use JPA entities.
- **Cascade.** Deleting an article removes comments, favorites and tags through
  `ON DELETE CASCADE` in the migrations.
- **Swagger UI.** springdoc's generated docs are disabled; the UI is pointed at the real
  `openapi.yml`, which is copied onto the classpath at build time (never committed).
- **Boot 4 specifics.** Flyway needs `spring-boot-starter-flyway` (auto-configuration moved
  out of the core starter), and Testcontainers' generic container is used in tests so they do
  not depend on module-specific class names that changed between major versions.

## Dependencies beyond the Boot starters

- `nimbus-jose-jwt` (version managed by the Boot BOM): JWT signing/verification.
- `springdoc-openapi-starter-webmvc-ui` 3.0.0: Swagger UI only.
- `flyway-database-postgresql`: Flyway's PostgreSQL support module.
- `org.testcontainers:testcontainers` (test): real PostgreSQL in integration tests.
