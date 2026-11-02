# Exercise Prompt Resources Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the 18 catalog exercise prompt instructions and four dynamic Stage 5 task-format instructions into classpath resources.

**Architecture:** `AiPromptFactory.create(AiRequest)` keeps its public interface and shared language/JSON rules, but loads the exercise instruction by stage and exam identity. Stage 3 interpolates the sub-exam number; Stage 5 loads the dynamically selected task-format resource and inserts it into that exercise's template.

**Tech Stack:** Java, Spring Boot classpath resources, JUnit 5, Maven.

**Spec:** `docs/superpowers/specs/2026-11-02-exercise-prompts-design.md`

## Global Constraints

- Preserve current prompt wording, task-type selection, stage validation, and output contract.
- Put the 18 per-exercise resources and four Stage 5 task-format resources in `typeshift-backend/src/main/resources/prompts/exercises/`.
- Do not change the request model or add dependencies.

## Review Focus

- Graduation requests must select `grad` resources, not an exam-number resource; test graduation selection for all five stages.
- Stage 3 must interpolate the requested sub-exam number; test two different numbers.
- Stage 5 must retain its task-type-specific wording and reject a missing type; test all four task types and null.
- Unknown stage IDs must continue to fail with `IllegalArgumentException`; test the existing validation behavior.
- Resource files must be read as UTF-8 and preserve source punctuation; test the exact punctuation-bearing prompt wording and UTF-8 resource-loading path. (The current basic strings are ASCII-only.)

---

### Task 1: Load exercise and task prompts from resources

**Files:**
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/AiPromptFactory.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/AiPromptFactoryTest.java`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-1-exam-1.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-1-exam-2.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-1-exam-3.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-1-grad.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-2-exam-1.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-2-exam-2.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-2-grad.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-3-exam-1.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-3-exam-2.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-3-grad.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-4-exam-1.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-4-exam-2.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-4-grad.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-exam-1.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-exam-2.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-exam-3.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-exam-4.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-grad.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/task-chat-burst.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/task-email-reply.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/task-document-paragraph.md`
- Create: `typeshift-backend/src/main/resources/prompts/exercises/task-code-comment.md`

**Interfaces:**
- Consumes: `AiRequest(stageId, examType, subExamNumber, ..., taskType)`.
- Produces: unchanged `AiPromptFactory.create(AiRequest) -> String` API, with classpath prompt selection by stage/exam identity.

- [x] **Step 1: Add failing resource-backed prompt tests**

Update `AiPromptFactoryTest` to exercise all 18 stage/exam identities and assert the existing basic wording; assert graduation paths, Stage 3 interpolation, all four Stage 5 task formats, punctuation-bearing resource text, missing Stage 5 task type, unknown-stage rejection, and rejection of an exercise identity with no matching resource.

- [x] **Step 2: Run the focused test and confirm it fails for missing resources**

Run: `mvn -pl typeshift-backend -am -Dtest=AiPromptFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because the new exercise resources are not loaded, including the assertion that an unknown exercise identity must not silently receive another exercise's prompt.

- [x] **Step 3: Add the 18 exercise templates and four task-format resources**

Use the existing basic prompt wording verbatim. Stage 3 exercise files contain a `{subExamNumber}` placeholder; Stage 5 exercise files contain a `{taskFormat}` placeholder. The four task files contain the existing `TaskType` prompt strings.

- [x] **Step 4: Update `AiPromptFactory.create(AiRequest)` to load and interpolate resources**

Derive `stage-N-exam-N` for `ExamType.SUB_EXAM` and `stage-N-grad` for `ExamType.GRADUATION`; preserve current null checks and unknown-stage exceptions. Load resources as UTF-8, substitute the applicable Stage 3 or Stage 5 placeholder, then append the existing shared language and JSON response rules unchanged. Convert resource I/O failures to an unchecked configuration error.

- [x] **Step 5: Run the focused test and confirm it passes**

Run: `mvn -pl typeshift-backend -am -Dtest=AiPromptFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS.

- [x] **Step 6: Run the complete backend test suite**

Run: `mvn -pl typeshift-backend -am test`
Expected: PASS with no test failures.
