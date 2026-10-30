# Tasks

## 1. Parent and module scaffolding

- [x] 1.1 Create the parent `pom.xml` (`hu.elte.ntb.typeshift`, pom packaging, Java 21, `spring-boot-starter-parent` 3.5.x line) declaring modules `typeshift-backend` and `typeshift-frontend` and shared versions (springdoc-openapi, sqlite-jdbc, hibernate-community-dialects). Verify: `mvn -q validate` from the parent resolves both modules.
- [x] 1.2 Create `typeshift-frontend` (jar packaging, no Java sources) with directories `src/main/resources/{templates,static/css,static/js,materials}`. Verify: module builds and its jar contains the resource folders.
- [x] 1.3 Create `typeshift-backend` (Spring Boot jar packaging) with dependencies on `typeshift-frontend`, starters `web`, `thymeleaf`, `data-jpa`, `validation`, `security`, plus `springdoc-openapi-starter-webmvc-ui`, `sqlite-jdbc`, `hibernate-community-dialects`; main class `hu.elte.ntb.typeshift.TypeShiftCoreApplication` and package tree `hu/elte/ntb/typeshift/{config,controller,model,repository,service}`. Verify: `mvn -q compile` succeeds.
- [x] 1.4 Create `application.properties` in the backend module with `spring.threads.virtual.enabled=true`, `spring.datasource.url=jdbc:sqlite:typeshift.db`, `spring.datasource.driver-class-name=org.sqlite.JDBC`, `spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect` (Hibernate 6 community dialect; the legacy `org.hibernate.dialect.SQLiteDialect` name no longer exists), `spring.jpa.hibernate.ddl-auto=update`. Verify: `mvn -q spring-boot:run` (from `typeshift-backend`) boots the web server against SQLite and shuts down cleanly.

## 2. Static analysis gates across modules

- [x] 2.1 Configure Checkstyle (`google_checks.xml`, `failsOnError=true`), (`google_checks.xml`, `failsOnError=true`), PMD (`failurePriority=5`, `failOnViolation=true`), and SpotBugs (`effort=Max`, `threshold=Low`, `failOnError=true`) once in the parent, bound to `verify`, inherited by both modules. Verify: `mvn -q help:effective-pom` from each child shows all three plugins.
- [x] 2.2 Run `mvn verify` at the parent over the minimal codebase and fix every finding in code (no suppression files). Verify: `mvn verify` exits with success at the parent.

## 3. Frontend shell (typeshift-frontend module)

- [x] 3.1 Create `templates/layout.html`: HTML5 boilerplate, `<meta charset="UTF-8">`, HTMX via CDN in `<head>`, references to `/css/style.css` and `/js/engine.js`, and a content injection point (`th:replace` layout pattern with a `th:fragment="content"` slot). Verify: template structure follows the decorator pattern task 3.2 composes against.
- [x] 3.2 Create `templates/index.html` declaring `th:fragment="content"` and an `DashboardController` in the backend mapping `/` to the index view. Verify: `mvn -q spring-boot:run` then GET `http://localhost:8080/` returns the index view composed with the shell.
- [x] 3.3 Create `static/css/style.css` (minimal base styling) and `static/js/engine.js` (placeholder comment reserving the typing engine slot), plus a note file in `materials/`, all inside the frontend module. Verify: the HTML from 3.2 references `/css/style.css` and `/js/engine.js`; `materials/` exists.

## 4. OpenAPI contract (backend module)

- [x] 4.1 Configure springdoc-openapi and expose at least one anchor endpoint: `GET /api/health` returning a record DTO. Verify: GET `/v3/api-docs` returns valid OpenAPI 3 JSON and lists `/api/health`, and `/swagger-ui.html` renders it.

## 5. User management (backend module)

- [x] 5.1 Add the `UserAccount` JPA entity (unique username, BCrypt hash) and repository; configure `PasswordEncoder` (BCrypt) and a `SecurityFilterChain` with form login and anonymous access to the signup and shell views. Verify: `mvn -q spring-boot:run` loads the security config and an unauthenticated request to `/` is never asked to log in.
- [x] 5.2 Create the minimal signup route (Thymeleaf view + POST handler in `controller/`) creating a unique-username account with a BCrypt-hashed password; verify the service layer refuses duplicate usernames. Verify: signup then login yields an authenticated session; duplicate signup is rejected; the fresh database stores no plaintext password and no pre-provisioned user.

## 6. Deployment packaging

- [x] 6.1 Build from the parent with `mvn package` and run `java -jar typeshift-backend-*.jar` standalone with only the Java 21 runtime. Verify: the jar contains the frontend templates/assets, serves `/` (shell), `/v3/api-docs`, and Swagger UI booting with no external services — matching the Azure App Service free-tier execution model.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied (implementation done and verified).
- Verify the archived result before closing out.
