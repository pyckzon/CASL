<API_TASKS>
Before responding to any non-trivial API engineering task — designing, implementing, mocking, testing, monitoring, documenting, or deploying an API or Postman Flow — read `postman/skills/api-engineer/SKILL.md` and follow it. It is the default entry point for API work in this repository. Pure questions and trivial one-line edits do not need it.
</API_TASKS>

## Project
- Spring Boot 4.1.2 (Gradle Kotlin DSL), Java 21, monorepo root `casl` (`settings.gradle.kts`)
- Modules under `services/casl-*`; current: `services/casl-users-service` (package `com.casl.users`, main `UsersServiceApplication`), `services/casl-catalog-service` (package `com.casl.catalog`, main `CatalogServiceApplication`), `services/casl-gateway` (package `com.casl.gateway`, main `GatewayApplication`)
- Shared build conventions: `buildSrc` plugin `casl-spring-boot-service`; versions in `gradle/libs.versions.toml`; ADRs in `docs/architecture/decisions/`
- Config: `services/casl-users-service/src/main/resources/application.yaml` (`casl-users-service`, `server.port: 8666`, actuator health expuesto)
- Java 21 toolchain enforced by the `buildSrc` convention plugin

## Build & run
- Use wrapper: `./gradlew` (Unix) or `gradlew.bat` (Windows)
- Common: `./gradlew build`, `./gradlew test`, `./gradlew :services:casl-users-service:bootRun`
- Image (buildpacks): `./gradlew :services:casl-users-service:bootBuildImage` → `casl/casl-users-service:0.0.1-SNAPSHOT`
- Local infra: `docker compose --profile core up -d` (PostgreSQL 17 + service). Do not use `spring-boot-docker-compose` starters; compose is managed manually
- Logging uses Log4j2 (`spring-boot-starter-log4j2`) with default logging excluded by the convention plugin; config `log4j2-spring.xml`
- Dev: `spring-boot-devtools` included as `developmentOnly`

## API
- Users: base path `/api/users/v1/users` (gateway `http://localhost:8080/api/users/**`)
- Catalog: base path `/api/catalog/v1/products`, `/api/catalog/v1/categories` (gateway `http://localhost:8080/api/catalog/**`)
- Errores en `application/problem+json` (RFC 9457) con `code` estable
- Contratos: `postman/specs/users-v1.yaml`, `postman/specs/catalog-v1.yaml` (OpenAPI 3.1)
- Models in `com.casl.users.models` (`users`, `request`)

## Tests
- JUnit Platform via `useJUnitPlatform()`; coverage gate 50% LINE (convention plugin)
- `@SpringBootTest` context test (`UsersServiceApplicationTests.contextLoads()`)
- `@WebMvcTest(UserController.class)` with MockMvc in `UserControllerTest`; requires `spring-boot-starter-webmvc-test` (Boot 4)
- Run all: `./gradlew test`. Run single class: `./gradlew :services:casl-users-service:test --tests com.casl.users.controller.UserControllerTest`

## Notes
- Lombok used (compile/test annotation processors)
- Postman skills under `postman/skills/` with resources in `.postman/`

## AI Working Rules

### Token Efficiency
- Inspect only files relevant to the current task.
- Prefer targeted searches and focused file sections over reading entire directories.
- Do not reread files already inspected unless they may have changed.
- Avoid repeating project information already documented here.
- Keep explanations concise; prioritize actionable findings and results.
- Do not generate documentation or files unless they provide lasting value or are requested.

### Repository Understanding
- Inspect existing implementations before proposing new abstractions.
- Follow the project's existing architecture and coding conventions.
- Reuse existing dependencies and utilities when appropriate.
- Do not assume a file, class, endpoint, dependency, or feature exists without verifying it.
- Do not introduce new dependencies without explaining why they are needed.

### Java and Spring Boot
- Use Java 21 and the versions declared by the project.
- Follow Spring Boot conventions and constructor injection.
- Prefer clear, maintainable implementations over unnecessary abstractions.
- Consider error handling, input validation, security, and testability.
- Preserve the existing Log4j2 logging configuration.
- Do not introduce another logging implementation without explicit justification.

### Documentation and Dependencies
- Treat repository files and build configuration as the source of truth for project versions.
- Consult official documentation matching the versions actually used.
- Prefer targeted documentation searches over loading entire documentation sites.
- Distinguish verified facts from assumptions.
- Never invent API signatures, configuration properties, or dependency behavior.

### Testing and Validation
- Use the Gradle wrapper provided by the repository.
- Run focused tests first, then broader tests when appropriate.
- Report exactly which tests and build commands were executed.
- Never claim that tests pass unless they were actually executed successfully.
- If validation fails, report the relevant error and its likely cause.

### Git and File Safety
- Preserve unrelated changes in the working tree.
- Inspect the relevant diff before completing a change.
- Do not discard user changes, reset branches, or perform destructive Git operations without explicit permission.
- Do not expose credentials, tokens, or secrets in generated files or responses.

### Persistent Knowledge
- Update existing documentation when a lasting architectural decision changes.
- Record significant architectural decisions in docs/architecture/decisions/.
- Avoid duplicating information already present in AGENTS.md or other documentation.
- Do not create persistent notes for temporary debugging details.

## Documentation Research

- Inspect build.gradle.kts and settings.gradle.kts before researching dependencies.
- Identify the exact library and framework versions before proposing version-specific solutions.
- Prefer official documentation and release notes for the versions used by the project.
- Search for the specific API, configuration property, or behavior relevant to the task.
- Read only the relevant documentation sections.
- Use web search to discover sources and web fetch to inspect specific pages when available.
- Verify documentation claims against the project's code and tests.
- Never assume examples from the latest documentation are compatible with older versions.
- If authoritative documentation cannot be found, state the limitation instead of guessing.
- Do not repeatedly search for facts already verified during the current task.