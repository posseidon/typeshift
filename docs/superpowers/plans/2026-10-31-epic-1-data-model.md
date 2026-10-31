# Epic 1 — Core Data Model Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the schema to the story-1.x state with plain-SQL Flyway migrations V3–V9: aligned user/learning-profile tables, the stage reference table, and the practice-session, score-record, weak-key-profile, and graduation-attempt tables.

**Architecture:** Each story adds one migration to `typeshift-backend/src/main/resources/db/migration` (V1/V2 are never rewritten). Two `@Table` renames realign existing entities with the story names; the six new tables have no entities yet (their writers/readers arrive in epics 2, 4, 6, 7 — extra DB tables are ignored by `ddl-auto=validate`).

**Tech Stack:** PostgreSQL 16 (Docker Compose + Supabase), Flyway plain SQL (ADR-009), Java 21 / Spring Boot 3.5.16 multi-module Maven.

**Spec:** `openspec/epics/epic-1-data-model/epic.md` and `story-1.1` … `story-1.7` in the same folder; ADR-002, 003, 009, 012, 013, 015, 016 under `openspec/adrs/`; `openspec/constants/stage-map.md`; decisions in `openspec/epics/epic-1-data-model/gaps-and-questions.md`.

## Global Constraints

- Java 21, Spring Boot `3.5.16`. Every task ends green: `mvn -pl typeshift-backend verify` (checkstyle `google_checks.xml` failOnViolation, PMD failurePriority 5, SpotBugs threshold Low — all failing).
- All schema changes are plain standard PostgreSQL SQL in Flyway migrations (ADR-009); the same migrations must run clean on Docker Compose Postgres and Supabase. No RLS, no extensions.
- One migration per story, named `V3__…` through `V9__…` in `typeshift-backend/src/main/resources/db/migration`. Never modify `V1__baseline.sql` or `V2__core_schema.sql`.
- `spring.jpa.hibernate.ddl-auto=validate` must keep passing: entity-mapped columns and DB columns must match in type and nullability. Existing timestamp columns are declared `TIMESTAMP(6) WITH TIME ZONE` (V2 style) — use the same phrasing.
- Tests keep the SQLite in-memory setup untouched; Flyway never runs in tests. Migration structure is pinned by `SchemaMigrationTest` (Task 1), the real-db run happens once in Task 8.
- New tables use the story names verbatim (singular): `stage`, `practice_session`, `score_record`, `weak_key_profile`, `graduation_attempt`. Constraints named `uk_/fk_/ck_/idx_…` following the V2 prefix style.
- No Telex or Vietnamese fields; `contentType` is `PROSE` or `MATH` (ADR-004/005).

## Review Focus

1. **SQLite tests bypass Flyway entirely** — a broken migration would ship silently. A reasonable person expects every migration to have been applied to a real Postgres at least once. → Each task adds a `SchemaMigrationTest` method pinning its file's key DDL; Task 8 runs all migrations on real Compose Postgres.
2. **Fresh vs upgraded database divergence** — V3 renames tables, so an existing dev DB and a fresh one must both end at the same shape. → Task 8 boots once on the surviving volume (upgraded path) and once after `docker compose down -v` (fresh path).
3. **FK type mismatch for `stage_id`** — if `stage.id` were `INTEGER`, FKs from `BIGINT` tables would fail at create time. → Task 3 test asserts `id BIGINT` and Task 8 proves the create runs.
4. **Stories 1.7/2.3 need score rows linkable to attempts** — the only sane direction is `score_record.graduation_attempt_id` (one attempt has three sub-scores at stage 5). → Task 7 test pins the column and the only-for-graduation CHECK.
5. **Weak-key merge of Story 4.5 relies on an upsert** — merging a session's miss map needs `ON CONFLICT (user_id, key_char)` to work, i.e. the composite PK must exist exactly. → Task 6 test pins the composite primary key.
6. **Seed rows drift from the authoritative stage map** — wrong names/counts would confuse every later plan that reads them. → Task 3 test asserts all five seed rows and counts 3,2,2,2,4 (ADR-013).

---

### Task 1: `app_user` + revoked-token schema (Story 1.1)

**Files:**
- Create: `typeshift-backend/src/main/resources/db/migration/V3__app_user_schema.sql`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/account/model/UserAccount.java:20`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/SchemaMigrationTest.java` (create)

**Interfaces:**
- Consumes: V2's `users` and `revoked_token` tables.
- Produces: table `app_user` (username-unique); `revoked_token.user_id BIGINT` (nullable FK `fk_revoked_token_user` → `app_user(id)`) and index `idx_revoked_token_expires_at`; `SchemaMigrationTest.read(file)` helper all later tasks reuse.

- [ ] **Step 1: Write the failing test**

Create `hu.elte.ntb.typeshift.config.SchemaMigrationTest` next to the existing `FlywayConfigTest`, with helper `read`:

```java
  private static String read(String migration) throws IOException {
    ClassPathResource resource = new ClassPathResource("db/migration/" + migration);
    try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
      return new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
    }
  }
```

Tests:

```java
  @Test
  void userSchemaMatchesStory11() throws IOException {
    String sql = read("V3__app_user_schema.sql");
    assertThat(sql).contains("ALTER TABLE users RENAME TO app_user");
    assertThat(sql).contains("ADD COLUMN user_id BIGINT");
    assertThat(sql).contains("CREATE INDEX idx_revoked_token_expires_at ON revoked_token (expires_at)");
  }

  @Test
  void userAccountMapsAppUser() {
    assertThat(UserAccount.class.getAnnotation(Table.class).name()).isEqualTo("app_user");
  }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl typeshift-backend test -Dtest=SchemaMigrationTest`
Expected: FAIL — V3 does not exist; `@Table` name is `users`.

- [ ] **Step 3: Implement**

`V3__app_user_schema.sql`:

```sql
-- Story 1.1: users becomes app_user; revoked_token gains user_id and purge index.
ALTER TABLE users RENAME TO app_user;
ALTER TABLE app_user RENAME CONSTRAINT uk_users_username TO uk_app_user_username;

ALTER TABLE revoked_token ADD COLUMN user_id BIGINT;
ALTER TABLE revoked_token
    ADD CONSTRAINT fk_revoked_token_user FOREIGN KEY (user_id) REFERENCES app_user (id);
CREATE INDEX idx_revoked_token_expires_at ON revoked_token (expires_at);
```

(`user_id` stays unmapped in `RevokedToken` — deliberately unpopulated, see gaps file.)

`UserAccount.java:20`: `@Table(name = "app_user")`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl typeshift-backend verify` — full gate green (unrenamed callers compile unchanged; SQLite `create-drop` tests see the rename transparently).

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/db/migration/V3__app_user_schema.sql \
  typeshift-backend/src/main/java/hu/elte/ntb/typeshift/account/model/UserAccount.java \
  typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/SchemaMigrationTest.java
git commit -m "feat(schema): app_user table and revoked-token user index (story 1.1)"
```

---

### Task 2: `learning_profile` alignment (Story 1.2)

**Files:**
- Create: `typeshift-backend/src/main/resources/db/migration/V4__learning_profile_schema.sql`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/learning/model/LearningProfile.java`
- Test: `SchemaMigrationTest` + `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/learning/service/LearningProfileServiceTest.java`

**Interfaces:**
- Consumes: V2's `learning_profiles` / `learning_profile_unlocked_stages`; Task 1's `read` helper.
- Produces: table `learning_profile` with `keyboard_layout` and `updated_at`; entity `LearningProfile#getKeyboardLayout()` / `#getUpdatedAt()`; plus NOT NULL/range/unique constraints on `learning_profile_unlocked_stages.stage`.

- [ ] **Step 1: Write the failing test**

Add to `SchemaMigrationTest`:

```java
  @Test
  void learningProfileSchemaMatchesStory12() throws IOException {
    String sql = read("V4__learning_profile_schema.sql");
    assertThat(sql).contains("ALTER TABLE learning_profiles RENAME TO learning_profile");
    assertThat(sql).contains("ADD COLUMN keyboard_layout");
    assertThat(sql).contains("CHECK (current_stage BETWEEN 1 AND 5)");
    assertThat(sql).contains("CREATE UNIQUE INDEX");
  }
```

Add to `LearningProfileServiceTest.initializeForUsesStageOneDefaults`:

```java
    assertThat(profile.getKeyboardLayout()).isEqualTo("HU_QWERTZ");
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl typeshift-backend test -Dtest='SchemaMigrationTest,LearningProfileServiceTest'`
Expected: FAIL — V4 missing, no `getKeyboardLayout()`.

- [ ] **Step 3: Implement**

`V4__learning_profile_schema.sql`:

```sql
-- Story 1.2: learning_profiles becomes learning_profile; keyboard layout and
-- updated_at columns plus one-profile/stage-range guarantees.
ALTER TABLE learning_profiles RENAME TO learning_profile;
ALTER TABLE learning_profile
    RENAME CONSTRAINT uk_learning_profiles_user TO uk_learning_profile_user;

ALTER TABLE learning_profile
    ADD COLUMN keyboard_layout VARCHAR(16) NOT NULL DEFAULT 'HU_QWERTZ';
ALTER TABLE learning_profile
    ADD COLUMN updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE learning_profile
    ADD CONSTRAINT ck_learning_profile_current_stage CHECK (current_stage BETWEEN 1 AND 5);

ALTER TABLE learning_profile_unlocked_stages ALTER COLUMN stage SET NOT NULL;
ALTER TABLE learning_profile_unlocked_stages
    ADD CONSTRAINT ck_unlocked_stage_range CHECK (stage BETWEEN 1 AND 5);
CREATE UNIQUE INDEX uk_learning_profile_unlocked_stage
    ON learning_profile_unlocked_stages (learning_profile_id, stage);
```

(`unlocked_stages` stays the reference table — existing decision; "is stage N unlocked" is `EXISTS` in one query.)

`LearningProfile.java`: `@Table(name = "learning_profile")`; add two mapped fields following the entity's existing style:

```java
  @Column(nullable = false)
  private String keyboardLayout = "HU_QWERTZ";

  @Column(nullable = false)
  private Instant updatedAt;
```

Constructor sets `this.updatedAt = Instant.now();`; add `@PreUpdate void onUpdate() { updatedAt = Instant.now(); }` (mirrors `UserAccount`), imports `jakarta.persistence.PreUpdate`.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl typeshift-backend verify` — green.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/db/migration/V4__learning_profile_schema.sql \
  typeshift-backend/src/main/java/hu/elte/ntb/typeshift/learning/model/LearningProfile.java \
  typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/SchemaMigrationTest.java \
  typeshift-backend/src/test/java/hu/elte/ntb/typeshift/learning/service/LearningProfileServiceTest.java
git commit -m "feat(schema): learning_profile alignment with keyboard layout (story 1.2)"
```

---

### Task 3: Stage reference table (Story 1.3)

**Files:**
- Create: `typeshift-backend/src/main/resources/db/migration/V5__stage_reference.sql`
- Test: `SchemaMigrationTest`

**Interfaces:**
- Consumes: nothing (reference data).
- Produces: `stage (id BIGINT PK CHECK 1–5, name VARCHAR(64) NOT NULL, sub_exam_count INTEGER NOT NULL, graduation_name VARCHAR(128) NOT NULL)`; seeded ids used as FK targets by Tasks 4–7. No entities in this epic.

- [ ] **Step 1: Write the failing test**

```java
  @Test
  void stageSchemaMatchesStory13() throws IOException {
    String sql = read("V5__stage_reference.sql");
    assertThat(sql).contains("CREATE TABLE stage (");
    assertThat(sql).contains("id BIGINT");
    assertThat(sql).contains("INSERT INTO stage");
    assertThat(sql).contains("Numpad and Math");
    assertThat(sql).contains("Hungarian Accents");
  }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl typeshift-backend test -Dtest=SchemaMigrationTest`
Expected: FAIL — V5 missing.

- [ ] **Step 3: Implement**

`V5__stage_reference.sql` — seed values copied verbatim from `openspec/constants/stage-map.md` / ADR-013:

```sql
-- Story 1.3: static stage reference data (ADR-013). Seeded by migration only;
-- no application path mutates it, and NO ACTION FKs protect the rows.
CREATE TABLE stage (
    id              BIGINT PRIMARY KEY CHECK (id BETWEEN 1 AND 5),
    name            VARCHAR(64)  NOT NULL,
    sub_exam_count  INTEGER      NOT NULL CHECK (sub_exam_count > 0),
    graduation_name VARCHAR(128) NOT NULL
);

INSERT INTO stage (id, name, sub_exam_count, graduation_name) VALUES
    (1, 'QWERTZ Core',      3, '3-minute QWERTZ passage'),
    (2, 'Symbol Sorcerer',  2, 'Technical support ticket'),
    (3, 'Numpad and Math',  2, 'Cashier Rush'),
    (4, 'Hungarian Accents',2, 'Formal Hungarian request letter'),
    (5, 'Real-World Tasks', 4, 'Three random tasks in sequence');
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl typeshift-backend verify` — green.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/db/migration/V5__stage_reference.sql
git commit -m "feat(schema): seeded stage reference table (story 1.3)"
```

---

### Task 4: Practice session summary (Story 1.4)

**Files:**
- Create: `typeshift-backend/src/main/resources/db/migration/V6__practice_session.sql`
- Test: `SchemaMigrationTest`

**Interfaces:**
- Consumes: `app_user`, `stage` (Tasks 1–3).
- Produces: `practice_session`; PK/FK/columns read by epic 4 story 4.4 (writer) and epic 7 story 7.1 (chart over `user_id, submitted_at`).

- [ ] **Step 1: Write the failing test**

```java
  @Test
  void practiceSessionSchemaMatchesStory14() throws IOException {
    String sql = read("V6__practice_session.sql");
    assertThat(sql).contains("CREATE TABLE practice_session (");
    assertThat(sql).contains("exercise_kind");
    assertThat(sql).contains("CREATE INDEX idx_practice_session_user_submitted");
  }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl typeshift-backend test -Dtest=SchemaMigrationTest`
Expected: FAIL — V6 missing.

- [ ] **Step 3: Implement**

`V6__practice_session.sql` — one row per submitted session (ADR-002/011 honor client numbers), no keystroke rows (ADR-003):

```sql
-- Story 1.4: written once per practice session at submission; speed numbers
-- are the client-computed summary of ADR-002, advisory only.
CREATE TABLE practice_session (
    id                 BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    stage_id           BIGINT NOT NULL,
    exercise_kind      VARCHAR(8)     NOT NULL,
    wpm                NUMERIC(6, 2)  NOT NULL CHECK (wpm >= 0),
    cpm                NUMERIC(6, 2)  NOT NULL CHECK (cpm >= 0),
    correct_characters INTEGER        NOT NULL CHECK (correct_characters >= 0),
    total_characters   INTEGER        NOT NULL CHECK (total_characters >= 0),
    duration_seconds   INTEGER        NOT NULL CHECK (duration_seconds >= 0),
    submitted_at       TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_practice_session_user  FOREIGN KEY (user_id)  REFERENCES app_user (id),
    CONSTRAINT fk_practice_session_stage FOREIGN KEY (stage_id) REFERENCES stage (id),
    CONSTRAINT ck_practice_session_kind CHECK (exercise_kind IN ('PROSE', 'MATH'))
);

CREATE INDEX idx_practice_session_user_submitted
    ON practice_session (user_id, submitted_at DESC);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl typeshift-backend verify` — green.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/db/migration/V6__practice_session.sql
git commit -m "feat(schema): practice session summary table (story 1.4)"
```

---

### Task 5: Score record (Story 1.5)

**Files:**
- Create: `typeshift-backend/src/main/resources/db/migration/V7__score_record.sql`
- Test: `SchemaMigrationTest`

**Interfaces:**
- Consumes: `app_user`, `stage`.
- Produces: `score_record` with the pass flag stored at write time (ADR-016); PK/FK/columns used by epics 2 (story 2.1 writer), 6 (story 6.2), 7 (stories 7.3/7.4 readers). Task 7 extends it.

- [ ] **Step 1: Write the failing test**

```java
  @Test
  void scoreRecordSchemaMatchesStory15() throws IOException {
    String sql = read("V7__score_record.sql");
    assertThat(sql).contains("CREATE TABLE score_record (");
    assertThat(sql).contains("attempt_kind");
    assertThat(sql).contains("CHECK ((attempt_kind = 'SUB_EXAM' AND sub_exam_number IS NOT NULL)"
        + " OR (attempt_kind = 'GRADUATION' AND sub_exam_number IS NULL))");
  }
```

(Exact CHECK text may wrap differently inside the migration file — assert on the two key fragments `"attempt_kind = 'SUB_EXAM' AND sub_exam_number IS NOT NULL"` and `"attempt_kind = 'GRADUATION' AND sub_exam_number IS NULL"` if the whole-constraint string is brittle.)

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl typeshift-backend test -Dtest=SchemaMigrationTest`
Expected: FAIL — V7 missing.

- [ ] **Step 3: Implement**

`V7__score_record.sql` — exercise text is not stored (ADR-015); edit-distance parts per ADR-016; `passed` snapshot sits in the row so history stays honest (story 2.4 re-checks a retake):

```sql
-- Story 1.5: one row per submitted sub-exam or graduation attempt. Accuracy is
-- the ADR-016 Wagner-Fischer fraction (0..1); passed snapshots accuracy >= 0.95
-- at write time.
CREATE TABLE score_record (
    id               BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    stage_id         BIGINT NOT NULL,
    attempt_kind     VARCHAR(16)   NOT NULL,
    sub_exam_number  INTEGER       NULL CHECK (sub_exam_number IS NULL OR sub_exam_number BETWEEN 1 AND 4),
    accuracy         NUMERIC(5, 4) NOT NULL CHECK (accuracy >= 0 AND accuracy <= 1),
    substitutions    INTEGER       NOT NULL CHECK (substitutions >= 0),
    insertions       INTEGER       NOT NULL CHECK (insertions >= 0),
    deletions        INTEGER       NOT NULL CHECK (deletions >= 0),
    passed           BOOLEAN       NOT NULL,
    submitted_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_score_record_user  FOREIGN KEY (user_id)  REFERENCES app_user (id),
    CONSTRAINT fk_score_record_stage FOREIGN KEY (stage_id) REFERENCES stage (id),
    CONSTRAINT ck_score_record_kind CHECK (attempt_kind IN ('SUB_EXAM', 'GRADUATION')),
    CONSTRAINT ck_score_record_sub_exam CHECK (
        (attempt_kind = 'SUB_EXAM' AND sub_exam_number IS NOT NULL)
        OR (attempt_kind = 'GRADUATION' AND sub_exam_number IS NULL))
);

CREATE INDEX idx_score_record_user_submitted
    ON score_record (user_id, submitted_at DESC);
```

(The index is not an AC of 1.5 — same read shape as 1.4 serves stories 7.3/7.4; noted in the gaps file.)

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl typeshift-backend verify` — green.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/db/migration/V7__score_record.sql
git commit -m "feat(schema): score record table with pass snapshot (story 1.5)"
```

---

### Task 6: Weak key profile (Story 1.6)

**Files:**
- Create: `typeshift-backend/src/main/resources/db/migration/V8__weak_key_profile.sql`
- Test: `SchemaMigrationTest`

**Interfaces:**
- Consumes: `app_user`.
- Produces: `weak_key_profile` with composite PK `(user_id, key_char)` — the conflict target of the Story 4.5 cumulative merge — and index `idx_weak_key_user_miss` for ADR-012's top-5 read.

- [ ] **Step 1: Write the failing test**

```java
  @Test
  void weakKeySchemaMatchesStory16() throws IOException {
    String sql = read("V8__weak_key_profile.sql");
    assertThat(sql).contains("CREATE TABLE weak_key_profile (");
    assertThat(sql).contains("PRIMARY KEY (user_id, key_char)");
    assertThat(sql).contains("CREATE INDEX idx_weak_key_user_miss");
  }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl typeshift-backend test -Dtest=SchemaMigrationTest`
Expected: FAIL — V8 missing.

- [ ] **Step 3: Implement**

`V8__weak_key_profile.sql`:

```sql
-- Story 1.6: cumulative per-character miss counts (ADR-003 merge -> Story 4.5).
-- key_char uses VARCHAR(4): decomposed Unicode forms (e + combining mark) exceed
-- one code point though ADR-003 keys are conceptually single characters.
CREATE TABLE weak_key_profile (
    user_id    BIGINT                              NOT NULL,
    key_char   VARCHAR(4)                          NOT NULL,
    miss_count INTEGER                             NOT NULL CHECK (miss_count >= 0),
    updated_at TIMESTAMP(6) WITH TIME ZONE         NOT NULL,
    PRIMARY KEY (user_id, key_char),
    CONSTRAINT fk_weak_key_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_weak_key_user_miss
    ON weak_key_profile (user_id, miss_count DESC);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl typeshift-backend verify` — green.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/db/migration/V8__weak_key_profile.sql
git commit -m "feat(schema): weak key profile table (story 1.6)"
```

---

### Task 7: Graduation attempt (Story 1.7)

**Files:**
- Create: `typeshift-backend/src/main/resources/db/migration/V9__graduation_attempt.sql`
- Test: `SchemaMigrationTest`

**Interfaces:**
- Consumes: `app_user`, `stage`, `score_record` (Task 5).
- Produces: `graduation_attempt` (retakes accumulate, never overwritten) and `score_record.graduation_attempt_id` — used by epics 2 (stories 2.3/2.4) and 7 (story 7.5).

- [ ] **Step 1: Write the failing test**

```java
  @Test
  void graduationAttemptSchemaMatchesStory17() throws IOException {
    String sql = read("V9__graduation_attempt.sql");
    assertThat(sql).contains("CREATE TABLE graduation_attempt (");
    assertThat(sql).contains("'IN_PROGRESS', 'PASSED', 'FAILED'");
    assertThat(sql).contains("ADD COLUMN graduation_attempt_id");
    assertThat(sql).contains("graduation_attempt_id IS NULL OR attempt_kind = 'GRADUATION'");
  }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -pl typeshift-backend test -Dtest=SchemaMigrationTest`
Expected: FAIL — V9 missing.

- [ ] **Step 3: Implement**

`V9__graduation_attempt.sql` — unlock state deliberately stays out (lives in `learning_profile`); `passed` snapshots the close state beside `status` per the story:

```sql
-- Story 1.7: one row per graduation attempt; retakes accumulate, history is
-- never overwritten. Component sub-exam/graduation score rows link back via
-- score_record.graduation_attempt_id (stage 5 has three).
CREATE TABLE graduation_attempt (
    id          BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    stage_id    BIGINT NOT NULL,
    status      VARCHAR(16)                 NOT NULL,
    started_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP(6) WITH TIME ZONE NULL,
    passed      BOOLEAN                     NOT NULL,
    CONSTRAINT fk_graduation_attempt_user  FOREIGN KEY (user_id)  REFERENCES app_user (id),
    CONSTRAINT fk_graduation_attempt_stage FOREIGN KEY (stage_id) REFERENCES stage (id),
    CONSTRAINT ck_graduation_attempt_status CHECK (status IN ('IN_PROGRESS', 'PASSED', 'FAILED')),
    CONSTRAINT ck_graduation_attempt_closed CHECK (
        (status = 'IN_PROGRESS' AND finished_at IS NULL)
        OR (status <> 'IN_PROGRESS' AND finished_at IS NOT NULL))
);

ALTER TABLE score_record ADD COLUMN graduation_attempt_id BIGINT;
ALTER TABLE score_record
    ADD CONSTRAINT fk_score_record_graduation
    FOREIGN KEY (graduation_attempt_id) REFERENCES graduation_attempt (id);
ALTER TABLE score_record
    ADD CONSTRAINT ck_score_record_graduation_link
    CHECK (graduation_attempt_id IS NULL OR attempt_kind = 'GRADUATION');
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -pl typeshift-backend verify` — green.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/resources/db/migration/V9__graduation_attempt.sql
git commit -m "feat(schema): graduation attempt table and score links (story 1.7)"
```

---

### Task 8: Apply all migrations to real Postgres (manual, per 0b-Q6 precedent)

**Files:**
- Modify: `docs/database.md` (append the epic-1 verification commands)

**Interfaces:**
- Consumes: all of V3–V9, Docker Compose Postgres (Task 1 of the 0b plan), dev profile.
- Produces: a verified-in-place schema; `flyway_schema_history` containing exactly rows 1–9.

- [ ] **Step 1: Upgraded-database path**

Start the stack, boot once so Flyway applies V3–V9 on the **existing** volume, then inspect:

```bash
docker compose up -d
mvn -pl typeshift-backend spring-boot:run -Dspring-boot.run.profiles=dev
# Ctrl-C after startup completes
docker compose exec db psql -U typeshift -d typeshift \
  -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank" \
  -c "\dt" \
  -c "SELECT id, name, sub_exam_count FROM stage ORDER BY id"
```

Expected: 9 rows version 1–9 all `success`; tables `app_user, learning_profile, revoked_token, learning_profile_unlocked_stages, stage, practice_session, score_record, weak_key_profile, graduation_attempt`; five stage rows with counts 3,2,2,2,4.

- [ ] **Step 2: Fresh-database path**

Wipe and apply from scratch, confirming `validate` and all constraints:

```bash
docker compose down -v
docker compose up -d
mvn -pl typeshift-backend spring-boot:run -Dspring-boot.run.profiles=dev
# Ctrl-C after startup completes
docker compose exec db psql -U typeshift -d typeshift \
  -c "\d weak_key_profile" \
  -c "\d score_record" \
  -c "SELECT conname FROM pg_constraint WHERE conrelid = 'graduation_attempt'::regclass"
```

Expected: boot completes with no validation error; composite PK `(user_id, key_char)` present; CHECKs `attempt_kind/type`, `status`, link, and 1–5 stage ranges visible.

- [ ] **Step 3: Commit**

```bash
git add docs/database.md
git commit -m "docs: epic 1 schema verification commands"
```

(If `docs/database.md` already documents this flow, skip the edit and this commit.)
