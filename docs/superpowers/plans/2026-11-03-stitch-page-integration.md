# Stitch Page Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply the Stitch page content/layout to the stage map and typing exercise while retaining the existing TypeShift flows.

**Architecture:** Keep the shared Thymeleaf shell and controller view models. Reshape the two content fragments around the Stitch markup, binding stage/exercise links to live catalog data and preserving the exercise shell/docket contract used by htmx and `engine.js`.

**Tech Stack:** Spring Boot, Thymeleaf, MockMvc/JUnit, existing CSS and vanilla JavaScript.

**Spec:** `docs/superpowers/specs/2026-11-03-stitch-page-integration-design.md`

## Global Constraints

- Keep `layout.html` as the shared shell and the `contentFragment` contract unchanged.
- Keep stage links on the existing stage/exam routes and render from the existing catalog model.
- Preserve asynchronous exercise generation, prose/numeric modes, keypad hooks, notices, metrics, and input capture.
- Reuse `style.css` and `engine.js`; do not add dependencies or change backend logic.
- Preserve unrelated working-tree changes.

## Review Focus

- Every catalog entry stays selectable at its existing URL — pin with `everyCatalogExerciseHasASelectableLink`.
- Stage names and exercises remain dynamically rendered rather than duplicated as demo data — pin with the same stage-map test.
- Initial exercise HTML remains a shell without generated target text, and retains retry behavior — pin with `shellPageIsPublicAndServesLoadingPlaceholder` and `providerFailureKeepsThePublicShellRetryable`.
- Generated prose continues to expose the input/display IDs expected by `engine.js` — pin with `publicDocketServesAiGeneratedText` and `proseDocketOmitsKeyPad`.
- Numeric dockets retain the keypad, while the stage-map return link remains available — pin with `numericDocketServesKeypad` and `completionReturnsToStageMapWithoutSubmittingResults`.

---

### Task 1: Integrate the Stitch stage-map content

**Files:**
- Modify: `src/main/resources/templates/stage-map.html`
- Modify: `src/main/resources/static/css/style.css`
- Modify: `src/main/resources/messages.properties`
- Modify: `src/main/resources/messages_hu.properties`
- Modify: `src/main/resources/messages_vi.properties`
- Test: `src/test/java/hu/elte/ntb/typeshift/stagemap/controller/StageMapControllerTest.java`

**Interfaces:**
- Consumes: `stageIds`, `stageNames`, and `exercisesByStage` supplied by `StageMapController.stageMap`.
- Produces: Stage cards and exercise actions that continue linking to `/exercise/{stageId}/{examKey}`.

- [x] **Step 1: Extend `everyCatalogExerciseHasASelectableLink`** to assert the new page structure and key stage-map content while continuing to verify every catalog link and all five stage names.
- [x] **Step 2: Run the focused test and confirm the new layout assertions fail.**

Run: `mvn -Dtest=StageMapControllerTest test`
Expected: FAIL on the new Stitch-layout assertion(s), without unrelated failures.

- [x] **Step 3: Adapt `stage-map.html`** to use the Stitch station/card structure, keeping Thymeleaf loops and route expressions bound to the existing model. Keep all labels localizable using existing message keys.
- [x] **Step 4: Re-run the focused test.**

Run: `mvn -Dtest=StageMapControllerTest test`
Expected: PASS; each current catalog exercise renders one existing route.

### Task 2: Integrate the Stitch exercise layout without breaking typing

**Files:**
- Modify: `src/main/resources/templates/exercise.html`
- Modify: `src/main/resources/static/css/style.css`
- Test: `src/test/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseControllerTest.java`

**Interfaces:**
- Consumes: `stageId` and `examKey` for the shell; the existing async `docket` fragment model for generated content.
- Produces: A Stitch-based exercise page whose stable DOM IDs, htmx attributes, and numeric/prose branches remain compatible with `engine.js`.

- [x] **Step 1: Extend shell and docket assertions** in `shellPageIsPublicAndServesLoadingPlaceholder`, `publicDocketServesAiGeneratedText`, and `numericDocketServesKeypad` to cover the adopted layout structure and preserve required hooks.
- [x] **Step 2: Run the focused test and confirm the new layout assertions fail.**

Run: `mvn -Dtest=ProseExerciseControllerTest test`
Expected: FAIL on the new layout assertion(s), without unrelated failures.

- [x] **Step 3: Adapt `exercise.html`** to use the Stitch exercise layout as its structure, retaining readouts, notices, async loading/retry, generated docket, prose/numeric input, keypad, and stage-map navigation. Do not add unsupported topic-selection interactions or static demo content.
- [x] **Step 4: Re-run the focused test.**

Run: `mvn -Dtest=ProseExerciseControllerTest test`
Expected: PASS for initial shell, generated prose, numeric keypad, error/retry, and return navigation.

### Task 3: Verify the integrated pages

**Files:**
- Verify: `src/main/resources/templates/stage-map.html`
- Verify: `src/main/resources/templates/exercise.html`
- Verify: `src/main/resources/static/js/engine.js`

- [x] **Step 1: Run both focused controller test classes together.**

Run: `mvn -Dtest=StageMapControllerTest,ProseExerciseControllerTest test`
Expected: PASS.

- [x] **Step 2: Run the full Maven test suite.**

Run: `mvn test`
Expected: BUILD SUCCESS; report any pre-existing unrelated failures separately.
