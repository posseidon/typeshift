# B1 Stage Prompt Content Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rewrite the exercise and graduation prompt content for clear B1-level Hungarian text that follows the user's stage topics.

**Architecture:** Keep the existing 18 exercise-specific Markdown resources and four shared Stage 5 format resources. Each exercise resource supplies its topic and B1 guidance; for Stage 5, the dynamically selected task-format resource continues to determine the output shape. Do not change Java behavior, random task selection, or the current one-task-per-request graduation flow.

**Tech Stack:** Markdown classpath resources, JUnit 5 prompt tests, Maven.

**Spec:** `docs/superpowers/specs/2026-11-02-exercise-prompt-quality-design.md`

## Global Constraints

- Use the supplied Station 01–05 topic names and keep generated Hungarian at CEFR B1.
- Retain station key/character coverage, Stage 3 parser grammar, existing exercise-length instructions, and the four TaskType format contracts.
- Keep Stage 5 sub-exam task type random; exercise topics provide scenario context and do not override the selected format.
- Stage 5 graduation currently returns one selected task per request; frame it as one task in the three-task requirement without requesting three tasks in one output.
- No Java, model, validation, or dependency changes.

## Review Focus

- Every exercise and graduation prompt must name its supplied topic and request clear B1-level Hungarian; pin each identity in the prompt test.
- Stage 1 must retain row-specific key focus and the 3-minute graduation passage bounds.
- Stage 2 must distinguish standard punctuation/hyphenation, AltGr symbols/brackets/quotes, and the formatted support-ticket graduation.
- Stage 3 must retain keypad/ledger/Cashier Rush topics and only parser-supported arithmetic output.
- Stage 5 topic context must remain compatible with any randomly selected TaskType, and graduation must remain one task per request.

---

### Task 1: Rewrite the 22 Markdown prompt resources for B1 topics

**Files:**
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/AiPromptFactoryTest.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/AzureOpenAIContentServiceTest.java`
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
- Consumes: Existing `AiPromptFactory.create(AiRequest): Prompt` and current resource substitution for sub-exam numbers and Stage 5 task formats.
- Produces: Same factory and generation behavior, with more specific B1-oriented resource instructions.

- [x] **Step 1: Write failing prompt-content assertions and align test setup**

Update `stationAndTaskPromptsFollowSpecifications` to assert the user-supplied topic phrases for all 18 catalog identities, that each exercise resource requests B1-level Hungarian, and that the four Stage 5 format files remain format-specific. For Stage 5 graduation, assert the prompt says this response is one task in the three-task requirement while still generating one selected task. Retain tests for key/accent coverage, math grammar, and task formats. Update the five Azure test constructions to use the current `AzureOpenAIContentService(ChatModel, ObjectMapper)` signature; this is test-only compatibility work and changes no production behavior.

- [x] **Step 2: Run the focused test and confirm it fails on the old prompt text**

Run: `mvn -pl typeshift-backend -am -Dtest=AiPromptFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because current prompts do not contain the supplied topic mapping or consistent B1 guidance.

- [x] **Step 3: Rewrite the prompt resources**

Use the exact topic map in `docs/superpowers/specs/2026-11-02-exercise-prompt-quality-design.md`. Use familiar B1 vocabulary, direct sentences, and realistic stories. Preserve Stage 1 row focus, Stage 2 symbol coverage, Stage 3’s numeric grammar, Stage 4 accents/formal register, and the random Stage 5 task format. Do not make the prompt ask for a three-task output.

- [x] **Step 4: Run the focused test and confirm it passes**

Run: `mvn -pl typeshift-backend -am -Dtest=AiPromptFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS.

- [x] **Step 5: Scan all prompt Markdown and run backend verification**

Run the humanizer scanner on all `typeshift-backend/src/main/resources/prompts/exercises/*.md` files, then run: `mvn -pl typeshift-backend -am verify`
Expected: no high-severity writing findings and BUILD SUCCESS.
