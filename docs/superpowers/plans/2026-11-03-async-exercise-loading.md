# Async Exercise Loading Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Render the exercise page shell instantly and load the generated docket asynchronously via htmx once the LLM responds.

**Architecture:** `GET /exercise/{stageId}/{examKey}` renders the shell (readout bar, notices, loading placeholder, notes) without calling generation. A new `GET /exercise/{stageId}/{examKey}/docket` generates and returns the Thymeleaf fragment `exercise :: docket` (typing donut, keypad/textarea, task class, token form). The placeholder fetches the fragment with `hx-get ... hx-trigger="load"`; `engine.js` initializes on `htmx:afterSwap`; `htmx:responseError` swaps in an inline error with a Retry button.

**Tech Stack:** Spring MVC + Thymeleaf fragments, htmx (already in layout), vanilla JS, JUnit 5 + MockMvc, Maven.

**Spec:** `docs/superpowers/specs/2026-11-03-async-exercise-loading-design.md`

## Global Constraints

- No changes to generation logic, validation, tickets, submission, catalog, task-type selection, or provider timeout settings.
- No new dependencies.
- Unknown stage/exam must still 404 on the shell GET (cheap catalog check, no LLM call).
- Validation at trust boundaries is preserved: `submit` still verifies the ticket, unchanged.
- Keep the session precedent: changes stay uncommitted on `main`; no commit steps.

## Review Focus

- Provider failure during the docket GET (e.g. `ConnectTimeoutException`) → docket endpoint 500s; the expected behavior is an inline, retryable error on the exercise page — frontend `htmx:responseError` handler, not covered by backend tests (manual/UI check).
- Unauthenticated docket GET → login redirect, same rule as the page route (pinned by test in Task 1).
- Unknown stage or exam key on the docket GET → 404, not a 500 (pinned by tests in Task 1).
- `engine.js` init must not double-initialize when the shell is re-rendered or the event fires for other swaps: guard on the docket being present and not already initialized (guarded in Task 1 implementation; behavior-only).
- `htmx:afterSwap` also fires for other swaps on other pages: the guard above keeps this inert everywhere else (same guard).

---

### Task 1: Split the exercise page into shell + docket fragment with async loading

**Files:**
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseControllerTest.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseController.java`
- Modify: `typeshift-frontend/src/main/resources/templates/exercise.html`
- Modify: `typeshift-frontend/src/main/resources/static/js/engine.js`
- Modify: `typeshift-frontend/src/main/resources/messages.properties` (+ `messages_hu.properties`, `messages_vi.properties`)

**Interfaces:**
- Consumes: existing `ExerciseGenerationService.generate(String username, CannedExercise definition)`, `ExerciseTicketService.issue(String username, CannedExercise exercise)`, and `CannedCatalog.find(int, String)`.
- Produces: `GET /exercise/{stageId}/{examKey}/docket` returning view name `"exercise :: docket"`; model attrs `docketLabel`, `docketFraming`, `dataTarget`, `dataExpected`, `dataMode`, `taskClass`, `exerciseToken`, `stageId`, `examKey` inside that fragment.

- [ ] **Step 1: Rewrite the controller tests for the async flow**

In `ProseExerciseControllerTest`:
- Change `proseDocketServesStageFourText` to: GET `/exercise/4/exam-1` returns 200, contains `hx-get="/exercise/4/exam-1/docket"` and the loading placeholder, does NOT contain `data-target`, and `verify(generation, never()).generate(anyString(), any(CannedExercise.class))`.
- Keep `proseDocketServesTargetText` assertions but move them onto the docket route. Rename to `docketFragmentServesGeneratedText`: GET `/exercise/4/exam-1/docket` (same session cookie) asserts as before (`data-target="A hivatalos iratokat`, `data-mode="prose"`, no `data-expected`, textarea, "New copy", "Save result", ticket-bearing form) plus numeric/format cases (`/exercise/2/grad/docket` → `Tárgy: Bejelentkezési hiba`; `/exercise/5/exam-1/docket` → `docket--chat-burst`), and `verify(generation).generate(OWNER, ...)` once.
- `numericDocketServesKeypad`, `numericDocketServesPadAndVerdict`, `proseDocketOmitsKeyPad`, `graduationDocketServes`: point these at the docket route.
- Add `unknownStageOnDocketGivesNotFound` and `unknownExamKeyOnDocketGivesNotFound` (404 on the docket route).
- Add `unauthenticatedDocketRequestIsRebuffed` (docket GET without cookie → 3xx redirect to `/login?auth=MISSING`).
- Add `unknownStageGivesNotFound`/`unknownExamKeyGivesNotFound` still pass against the shell route unchanged.

- [ ] **Step 2: Run the focused tests and verify they fail**

Run: `mvn -pl typeshift-backend -am -Dtest=ProseExerciseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL — the shell still renders `data-target`/generated text and `/docket` routes do not exist.

- [ ] **Step 3: Implement the controller split**

In `ProseExerciseController`:
- `lookup(...)`: keep catalog lookup (404 on unknown), drop `generation`/`tickets` use; set `title.exercise`, `contentFragment=exercise`, `stageId`, `examKey`; return `layout`.
- New `docket(int stageId, String examKey, Principal principal, Model model)`: catalog lookup → 404; generate via `generation.generate(principal.getName(), definition)`; add the model attrs named in Interfaces (reuse the existing `docketLabel`, `framing`, `taskClass` helpers); return `"exercise :: docket"`.
- Keep `submit`, helpers, and `redirectToAssignment` unchanged.

- [ ] **Step 4: Restructure `exercise.html` into shell + `docket` fragment with htmx loading**

- Wrap the current docket markup (docket head through the docket foot, plus the submission form) as `<div th:fragment="docket" ...>` served by the docket route; the fragment carries `data-target/data-mode/data-expected`, keypad/textarea, token input.
- In the shell, replace that block with the loading placeholder: a card with `id="exercise-loading"`, `hx-get="@{/exercise/{stageId}/{examKey}/docket(stageId=${stageId},examKey=${examKey})}"`, `hx-trigger="load"`, `hx-target="this"`, `hx-swap="outerHTML"`, showing localized "Generating exercise…" text.
- Add an inline `<script>` on the page: on `htmx:responseError`, replace the placeholder with an error card showing a localized failure message and a Retry button whose hx-get/hx-target/hx-swap mirror the placeholder's.

- [ ] **Step 5: Teach `engine.js` to initialize after the swap**

- Extract the current immediate-init tail (`if (docket.dataset.mode === 'numeric') initNumeric(); else initProse();`) into an `initialize()` guarded by an `initialized` flag; keep the immediate call when the docket is server-rendered; call `initialize()` from a `document`-level `htmx:afterSwap` listener when `#typing-docket` is present.
- Keep the Escape/leave handler as is.

- [ ] **Step 6: Add i18n keys**

Add to all three message bundles: `exercise.generating` ("Generating exercise…"), `exercise.generationFailed` ("The exercise could not be generated. Try again."), `exercise.retry` ("Retry") — with Hungarian and Vietnamese translations consistent with the existing tone.

- [ ] **Step 7: Run focused tests and verify they pass**

Run: `mvn -pl typeshift-backend -am -Dtest=ProseExerciseControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS.

- [ ] **Step 8: Run full verification**

Run: `mvn -pl typeshift-backend -am verify`
Expected: BUILD SUCCESS — all tests pass, Checkstyle 0 violations, PMD clean, SpotBugs 0 bugs.

- [ ] **Step 9: Manual UI check**

Start the backend and load `/exercise/4/exam-1` in a browser: shell appears immediately, docket swaps in when content is ready, and (with the LLM unreachable) the inline error with Retry appears instead of a raw 500.
