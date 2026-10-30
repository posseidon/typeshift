# Epic 0b — Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the same Spring Boot app run against PostgreSQL with Flyway migrations locally (Docker Compose + Ollama) and in prod (Supabase + Azure OpenAI).

**Architecture:** Replace the SQLite/JPA-auto-DDL prototype with PostgreSQL + Flyway owning the schema, split config into `dev` and `prod` Spring profiles, and introduce an `AIContentService` abstraction with one implementation per profile (Ollama in dev, Azure OpenAI in prod) behind the ADR-004 envelope.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven multi-module (`typeshift-backend`, `typeshift-frontend`), PostgreSQL 16, Flyway, Docker Compose, Spring `RestClient`, Ollama, Azure OpenAI.

**Spec:** `openspec/epics/epic-0b-infrastructure/epic.md` and stories `story-0b.1` … `story-0b.7` in the same folder; ADRs `openspec/adrs/ADR-004`, `ADR-005`, `ADR-009`, `ADR-010`, `ADR-012`, `ADR-017`; decisions in `openspec/epics/epic-0b-infrastructure/gaps-and-questions.md`.

## Global Constraints

- Java 21 (`.java-version`, pom `<java.version>21`).
- Spring Boot `3.5.16` (parent pom).
- Quality gates in root `pom.xml` must stay green: checkstyle (`google_checks.xml`, `failOnViolation=true`), PMD (`failurePriority=5`, `failOnViolation=true`), SpotBugs (`effort=Max`, `threshold=Low`, `failOnError=true`). Every task's code and tests must pass `mvn verify`.
- Package root `hu.elte.ntb.typeshift`, domain-by-domain (AGENTS.md): a new feature gets a new domain package; cross-cutting concerns go in `config`/`security`.
- No Telex or Vietnamese fields anywhere in the AI envelope (ADR-005). `contentType` is `PROSE` or `MATH` only.
- All schema changes are plain-SQL Flyway migrations (ADR-009); no Supabase extensions (RLS/Realtime/pgvector).
- Model name is an environment variable, never hardcoded (ADR-010).
- Secrets (passwords, API keys, JWT secret) are environment variables or a git-ignored `.env`, never committed.

## Review Focus

1. **Dev and prod sharing a database** — a misconfigured profile could point dev at Supabase. A reasonable person expects the two profiles to be physically different DBs. → Task 3 test asserts dev URL is `127.0.0.1` and prod URL comes from `DATABASE_URL`.
2. **A secret committed to git** — `.env` (or a prod secret) accidentally tracked. → Task 1 test asserts `.env` is git-ignored.
3. **AI provider returning malformed JSON or a non-2xx** — a raw `JsonMappingException`/`RestClientException` leaking out instead of `AiProviderException` with the cause. → Task 4 test pins the error mapping.
4. **Hibernate and Flyway both owning the schema** — `ddl-auto` left at `update` silently fights Flyway. → Task 5 test asserts `ddl-auto=validate`.
5. **Missing required env var at startup** — the app boots half-configured instead of failing fast with a clear message. → Tasks 2/7 tests pin fail-fast on absent `DATABASE_URL`/`AZURE_OPENAI_API_KEY`.
6. **Ollama model not installed** — the app should warn, not crash or silently mis-generate. → Task 6 test pins the warn-not-crash behavior.

---

### Task 1: Docker Compose PostgreSQL (Story 0b.1)

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`
- Modify: `.gitignore`
- Create: `docs/database.md`

**Interfaces:**
- Produces: a local PostgreSQL on `127.0.0.1:5432`, database `typeshift`, user/password from `.env` (`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`). Tasks 2/3/5 read `127.0.0.1:5432/typeshift` from here.

- [ ] **Step 1: Write the failing test (verification)**

Run: `docker compose config -q`
Expected: FAIL — no `docker-compose.yml`, so the command errors.

- [ ] **Step 2: Implement `docker-compose.yml`, `.env.example`, `.gitignore` change, `docs/database.md`**

`docker-compose.yml`: one `postgres:16-alpine` service named `db`; named volume `pgdata`; env read from a `.env` file (`POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB=typeshift`); ports bound to `127.0.0.1:5432:5432` only; `healthcheck` using `pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}` with `interval: 5s`, `retries: 5`.

`.env.example`: the three `POSTGRES_*` keys with placeholder values and a comment stating it is copied to `.env`.

`.gitignore`: add a line `.env`.

`docs/database.md`: the documented commands — `docker compose up -d` (start), `docker compose down` (stop), `docker compose down -v` (wipe data).

- [ ] **Step 3: Run test to verify it passes**

Run: `docker compose config -q`
Expected: PASS (exit 0, no output).

- [ ] **Step 4: Verify healthcheck + git-ignore**

Run: `docker compose up -d && docker compose ps` (start, then inspect status), then `git check-ignore .env`
Expected: `db` reaches `healthy`; `git check-ignore` prints `.env`.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml .env.example .gitignore docs/database.md
git commit -m "feat(infra): add docker compose postgres with named volume and healthcheck"
```

---

### Task 2: Supabase connection configuration (Story 0b.2)

**Files:**
- Create: `typeshift-backend/src/main/resources/application-prod.yml`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/ProdDatasourceConfigTest.java`

**Interfaces:**
- Produces: prod datasource `spring.datasource.url=${DATABASE_URL}`, Hikari `maximum-pool-size=2`, and fail-fast on connection failure. Task 3 consumes this file; Task 5 depends on Postgres being the only driver.

- [ ] **Step 1: Write the failing test**

`ProdDatasourceConfigTest` (JUnit 5 + `ApplicationContextRunner`):

```java
contextRunner().withUserConfiguration(...)
    .withPropertyValues("spring.profiles.active=prod",
        "DATABASE_URL=jdbc:postgresql://db.example:5432/typeshift",
        "DB_USERNAME=u", "DB_PASSWORD=p")
    .run(ctx -> {
      assertThat(ctx).hasSingleBean(HikariDataSource.class);
      assertThat(ctx.getBean(HikariDataSource.class).getMaximumPoolSize()).isEqualTo(2);
      assertThat(ctx.getBean(HikariDataSource.class).getJdbcUrl())
          .isEqualTo("jdbc:postgresql://db.example:5432/typeshift");
    });
```

Expected: FAIL — `application-prod.properties` does not exist, so the prod datasource is absent.

- [ ] **Step 2: Implement `application-prod.yml`**

Keys (values are env vars, `${ENV:}` placeholders):

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 2
```

Fail-fast is Hikari's default (`initialization-fail-timeout=1`): a dead URL aborts startup with a pool exception carrying the cause — that is the "clear message".

- [ ] **Step 3: Run test to verify it passes**

Run: `mvn -pl typeshift-backend test -Dtest=ProdDatasourceConfigTest`
Expected: PASS.

- [ ] **Step 4: Verify fail-fast on a missing URL**

Run: `mvn -pl typeshift-backend -Dspring.profiles.active=prod spring-boot:run` (with no `DATABASE_URL` set)
Expected: startup fails fast with a datasource/URL error naming the missing value.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/application-prod.properties typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/ProdDatasourceConfigTest.java
git commit -m "feat(infra): add prod datasource config for supabase with small pool"
```

---

### Task 3: Spring Boot profiles (Story 0b.3)

**Files:**
- Create: `typeshift-backend/src/main/resources/application-dev.yml`
- Rename: `typeshift-backend/src/main/resources/application.properties` → `application.yml`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/ProfileConfigTest.java`

**Interfaces:**
- Consumes: dev Postgres `127.0.0.1:5432/typeshift` (Task 1), prod `DATABASE_URL` (Task 2), Ollama `OLLAMA_BASE_URL`/`OLLAMA_MODEL` (Task 6), Azure `AZURE_OPENAI_*` (Task 7).
- Produces: `dev` profile → local Postgres + Ollama; `prod` profile → Supabase + Azure OpenAI. Tasks 4/5/6/7 rely on these two profiles existing.

- [ ] **Step 1: Write the failing test**

`ProfileConfigTest` — two `ApplicationContextRunner` cases:

```java
// dev
.withPropertyValues("spring.profiles.active=dev")
.run(ctx -> assertThat(ctx.getEnvironment().getActiveProfiles()).contains("dev"));

// prod
.withPropertyValues("spring.profiles.active=prod")
.run(ctx -> assertThat(ctx.getEnvironment().getActiveProfiles()).contains("prod"));
```

Plus a distinct-database assertion: dev resolves `jdbc:postgresql://127.0.0.1:5432/typeshift`, prod resolves from `DATABASE_URL` (never equal to the dev URL).

Expected: FAIL — `application-dev.properties` does not exist.

- [ ] **Step 2: Implement `application-dev.yml` and rename `application.properties` → `application.yml`**

`application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/typeshift
    username: ${POSTGRES_USER:typeshift}
    password: ${POSTGRES_PASSWORD:typeshift}
typeshift:
  ai:
    ollama-base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
    ollama-model: ${OLLAMA_MODEL:qwen2.5:14b}
```

`application.yml` (renamed from `application.properties`): drop the SQLite block (`spring.datasource.url=jdbc:sqlite:...`, the driver, the `SQLiteDialect`, `ddl-auto`); keep the remaining keys in YAML form; add `spring.main.banner-mode: log`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
  main:
    banner-mode: log
typeshift:
  jwt-secret: ${JWT_SECRET:}
  cookie-secure: ${COOKIE_SECURE:false}
```

Add an `ApplicationRunner` bean in `config` that logs the active profiles (`ctx.getEnvironment().getActiveProfiles()`). (`ddl-auto=validate` and `flyway.enabled` land in Task 5.)

- [ ] **Step 3: Run test to verify it passes**

Run: `mvn -pl typeshift-backend test -Dtest=ProfileConfigTest`
Expected: PASS.

- [ ] **Step 4: Verify the active profile is logged at startup**

Run: `mvn -pl typeshift-backend -Dspring.profiles.active=dev spring-boot:run` (against the Task 1 DB)
Expected: startup log prints the active profile `dev`.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/application-dev.properties typeshift-backend/src/main/resources/application.properties typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/ProfileConfigTest.java
git commit -m "feat(infra): add dev and prod spring profiles with env-var secrets"
```

---

### Task 4: AIContentService provider abstraction (Story 0b.4)

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/model/AiRequest.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/model/AiResponse.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/model/ExamType.java`, `LanguageTarget.java`, `TaskType.java`, `ContentType.java` (enums)
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/AIContentService.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/AzureOpenAIContentService.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/OllamaContentService.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/AiProviderException.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/AiProperties.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/AIContentServiceTest.java`

**Interfaces:**
- Consumes: dev/prod profiles (Task 3); env vars `OLLAMA_BASE_URL`, `OLLAMA_MODEL`, `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT`.
- Produces (epic 3 consumes these exact names):

```java
public interface AIContentService { AiResponse generate(AiRequest request); }
public record AiRequest(int stageId, ExamType examType, List<String> focusKeys,
    List<String> recentContentHashes, LanguageTarget languageTarget, TaskType taskType) {}
public record AiResponse(ContentType contentType, String displayText, String expectedAnswer,
    String contentHash, int estimatedDurationSeconds) {}
public final class AiProviderException extends RuntimeException {
  public AiProviderException(String message, Throwable cause) { super(message, cause); }
}
```

Enums: `ExamType {SUB_EXAM, GRADUATION}`, `LanguageTarget {HUNGARIAN, ENGLISH, MIXED}`, `TaskType {CHAT_BURST, EMAIL_REPLY, DOCUMENT_PARAGRAPH, CODE_COMMENT}`, `ContentType {PROSE, MATH}`. No Telex/Vietnamese values.

- [ ] **Step 1: Write the failing test**

`AIContentServiceTest` — using Spring's `MockRestServiceServer` bound to a `RestClient.Builder` that each impl is constructed with:

```java
// Ollama: POST to {baseUrl}/api/generate with model=OLLAMA_MODEL, maps the JSON
//   response's "response"/"content" fields into AiResponse with contentType PROSE.
// Azure: POST to {endpoint}/openai/deployments/{deployment}/chat/completions
//   with api-key header, maps choices[0].message.content into AiResponse.

@Test void ollamaMapsResponseEnvelope() { /* mock 200 + JSON; assert AiResponse fields */ }
@Test void azureMapsResponseEnvelope() { /* mock 200 + JSON; assert AiResponse fields */ }
@Test void providerErrorBecomesAiProviderExceptionWithCause() {
  // mock a 500; assertThrows(AiProviderException.class); assert cause != null (full stack retained)
}
@Test void nonJsonResponseBecomesAiProviderException() { /* malformed body -> AiProviderException */ }
```

Expected: FAIL — the `ai` package and `AIContentService` do not exist.

- [ ] **Step 2: Implement the envelope types, interface, and `AiProperties`**

`AiProperties` — `@ConfigurationProperties(prefix = "typeshift.ai")` record with `azureEndpoint`, `azureApiKey`, `azureDeployment`, `ollamaBaseUrl`, `ollamaModel`.

- [ ] **Step 3: Implement `OllamaContentService` and `AzureOpenAIContentService`**

Both implement `AIContentService`; constructors take a `RestClient.Builder` (or a prebuilt `RestClient`) plus `AiProperties`. `OllamaContentService` is `@Profile("dev")`, `AzureOpenAIContentService` is `@Profile("prod")` — exactly one bean active at runtime. `AzureOpenAIContentService` fails fast at construction if `azureEndpoint`, `azureApiKey`, or `azureDeployment` is blank, throwing `IllegalStateException` with a message naming the missing key. Any transport/serialization failure is wrapped in `AiProviderException` with the underlying exception as `cause` (ADR-017: full stack trace preserved). Provider-specific types (e.g. the HTTP client's exceptions) never cross the `generate` boundary.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl typeshift-backend test -Dtest=AIContentServiceTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/AiProperties.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/AIContentServiceTest.java
git commit -m "feat(ai): add AIContentService abstraction with azure and ollama impls"
```

---

### Task 5: Flyway baseline migration (Story 0b.5)

**Files:**
- Modify: `typeshift-backend/pom.xml`
- Modify: `typeshift-backend/src/main/resources/application.yml`
- Create: `typeshift-backend/src/main/resources/db/migration/V1__baseline.sql`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/FlywayConfigTest.java`

**Interfaces:**
- Consumes: local Postgres (Task 1), prod Postgres (Task 2), the two profiles (Task 3).
- Produces: Flyway owns the schema (`ddl-auto=validate`, `flyway.enabled=true`); `V1__baseline.sql` is the empty marker epic 1 will follow.

- [ ] **Step 1: Write the failing test**

`FlywayConfigTest` (`ApplicationContextRunner`):

```java
.run(ctx -> {
  assertThat(ctx).hasSingleBean(Flyway.class);
  assertThat(ctx.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
});
```

Expected: FAIL — no Flyway on the classpath, `ddl-auto` still `update`.

- [ ] **Step 2: Swap dependencies in `typeshift-backend/pom.xml`**

Remove `org.xerial:sqlite-jdbc` and `org.hibernate.orm:hibernate-community-dialects`. Add `org.postgresql:postgresql` (runtime), `org.flywaydb:flyway-core`, `org.flywaydb:flyway-database-postgresql` (versions managed by the Boot parent).

- [ ] **Step 3: Point config at Postgres + Flyway, add the marker migration**

`application.yml`: add

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
```

Delete `typeshift.db` if present.

`V1__baseline.sql`: a comment-only file `-- Baseline (no schema yet; epic 1 adds the tables).`

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -pl typeshift-backend test -Dtest=FlywayConfigTest`
Expected: PASS.

- [ ] **Step 5: Verify migrations run clean on both databases (manual, per 0b-Q6)**

Run: boot once against the Task 1 compose DB (`-Dspring.profiles.active=dev`), then once with `DATABASE_URL` pointed at Supabase (`-Dspring.profiles.active=prod`).
Expected: both start; `flyway_schema_history` is created with row `1` (`V1__baseline.sql`).

- [ ] **Step 6: Commit**

```bash
git add typeshift-backend/pom.xml typeshift-backend/src/main/resources/application.properties typeshift-backend/src/main/resources/db/migration/V1__baseline.sql typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/FlywayConfigTest.java
git commit -m "feat(infra): switch to postgres with flyway owning the schema"
```

---

### Task 6: Ollama local setup (Story 0b.6)

**Files:**
- Create: `docs/ollama.md`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/OllamaModelAvailabilityRunner.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/OllamaModelAvailabilityRunnerTest.java`

**Interfaces:**
- Consumes: `OLLAMA_MODEL` (default `qwen2.5:14b`), `OLLAMA_BASE_URL` (Task 3), `AiProperties` (Task 4).
- Produces: a dev-only startup warning when the configured model is not present.

- [ ] **Step 1: Write the failing test**

`OllamaModelAvailabilityRunnerTest` — construct the runner with a stubbed `RestClient` (via `MockRestServiceServer`) whose `/api/tags` returns a tag list NOT containing the configured model; assert the runner logs a warning and does NOT throw; a second case where the model IS present asserts no warning.

Expected: FAIL — the runner class does not exist.

- [ ] **Step 2: Implement `OllamaModelAvailabilityRunner` and `docs/ollama.md`**

Runner: `@Component` + `@Profile("dev")`, `implements ApplicationRunner`; on startup, `GET {baseUrl}/api/tags` and check for `typeshift.ai.ollama-model`; log a WARN if missing, never throw. Uses the same `RestClient`/`AiProperties` wiring as Task 4.

`docs/ollama.md`: `ollama serve` (native install, not Compose), default model `qwen2.5:14b`, 8 GB fallback **Mistral 7B** (set `OLLAMA_MODEL=mistral`), and that the model name is an env var.

- [ ] **Step 3: Run tests to verify they pass**

Run: `mvn -pl typeshift-backend test -Dtest=OllamaModelAvailabilityRunnerTest`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add docs/ollama.md typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/OllamaModelAvailabilityRunner.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/OllamaModelAvailabilityRunnerTest.java
git commit -m "feat(ai): warn on missing ollama model at startup"
```

---

### Task 7: Azure OpenAI configuration (Story 0b.7)

**Files:**
- Create: `docs/azure-openai.md`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/health/controller/ProviderConnectivityController.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/SecurityConfig.java`
- Modify: `typeshift-backend/src/main/resources/application-prod.yml`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/health/controller/ProviderConnectivityControllerTest.java`

**Interfaces:**
- Consumes: `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT` (prod, Task 3/4), the active `AIContentService` (Task 4).
- Produces: a dev-profile-only on-demand connectivity route in the health domain.

- [ ] **Step 1: Write the failing test**

`ProviderConnectivityControllerTest` — `@WebMvcTest` with a mocked `AIContentService`: GET `/api/admin/provider-connectivity` returns 200 with a reachability payload; and a test asserting the controller bean is `@Profile("dev")` (not registered under `prod`).

Expected: FAIL — the controller does not exist.

- [ ] **Step 2: Implement the controller, security entry, and `docs/azure-openai.md`**

`ProviderConnectivityController`: `@RestController` + `@Profile("dev")` in the `health` domain, `GET /api/admin/provider-connectivity`; performs an on-demand reachability probe of the active provider and returns a small status record. Lives beside `HealthController`, not the public UI.

`SecurityConfig`: add `/api/admin/provider-connectivity` to the `permitAll()` list (it is dev-only; the profile already hides it in prod).

`application-prod.yml`: add the Azure env-var keys (empty defaults; fail-fast is Task 4's `AiProperties` validation):

```yaml
typeshift:
  ai:
    azure-endpoint: ${AZURE_OPENAI_ENDPOINT:}
    azure-api-key: ${AZURE_OPENAI_API_KEY:}
    azure-deployment: ${AZURE_OPENAI_DEPLOYMENT:}
```

`docs/azure-openai.md`: the quota prerequisite for a GPT-4o-class deployment, and the three env vars.

- [ ] **Step 3: Run tests to verify they pass**

Run: `mvn -pl typeshift-backend test -Dtest=ProviderConnectivityControllerTest`
Expected: PASS.

- [ ] **Step 4: Verify fail-fast on a missing API key**

Run: `mvn -pl typeshift-backend -Dspring.profiles.active=prod spring-boot:run` with `AZURE_OPENAI_API_KEY` unset.
Expected: startup fails with a clear message naming the missing key.

- [ ] **Step 5: Commit**

```bash
git add docs/azure-openai.md typeshift-backend/src/main/java/hu/elte/ntb/typeshift/health/controller/ProviderConnectivityController.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/SecurityConfig.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/health/controller/ProviderConnectivityControllerTest.java
git commit -m "feat(infra): add azure openai config and dev-only connectivity check"
```
