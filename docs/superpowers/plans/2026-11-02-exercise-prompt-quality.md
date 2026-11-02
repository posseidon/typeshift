# Exercise Prompt Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve all exercise prompt resources using the station specifications and ensure generated text includes the requested weak-key characters.

**Architecture:** Keep one Markdown template per catalog exercise, plus four reusable Stage 5 task-format resources. Tailor each template to its exact station/exam focus; `AiPromptFactory.create(AiRequest)` continues resolving those resources and adds a shared weak-key instruction when `focusKeys` is nonempty. Preserve task selection and the current JSON response wrapper.

**Tech Stack:** Java 21, Maven, JUnit 5, UTF-8 Markdown classpath resources.

**Spec:** `docs/superpowers/specs/2026-11-02-exercise-prompt-quality-design.md`

## Global Constraints

- Follow ADR-013 and the station definitions in `docs/superpowers/specs/2026-10-31-stations-playable-design.md`.
- `Stage 3` prompts use only `+`, `-`, `*`, `/`, and decimal commas; the expression must end in ` = ?`.
- Keep Stage 5 task type selection dynamic; Stage 5 graduation currently generates one selected task per request.
- Keep stage-specific instructions under 200 tokens; preserve the current response wrapper, request model, and one-model-call behavior.
- Include only weak-key characters in prompt guidance; do not include recent content hashes or typing history.
- No new dependencies.

## Review Focus

- Each of the 18 catalog exercise identities must select a prompt for its own stage and focus, including the distinct Stage 1 row drills and Stage 3 graduation; assert the expected focus phrase for each identity.
- Stage 1 graduation instruction must request at least 1,000 characters while staying within the 4,000-character service limit; assert both bounds.
- Stage 3 content must remain parseable and scale with sub-exam number; assert operator, decimal-comma, final ` = ?`, and division constraints.
- Stage 4 and all Stage 5 format resources must preserve their language/format and known content-length constraints; assert accent coverage and each task type's expected shape.
- Focus-key guidance must include all supplied prose characters and be absent when the list is empty; test both cases, including accented and punctuation keys. Math requests must send and validate only characters accepted by the math grammar, while prose requests retain all keys.

---

### Task 1: Tailor all prompt resources and pass weak-key guidance

**Files:**
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/AiPromptFactory.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/service/ExerciseGenerationService.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/AiPromptFactoryTest.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/service/ExerciseGenerationServiceTest.java`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-1-exam-1.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-1-exam-2.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-1-exam-3.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-1-grad.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-2-exam-1.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-2-exam-2.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-2-grad.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-3-exam-1.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-3-exam-2.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-3-grad.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-4-exam-1.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-4-exam-2.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-4-grad.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-exam-1.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-exam-2.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-exam-3.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-exam-4.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/stage-5-grad.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/task-chat-burst.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/task-email-reply.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/task-document-paragraph.md`
- Modify: `typeshift-backend/src/main/resources/prompts/exercises/task-code-comment.md`

**Interfaces:**
- Consumes: `AiRequest.focusKeys(): List<String>` and the existing stage, exam, and task fields.
- Produces: unchanged `AiPromptFactory.create(AiRequest): String`; the returned prompt includes a focus-key directive only when keys are present.

- [x] **Step 1: Write failing prompt-specification tests**

In `AiPromptFactoryTest`, add `stationAndTaskPromptsFollowSpecifications` to assert each of the 18 exercise requests contains its stage/exam-specific instruction, and that all four Stage 5 task types produce their format-specific constraints. Add `addsOnlyRequestedFocusKeysToPrompt` to assert every supplied focus key (including `ő` and `@`) appears in the prompt and an empty list adds no focus-key directive. In `ExerciseGenerationServiceTest`, add `mathRequestsOnlyParserCompatibleFocusKeys` to verify Stage 3 filters letters but retains numeric/operator focus keys for both the prompt request and validation, while prose generation retains all focus keys. Retain the existing invalid-stage, missing-task, and missing-resource checks.

- [x] **Step 2: Run the focused test and confirm it fails**

Run: `mvn -pl typeshift-backend -am -Dtest=AiPromptFactoryTest,ExerciseGenerationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because current resource wording lacks the station-specific assertions, `AiPromptFactory` does not include `focusKeys`, and Stage 3 validation rejects an unrepresentable letter focus key.

- [x] **Step 3: Enrich the prompt resources and add shared focus-key guidance**

Use ADR-013, the station design, the `ai-content` spec, the `real-world-tasks` spec, and existing catalog/test bounds. Give every exercise resource its exact exam focus; keep Stage 5 exercise context compatible with any dynamically selected task type. Put each Stage 5 format contract in its matching `task-*.md` file. For Stage 5 graduation, request one graduation-length task in the selected format, not a three-task sequence. In `ExerciseGenerationService`, filter math focus keys to single characters accepted by the evaluator and pass that same list to `AiRequest` and validation; leave prose keys unchanged. In `AiPromptFactory`, append a concise instruction containing `String.join(", ", request.focusKeys())` only when the list is nonempty; preserve the current target-language and JSON wrapper.

- [x] **Step 4: Run the focused test and confirm it passes**

Run: `mvn -pl typeshift-backend -am -Dtest=AiPromptFactoryTest,ExerciseGenerationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS.

- [x] **Step 5: Run backend verification**

Run: `mvn -pl typeshift-backend -am verify`
Expected: BUILD SUCCESS, all backend tests pass, and PMD/SpotBugs/Checkstyle report no violations.
