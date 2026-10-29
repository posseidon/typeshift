# Proposal

## Why

TypeShift is a typing trainer for adult learners (Hungarian and Vietnamese speakers) preparing for retail and store-management work, but the workspace currently contains only OpenSpec planning material and no application. Every future feature (dashboard, stage select, typing engine, telemetry) needs a buildable, quality-gated, deployable Spring Boot foundation to land on, so this skeleton is the prerequisite for all subsequent changes.

## What Changes

- Scaffold a Maven **multi-module** project with parent `hu.elte.ntb.typeshift` and two modules: **`typeshift-backend`** (deployable Spring Boot 3.x jar) and **`typeshift-frontend`** (Thymeleaf templates, static assets, Vanilla TS/JS engine placeholder, exercise materials directory).
- Configure the backend runtime: virtual threads enabled, SQLite datasource (`typeshift.db`), SQLite dialect, `ddl-auto=update`, UTF-8 throughout.
- Expose backend REST services with a **springdoc-openapi**-generated OpenAPI 3 contract and browsable **Swagger UI**, providing the machine-readable spec automated test coverage consumes.
- Add simple **username/password** user management: a users table with BCrypt-hashed passwords, form login, and a minimal signup path; no pre-provisioned credentials.
- Wire Maven `verify` (configured in the **parent pom**, inherited by both modules) to fail on violations from Checkstyle, PMD (priority 5), and SpotBugs (Max effort, Low threshold). Semgrep remains an external CLI step; the structure stays ready for it.
- Create the foundational package structure under the backend module's `src/main/java/hu/elte/ntb/typeshift`: `config/`, `controller/`, `model/`, `repository/`, `service/`.
- Package the frontend module's templates and assets into the backend jar at build time so the whole system deploys as a **single jar**, suitable for an **Azure App Service web app on the free tier** (Linux, Java 21).
- Provide a runnable Spring Boot entry point plus a minimal REST endpoint (e.g. `/api/health`) anchoring the OpenAPI contract.

## Capabilities

### New Capabilities

- `build-quality-gates`: The Maven build (parent + both modules) must fail when static analysis standards (Checkstyle, PMD, SpotBugs) are violated, enforcing the OpenSpec constitution's quality enforcement pipeline on every build.
- `application-runtime`: The Spring Boot 3 / Java 21 runtime configuration — virtual threads, SQLite persistence wiring, UTF-8-safe environment, and single-jar deployable packaging — such that the application boots and deploys to the Azure App Service free tier.
- `app-shell`: The server-rendered application shell (Thymeleaf + HTMX) that all TypeShift views plug into, with UTF-8 diacritic support, an index entry view, and static asset placeholders the typing engine will fill in later.
- `api-contract`: REST services exposed with a generated OpenAPI 3 specification and browsable Swagger UI; the contract backbone for automated test coverage.
- `user-account`: Simple username/password account access — hashed credentials at rest, form login, minimal signup, no pre-provisioned users.

### Modified Capabilities

- (none — no existing specs; all capabilities above are introduced by this change)

## Impact

- New code: Maven parent pom plus module tree (`typeshift-backend`, `typeshift-frontend`) under the workspace root, backend Java sources under `hu.elte.ntb.typeshift`, and a runnable application class.
- New frontend assets: Thymeleaf templates (now owned by `typeshift-frontend`), placeholder CSS/JS, exercise materials directory.
- Dependencies introduced: Spring Boot 3.x starters (web, thymeleaf, data-jpa, validation, security), springdoc-openapi-swagger starter, sqlite-jdbc, hibernate-community-dialects.
- Build system: Maven multi-module with strict quality gates inherited by both modules; `mvn verify` at the parent enforces them everywhere.
- Deployment: a single runnable jar targeting Azure App Service free tier (Linux, Java 21); no external services required at MVP.
- Risks accepted: SQLite local-file persistence on App Service does not survive restarts/redeploys — acceptable for MVP; revisit persistent storage later.
- Out of scope: typing engine logic, telemetry REST endpoint payloads and scoring, real exercise content, CI/CD pipeline wiring, dedicated security hardening beyond the login skeleton.
