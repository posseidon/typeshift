# Database-Backed Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace fabricated dashboard metrics and statuses with owner-scoped values from existing database tables, showing explicit empty states when no rows exist.

**Architecture:** `DashboardController` passes the authenticated username to a read-only `DashboardService`, which queries profile, stage, session, score, and weak-key data with Spring JDBC. A `DashboardSnapshot` groups values for Thymeleaf; graduation is never inferred from current stage alone.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring JDBC/JdbcTemplate (already available), Thymeleaf, SQLite-backed MockMvc tests.

**Spec:** `docs/superpowers/specs/2026-10-31-dashboard-real-data-design.md`.

## Global Constraints

- Read-only dashboard change: no migrations, demo data, score/session write flows, or progression writes.
- Queries are scoped by authenticated username/user id; `/` remains behind the existing JWT filter.
- Profile values come from `learning_profile`; labels/counts from `stage`; sessions from `practice_session`; accuracy/verdicts from `score_record`; misses from `weak_key_profile`.
- A station is “Graduated” only if a passing `GRADUATION` score exists; otherwise status precedence is `CURRENT`, then `UNLOCKED`, then `LOCKED`. Current/unlocked states come from `current_stage` and `learning_profile_unlocked_stages`.
- If an older account has no `learning_profile`, use the existing profile behavior's stage-1 / zero-practice-minutes fallback and treat stage 1 as unlocked.
- Empty results render explicit copy; never substitute canned dates, measurements, attempts, or misses.
- No new dependency. `mvn verify` must pass all tests, Checkstyle, PMD, and SpotBugs.

## Review Focus

1. **Cross-user data leak** — assert a second user's rows never appear in the requesting owner's dashboard (Task 1 seeded MockMvc test).
2. **Empty tables** — assert explicit empty states and absence of prior fake values (`68`, `97.4%`, `1958`, `CH-882`, `01-992`) (Task 1 empty-state test).
3. **Current stage mistaken for graduation** — assert stages are not “Graduated” without a passing graduation score and become so only with one (Task 1 status test).
4. **Accuracy fraction rendered incorrectly** — assert stored `0.974` displays as `97.4%`, and null accuracy is not shown as `0.0%` (Task 1 seeded/empty tests).
5. **History order/limit** — insert >7 sessions out of order and assert the seven newest display chronologically (Task 1 seeded-data test).

---

### Task 1: Database-backed dashboard snapshot and page

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/dashboard/model/DashboardSnapshot.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/dashboard/service/DashboardService.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/dashboard/controller/DashboardController.java`
- Modify: `typeshift-frontend/src/main/resources/templates/index.html`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/dashboard/controller/DashboardControllerTest.java`

**Interfaces:**
- Consumes: `Principal#getName()`; V4 `learning_profile`/`learning_profile_unlocked_stages`; V5 `stage`; V6 `practice_session`; V7/V9/V10 `score_record`; V8 `weak_key_profile`.
- Produces: `DashboardService#loadFor(String username) -> DashboardSnapshot`.
- `DashboardSnapshot` fields: `String displayName`, `int currentStage`, `String currentStageName`, `int totalPracticeMinutes`, `long practiceSessionCount`, nullable `BigDecimal latestWpm`, nullable `BigDecimal averageAccuracyPercent`, `long passedSubExams`, `int currentStageSubExamCount`, `List<StageProgress> stages`, `List<WpmPoint> wpmHistory`, `List<WeakKey> weakKeys`, `List<RecentScore> recentScores`.
- Nested records: `StageProgress(long stageId, String name, String status, int passedSubExams, int subExamCount)` (`GRADUATED`, `CURRENT`, `UNLOCKED`, `LOCKED`); `WpmPoint(String submittedAt, BigDecimal wpm)`; `WeakKey(String keyChar, int missCount)`; `RecentScore(String submittedAt, String stageName, String examLabel, BigDecimal accuracyPercent, boolean passed)`.

- [ ] **Step 1: Write failing MockMvc tests**

Create `DashboardControllerTest` following `StageMapControllerTest`: `@SpringBootTest`, `@AutoConfigureMockMvc`, SQLite `create-drop`, Flyway disabled, fixed JWT secret, seeded account and session cookie. Autowire `JdbcTemplate`; create SQLite-compatible test tables with migration columns for `stage (id, name, sub_exam_count, graduation_name)`, `practice_session (id, user_id, stage_id, wpm, submitted_at, plus remaining required columns)`, `graduation_attempt (id, user_id, stage_id, status, started_at, finished_at, passed)`, `score_record (id, user_id, stage_id, attempt_kind, sub_exam_number, accuracy, passed, submitted_at, graduation_attempt_id)`, and `weak_key_profile (user_id, key_char, miss_count, updated_at)`. Seed V5's five stage rows and clear owner data before each case. Seed linked graduation rows with a matching `graduation_attempt_id` to honor V10's constraint.

Add tests:

```java
@Test void emptyDatabaseShowsHonestDashboard()
```
`GET /` returns 200 for an account with no practice/score/weak-key rows; body contains “No practice sessions yet”, “No score attempts yet”, “No weak-key data yet”, “No WPM history yet”, stage 1 and zero practice minutes; body contains no plotted WPM points and omits representative fake values/references (`68`, `97.4%`, `142`, `1958`, `TS-40892`, `#HU-40892`, `CH-882`, `01-992`, `65%`, `3/5 PASS`). Also verify stage-1 fallback when the account has no learning profile.

```java
@Test void dashboardShowsOnlyCurrentOwnersPersistedMetricsAndRows()
```
Insert owner rows for sessions, scores (including accuracy `0.974`), and weak keys; add unique-valued rows for a second account. Assert actual WPM, `97.4%`, session count, passed sub-exam progress, top key/count, recent scores, and absence of the second account's values. Insert eight sessions with distinct timestamps; assert only seven appear and in chronological display order.

```java
@Test void stageIsGraduatedOnlyWhenPassingGraduationScoreExists()
```
Set current stage 3 and stages 1–3 unlocked without a passing graduation score; assert prior stages are not “Graduated”/“Approved”. Insert a passing graduation score for stage 1; assert only stage 1 becomes graduated.

```java
@Test void dashboardRequiresAuthentication()
```
Without the session cookie, `GET /` redirects to `/login?auth=MISSING`.

- [ ] **Step 2: Run focused tests to verify RED**

Run: `mvn -q -pl typeshift-backend -am test -Dtest=DashboardControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL because the controller/template still serve hardcoded values and the dashboard service/model do not exist.

- [ ] **Step 3: Implement `DashboardSnapshot` and `DashboardService#loadFor(String username)`**

Use parameterized `JdbcTemplate` queries, resolving `app_user.id` from username. Read profile/stage/unlocked stages; count sessions and find latest WPM; average score accuracy as percentage; count distinct passed sub-exam numbers for the current stage; identify stages with passing graduation rows; fetch newest 7 sessions (reverse to chronological display), newest 5 score rows, and top 5 weak keys by miss count. Label a score row `Graduation` when `attempt_kind='GRADUATION'`, otherwise `Sub-exam {sub_exam_number}`. Use null for single-value metrics with no matching rows. If the profile is absent, default to stage 1, 0 minutes, and unlocked stage 1. Determine status in this order: passing graduation score → `GRADUATED`; current stage → `CURRENT`; unlocked stage → `UNLOCKED`; otherwise → `LOCKED`. Keep SQL compatible with SQLite and PostgreSQL; do not add dependencies or migrations.

- [ ] **Step 4: Wire the route**

Change `DashboardController#index` to accept `Principal`, call `dashboardService.loadFor(principal.getName())`, and add the result to the model as `dashboard`. Keep title, `contentFragment="index"`, and `layout` return.

- [ ] **Step 5: Replace fabricated template content**

Bind `index.html` to `dashboard`: remove fake docket IDs, fixed KPIs, SVG points/dates, station approvals/progress, miss counts, and sample ledger rows. Render nullable metrics only when present; use the specified empty-state copy otherwise. Render stages, up to seven actual WPM points (none when empty), top weak keys, and recent scores from their lists. Preserve only copy that makes no unsupported user-data claim.

- [ ] **Step 6: Run focused tests to verify GREEN**

Run: `mvn -q -pl typeshift-backend -am test -Dtest=DashboardControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS, including all four test cases above.

- [ ] **Step 7: Run the full quality gate**

Run: `mvn -q verify`
Expected: exit 0; tests, Checkstyle, PMD, and SpotBugs pass.

- [ ] **Step 8: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/dashboard \
  typeshift-backend/src/test/java/hu/elte/ntb/typeshift/dashboard \
  typeshift-frontend/src/main/resources/templates/index.html
git commit -m "feat: render dashboard from persisted user data"
```
