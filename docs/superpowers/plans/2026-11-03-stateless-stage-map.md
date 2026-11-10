# Stateless Stage Map Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep AI-generated exercises while making the stage map the public landing page and removing persistence and user management.

**Architecture:** Keep Spring Boot, Thymeleaf, the catalog, and Gemini generation. Exercise generation becomes independent of accounts and databases; the public stage map lists the catalog, while exercise results remain temporary browser state.

**Tech Stack:** Java 21, Spring Boot 3.5, Thymeleaf, Spring AI Google GenAI, Maven.

**Spec:** `docs/superpowers/specs/2026-11-03-stateless-stage-map-design.md`

## Global Constraints

- Keep the current Spring Boot, Thymeleaf, and frontend layout conventions.
- `CannedCatalog` continues to define which stage/exercise choices are available and supplies generation parameters.
- Gemini configuration and `GEMINI_API_KEY` remain required for generating exercises.
- The user's existing deletions and edits are intentional and must be preserved; this change repairs the remaining references and tests rather than restoring deleted user-management code.

## Review Focus

- Unknown stage or exam key must return 404 — test both exercise shell and docket routes in Task 1.
- Invalid or oversized AI prose must be rejected without a second generation attempt — pin with `invalidExerciseFailsWithoutRegeneration` and an oversized-response case in Task 1.
- Invalid generated math must not be served as a valid answer — pin with the math validation case in Task 1.
- Stage 5 generation must still choose a task and validate its generated answer with no history — assert the request and task type in Task 1.
- A Gemini/provider failure must keep the exercise shell usable and expose retry feedback without redirecting to login — assert the public shell's retry behavior in Task 2.

---

### Task 1: Remove persistence from exercise generation and delivery

**Files:**
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/service/ExerciseGenerationService.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseController.java`
- Delete: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/service/ExerciseSubmissionService.java`
- Delete: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/service/ExerciseTicketService.java`
- Modify: `typeshift-backend/pom.xml`, `typeshift-backend/src/main/resources/application.yml`, and `application-dev.yml` only where database/auth configuration remains
- Delete: `docker-compose.yml` (its only service is local PostgreSQL)
- Modify: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/service/ExerciseGenerationServiceTest.java`
- Modify: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseControllerTest.java`
- Rewrite: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/stagemap/controller/StageMapControllerTest.java` to remove auth/database fixtures and assert public catalog links
- Modify: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/ProfileConfigTest.java`
- Delete obsolete database, authentication, account, profile, login, signup, learning-profile, dashboard, and submission tests: `AuthProbeController.java`; `account/model/UserAccountTest.java`, `account/service/UserAccountServiceTest.java`; `dashboard/controller/DashboardControllerTest.java`; `learning/service/LearningProfileServiceTest.java`; `login/service/LoginAttemptServiceTest.java`, `login/service/LoginServiceTest.java`, `login/controller/LoginControllerTest.java`; `profile/controller/ProfileControllerTest.java`; `security/JwtTokenServiceTest.java`, `security/RevocationServiceTest.java`, `security/JwtAuthenticationFilterTest.java`; `signup/controller/SignupControllerTest.java`; `exercise/service/ExerciseSubmissionServiceTest.java`; and `config/ProdDatasourceConfigTest.java`, `config/SchemaMigrationTest.java`, `config/FlywayConfigTest.java`.

**Interfaces:**
- `ExerciseGenerationService.generate(CannedExercise definition) -> CannedExercise` uses the existing `GeminiContentService` and makes an `AiRequest` with empty history/focus lists; keep response validation and randomized Stage 5 task selection.
- `ProseExerciseController` serves public exercise shell/docket routes and no longer accepts a principal or exposes a result-submission endpoint.

- [x] **Step 1: Update tests before implementation** — rewrite generation and exercise controller tests for the new signatures/public routes; add success, invalid/oversized response, math validation, Stage 5 selection, unknown-route, unauthenticated docket, and removed-submit-route assertions. Remove the listed auth/database-only tests and rewrite the old stage-map fixtures so no test depends on deleted user-management types. The checkout is already uncompilable because its manual deletions left production callers behind, so run the focused tests after Step 4 rather than treating those baseline compile errors as a useful red test.
- [x] **Step 2: Implement stateless generation** — remove `UserAccountService`, `JdbcTemplate`, database lookups/writes, recent-hash logic, and username parameters; keep empty focus/history inputs, response validation, response shaping, and randomized Stage 5 task selection.
- [x] **Step 3: Remove ticketing and result persistence** — update the exercise controller to call `generate(definition)`; remove `Principal`, ticket verification, and the submit route; delete the two database/ticket services.
- [x] **Step 4: Remove backend database/auth dependencies and configuration** — drop JPA, Security, JDBC/database drivers, Flyway, and JJWT dependencies; remove the PostgreSQL-only Compose file; update profile config assertions to preserve Gemini configuration without datasource assumptions.
- [x] **Step 5: Verify the backend slice** — run `mvn -pl typeshift-backend -Dtest=ExerciseGenerationServiceTest,ProseExerciseControllerTest,ProfileConfigTest test`; confirm generation, public exercise routes, and Gemini configuration pass without datasource properties. The stage-map route tests run in Task 2 after its controller exists.

### Task 2: Make the stage map the public landing and exercise completion destination

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/stagemap/controller/StageMapController.java`
- Delete: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/LayoutDefaultsConfig.java` (its only fallback targets the removed dashboard fragment)
- Modify: `typeshift-frontend/src/main/resources/templates/stage-map.html`
- Modify: `typeshift-frontend/src/main/resources/templates/layout.html`
- Modify: `typeshift-frontend/src/main/resources/templates/exercise.html`
- Modify: `typeshift-frontend/src/main/resources/static/js/engine.js`
- Modify: `typeshift-frontend/src/main/resources/messages.properties`, `messages_hu.properties`, and `messages_vi.properties` for the no-save completion and practice footer labels
- Delete: `typeshift-frontend/src/main/resources/templates/index.html` (obsolete account/results dashboard)
- Modify: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/stagemap/controller/StageMapControllerTest.java` (prepared without auth/database fixtures in Task 1)
- Rewrite: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseControllerTest.java` as needed for public routes and the no-save completion flow

**Interfaces:**
- `StageMapController.stageMap(Model) -> String` handles both `/` and `/stage-map`, supplies sorted stage IDs, stage names, and catalog exercises, and renders `layout` with the `stage-map` fragment.
- The stage-map template iterates over those model values; every catalog exercise links to `/exercise/{stageId}/{examKey}` without account progress or lock state.
- The exercise completion form uses GET navigation to `/stage-map`; no result data or exercise ticket is submitted.

- [x] **Step 1: Finish the public landing/map tests** — assert `/` and `/stage-map` render the stage-map fragment without a cookie, every `CannedCatalog` entry has a matching exercise link, the exercise completion form has no ticket/result fields and navigates with GET, and retry feedback is available without a login redirect.
- [x] **Step 2: Run the stage-map tests** — `mvn -pl typeshift-backend -am -Dtest=StageMapControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`; build the frontend dependency in the reactor and confirm the missing controller/route and progress-dependent template fail.
- [x] **Step 3: Implement the catalog-driven stage map** — add the controller and replace fake progression/certification content with all five stages and all catalog links.
- [x] **Step 4: Update navigation and exercise completion** — remove profile/logout links, point the brand to the stage map, remove login-redirect handling and ticket/result fields, and make the enabled completion button return to `/stage-map`.
- [x] **Step 5: Remove the obsolete dashboard view** — delete `index.html`; the stage map now owns `/` and `/stage-map`.
- [x] **Step 6: Verify map and exercise routes** — run `mvn -pl typeshift-backend -am -Dtest=StageMapControllerTest,ProseExerciseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`; verify public docket loading, unknown routes return 404, all catalog links work, numeric/prose views remain correct, and completion does not POST or persist results.

### Task 3: Final integration and static checks

**Files:**
- Verify the backend and frontend modules, remaining YAML configuration, and all files touched in Tasks 1–2.

- [x] **Step 1: Run the complete Maven test suite** — `mvn test`; confirm both modules and all remaining tests pass.
- [x] **Step 2: Run Maven verification checks** — `mvn verify`; confirm Checkstyle, PMD, SpotBugs, and packaging succeed.
- [x] **Step 3: Inspect remaining database/auth references** — search main source, configuration, and POMs for JDBC/JPA/Flyway/database drivers, account/auth routes, and stale templates; confirm no runtime references remain.
- [x] **Step 4: Inspect the final diff** — ensure existing user edits/deletions and `.env.example` changes remain untouched; do not commit or stage files because the workspace already contains user-staged changes.
