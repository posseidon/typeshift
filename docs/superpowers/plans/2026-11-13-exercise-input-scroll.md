# Exercise input scroll Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Stop the hidden typing textarea from pulling the page upward while keeping the visible typing cursor in view.

**Architecture:** Keep the existing page-follow design. Pin the hidden textarea to the viewport, focus it with `preventScroll`, and use non-animated cursor scrolling to avoid competing scroll operations. Keep both resource copies aligned.

**Tech Stack:** Plain JavaScript, CSS, Node.js built-in test runner, Maven.

**Spec:** `docs/superpowers/specs/2026-11-13-exercise-input-scroll-design.md`

## Global Constraints

- No new dependencies.
- Preserve current exercise layout and typing/scoring behavior.
- Apply behavior consistently to both checked-in `engine.js` copies.

## Review Focus

- Focusing the hidden input after the page has scrolled must not move the viewport; regression test asserts focus requests `preventScroll`.
- Typing repeatedly must not restart smooth scroll animations; regression test asserts cursor scrolling is non-animated.
- Hidden input must remain fixed to viewport rather than docket position; inspect both CSS copies during final diff review because no browser runner is installed.
- Docket initialization and typed-character rendering must remain unchanged; run Java test suite.
- Duplicate backend resource must match primary scrolling behavior; run same Node regression against both engine files.

---

### Task 1: Fix input and cursor scroll anchoring (test-first)

**Files:**
- Create: `src/test/js/engine-scroll.test.js`
- Modify: `src/main/resources/static/js/engine.js`
- Modify: `typeshift-backend/src/main/resources/static/js/engine.js`
- Modify: `src/main/resources/static/css/style.css`
- Modify: `typeshift-backend/src/main/resources/static/css/style.css`

**Interfaces:**
- Consumes: Existing engine DOM IDs and event flow.
- Produces: A Node built-in test that verifies focus uses `preventScroll` and cursor scrolling is not smooth.

- [x] **Step 1: Write test `keeps_page_scroll_anchored_to_visible_cursor`** using a minimal DOM stub to evaluate each engine and trigger the container click/input events; assert `focus` receives `preventScroll: true` and cursor scroll options are not smooth.
- [x] **Step 2: Run `node --test src/test/js/engine-scroll.test.js`** and confirm it fails on current focus and cursor-scroll behavior.
- [x] **Step 3: Pin `#practice-input` to the viewport** in both CSS copies while preserving its hidden appearance and pointer behavior.
- [x] **Step 4: Focus input with `preventScroll` and remove smooth animation** from the visible cursor's per-keystroke `scrollIntoView` in both engine copies.
- [x] **Step 5: Run Node regression** and confirm it passes.

### Task 2: Verify exercise and application behavior

**Files:**
- Verify: exercise resources and project tests.

- [x] **Step 1: Run `node --test src/test/js/engine-scroll.test.js`**; expected: all engine assertions pass.
- [x] **Step 2: Run `mvn test`**; inspect any failures. If only `ProfileConfigTest.geminiAiConfigurationIsSharedByAllProfiles` fails because `application-dev.yml` and its assertion disagree on `.env` location, verify both files are unchanged and report it as unrelated.
- [x] **Step 3: Run `mvn -Dtest=ProseExerciseControllerTest,StageMapControllerTest test`**; expected: both exercise-facing controller test classes pass.
- [x] **Step 4: Inspect `git diff --check` and `git status --short`** to ensure only scoped changes exist and no whitespace errors were introduced.
