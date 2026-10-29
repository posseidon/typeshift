# Design

## Context

See proposal.md — Why. The workspace is empty apart from OpenSpec planning material; there is no existing code, build, or CI, so every structural choice is made here for the first time and constrains all later changes. The constitution (`openspec/constitution.md`) fixes the stack: Spring Boot 3.x, Java 21, virtual threads, SQLite via JPA/Hibernate, Thymeleaf + HTMX shell, Vanilla TS/JS typing engine, and strict static-analysis gates. The direction set during this revision adds: two Maven modules (backend + frontend), a REST API documented via a generated OpenAPI 3 contract with Swagger UI as the anchor for automated test coverage, simple username/password user management, and deployment to an Azure App Service web app on the free tier.

## Goals / Non-Goals

**Goals:**
- A runnable, buildable Maven multi-module project (parent `hu.elte.ntb.typeshift`) whose modules are `typeshift-backend` (deployable Spring Boot jar) and `typeshift-frontend` (templates, static assets, engine placeholder, materials directory).
- The whole system deploys as a single jar to an Azure App Service web app (free tier, Linux, Java 21).
- Maven `verify` fails on any Checkstyle, PMD (priority 5), or SpotBugs (Max effort, Low threshold) violation, configured once in the parent and inherited by both modules.
- springdoc-openapi-generated OpenAPI 3 spec at `/v3/api-docs` and browsable Swagger UI, with one documented anchor endpoint (`/api/health`) proving the coverage pipeline.
- Username/password accounts: users table, BCrypt hashes, form login, minimal signup, no seeded users.
- Thymeleaf layout shell with HTMX from CDN, UTF-8 meta, content fragment injection point, and an index view rendering through it.

**Non-Goals:**
- Typing engine logic, keystroke handling, scoring (constitution §4 concerns a later change).
- Real telemetry REST endpoint payloads (only the anchor endpoint exists in this change).
- Real exercise content for `materials/` (only the directory is scaffolded).
- CI/CD pipeline wiring (the gates are local-build enforced; pipeline integration comes later).
- Gradle (constitution allows either; Maven is the project spec's explicit choice).
- Advanced security hardening (password reset, email flows, rate limiting) beyond the skeleton.

## Decisions

1. **Maven multi-module with a resource-contributing frontend.** Parent pom `hu.elte.ntb.typeshift` aggregates `typeshift-backend` and `typeshift-frontend`. The frontend module contains only resources (templates, static assets, engine placeholder, materials) and no Java sources; the backend declares it as a dependency so Spring's conventions merge the assets into the deployable fat jar. Alternatives considered: a single `typeshift-core` module (rejected — the direction requires two modules and later engine compilation in the frontend); two independently hosted services (rejected — needs CORS, two App Service slots, and strains the free tier).
2. **Single Azure App Service, run-from-jar.** One Linux App Service with Java 21 runtime runs `java -jar typeshift-backend-*.jar`; the change's packaging check is exactly that invocation locally. Free tier fits: a single instance is cheapest and the constitution's MVP database is local. Deployment tooling (GitHub Actions, azure-maven-plugin) belongs to a later change.
3. **springdoc-openapi for the contract.** Controllers annotated with springdoc annotations generate OpenAPI 3 live at `/v3/api-docs` with Swagger UI at `/swagger-ui.html`; no separate spec file exists to drift. Alternatives: hand-written `openapi.yaml` first (rejected for the skeleton — drift risk and upfront cost), ReDoc-only (rejected — not parsed by test tooling as readily).
4. **Anchor REST endpoint.** `GET /api/health` (modelled in `model/` as record DTO per constitution §2) exists only so the generated contract is non-empty from the first build and automated coverage can consume it. Payload grows into telemetry later.
5. **Username/password auth via Spring Security form login.** `UserAccount` JPA entity (unique username, BCrypt hash) behind Spring Data JPA, a `ReplaceUserDetailsService`-style service, a `SecurityFilterChain` with form login and the signup view permitted to anonymous users, and a `PasswordEncoder` bean (BCrypt). No seeded accounts in any profile. Alternatives: HTTP Basic (rejected — no session UX for the shell views), container-managed auth (rejected — Azure App Service does not provide a fitting realm out of the box). All of this lives in `config/` + `model/` + `repository/` + `service/` per the constitution's package plan.
6. **Quality gates in the parent pom.** Checkstyle (`google_checks.xml`, `failsOnError=true`), PMD (`failurePriority=5`, `failOnViolation=true`), and SpotBugs (`effort=Max`, `threshold=Low`, `failOnError=true`) are declared once in `<pluginManagement>`/`<plugins>` of the parent and bound to `verify`, so every module inherits them (the frontend has no Java sources; its checks are effectively report-empty, which is fine). Alternative: per-module configuration (rejected — drift).
7. **Runtime configuration.** `spring.threads.virtual.enabled=true`, `spring.datasource.url=jdbc:sqlite:typeshift.db`, `spring.datasource.driver-class-name=org.sqlite.JDBC`, `spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect` (the Hibernate 6 community dialect — the older `org.hibernate.dialect.SQLiteDialect` name is defunct), `spring.jpa.hibernate.ddl-auto=update`, UTF-8 charset in the layout and Thymeleaf encoding properties. Configured in the backend module's `application.properties` exactly as the project spec states, with that one dialect correction.
8. **Package root stays `hu.elte.ntb.typeshift`.** Confirmed in the previous revision: backend code occupies `src/main/java/hu/elte/ntb/typeshift/{config,controller,model,repository,service}`; backend module keeps its role as the deployable.
9. **`materials/` lives in the frontend module** under `src/main/resources/materials/` (the constitution's "static JSON/Markdown in `src/main/resources` or an external volume"). Serving through `@Cacheable` backend endpoints is a later change; the directory is reserved here.
10. **Semgrep: readiness only.** The Java sources, templates, and REST annotations all live in standard Maven locations, so Semgrep targeting later works out of the box; no Maven integration is added this change.

## Risks / Trade-offs

- [SQLite file persistence on Azure App Service is ephemeral (local `typeshift.db` is lost on restart/redeploy)] → Accepted for MVP per the parent decision; data is regenerable so long as users re-signup; a later change can migrate to mounted persistent storage or Azure SQL free tier. Noted prominently so no one assumes durability.
- [Free-tier App Service cold starts and quotas] → Not addressable in the skeleton; noted so timing expectations are set. Local `java -jar` behavior is the verified contract, not VM size tuning.
- [Spring Security form login before any frontend styling implies a login page appears in front of the shell] → Accepted: anonymous access is granted to the shell view and signup, and login is enforced per endpoint as secured work arrives; in this change no endpoints are locked down besides auth itself.
- [Google Checkstyle conflicts with placeholder code verbosity] → Codebase is small and hand-written; violations are fixed in files, not suppressed — no suppression files introduced.
- [SpotBugs Max effort/Low threshold on a multi-module build can flag generated bytecode patterns] → Only one module has compiled sources; findings surface at the first `verify` run, before the codebase grows.
- [htmx CDN coupling means the shell is unusable offline] → Accepted for MVP; vendoring later touches only `layout.html`.

## Migration Plan

Not applicable — greenfield scaffolding; nothing is replaced or migrated. Rollback is simply not adopting the change directory.

## Open Questions

- None. Deployment shape, auth scope, and OpenAPI tooling were decided during this revision (single App Service; login included; springdoc-openapi).
