<API_TASKS>
Before responding to any non-trivial API engineering task — designing, implementing, mocking, testing, monitoring, documenting, or deploying an API or Postman Flow — read `postman/skills/api-engineer/SKILL.md` and follow it. It is the default entry point for API work in this repository. Pure questions and trivial one-line edits do not need it.
</API_TASKS>

## Project
- Spring Boot 4.1.1 (Gradle Kotlin DSL), Java 21 toolchain, root project `portfolio` (`settings.gradle.kts`)
- Package base `com.casl.portfolio`; main `PortfolioApplication` in `src/main/java/com/casl/portfolio/`
- Config: `src/main/resources/application.yaml` (app name/title/version, docker compose disabled, `Server.port: 8666`)

## Build & run
- Use wrapper: `./gradlew` (Unix) or `gradlew.bat` (Windows)
- Common: `./gradlew build`, `./gradlew test`, `./gradlew bootRun`
- Logging uses Log4j2 (`spring-boot-starter-log4j2`) with default logging excluded; config `log4j2-spring.xml` present
- Dev: `spring-boot-devtools` included; Docker Compose support present but `compose.yaml` is empty (see `HELP.md`)

## API
- Base path: `@RequestMapping("/rest-api/v1/crud")` in `PortfolioController`
- Endpoints: `GET /user/{userId}?id={Long}` returns JSON; `POST/PUT/DELETE /user` return strings (stubs)
- Models in `models/` (request/operations/users)

## Tests
- JUnit Platform via `useJUnitPlatform()`
- `@SpringBootTest` context test (`PortfolioApplicationTests.contextLoads()`)
- `@WebMvcTest(PortfolioController.class)` with MockMvc in `PortfolioControllerTest`
- Run all: `./gradlew test`. Run single class: `./gradlew test --tests com.casl.portfolio.controller.PortfolioControllerTest`

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