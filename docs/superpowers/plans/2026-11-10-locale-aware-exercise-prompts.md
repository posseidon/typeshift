# Locale-Aware Exercise Prompts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate exercise text and typing constraints in the user's selected English, Hungarian, or Vietnamese locale.

**Architecture:** Pass the resolved Spring `Locale` from the docket controller through exercise generation into the existing `AiRequest` target language. `AiPromptFactory` will use that target language to substitute locale-specific language, keyboard, diacritic, and email-subject values into the existing exercise prompt resources.

**Tech Stack:** Java 21, Spring Boot MVC, Spring AI, JUnit 5, AssertJ, Mockito, Maven.

**Spec:** `docs/superpowers/specs/2026-11-10-locale-aware-exercise-prompts-design.md`

## Global Constraints

- Supported locales are English (`en`), Hungarian (`hu`), and Vietnamese (`vi`).
- An unsupported locale falls back to Vietnamese, matching the application's default locale.
- Keep one prompt file per exercise; do not create translated copies.
- Preserve each exercise's scenario, length, output shape, and validation behavior.
- English uses QWERTY key rows without required diacritics; Hungarian keeps its current QWERTZ rows, y/z swap, and Hungarian accents; Vietnamese uses QWERTY rows and Vietnamese diacritics.
- Math outputs remain expressions without prose.
- Do not add dependencies or change the UI, canned catalog, or other AI features.

## Review Focus

- The locale change stored on the shell request must still be used by the later asynchronous docket request — test the shell followed by the docket with the shell's session and assert the generated `AiRequest` target.
- Unsupported locale tags must not result in an unsupported language or unresolved prompt values — test the Vietnamese fallback in `ExerciseGenerationServiceTest`.
- Every resource, including nested stage-five task formats, must be rendered without stale Hungarian-only language instructions or unresolved placeholders — test all locale variants in `AiPromptFactoryTest`.
- Vietnamese diacritics and Hungarian accents must remain valid UTF-8 prompt content — assert locale-specific characters in the generated prompts.
- Math prompts must keep the validator-compatible decimal comma for every locale and remain prose-free — assert each localized math prompt requires only an expression ending in ` = ?`.

---

### Task 1: Carry the UI locale into exercise generation

**Files:**
- Modify: `src/main/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseController.java`
- Modify: `src/main/java/hu/elte/ntb/typeshift/exercise/service/ExerciseGenerationService.java`
- Modify: `src/main/java/hu/elte/ntb/typeshift/ai/model/LanguageTarget.java`
- Test: `src/test/java/hu/elte/ntb/typeshift/exercise/service/ExerciseGenerationServiceTest.java`
- Test: `src/test/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseControllerTest.java`

**Interfaces:**
- Produces: `ExerciseGenerationService.generate(CannedExercise definition, Locale locale)`; `LanguageTarget` includes `VIETNAMESE`.
- The docket handler accepts Spring's resolved `Locale` and calls the new generation signature.
- Map `en` to `ENGLISH`, `hu` to `HUNGARIAN`, and `vi` or any unsupported tag to `VIETNAMESE`.

- [ ] **Step 1: Write failing service tests**

Add `usesUiLocaleAsTargetLanguage` covering `Locale.ENGLISH`, Hungarian, Vietnamese, and an unsupported locale; capture the generated `AiRequest` and assert its target is `ENGLISH`, `HUNGARIAN`, or `VIETNAMESE` as specified.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `mvn -Dtest=ExerciseGenerationServiceTest test`
Expected: FAIL because generation does not yet accept or map the locale.

- [ ] **Step 3: Implement locale propagation and mapping**

Add `VIETNAMESE` to `LanguageTarget`, accept `Locale` in `generate`, map the three supported language tags with Vietnamese fallback, and pass the resolved locale from `ProseExerciseController.docket`.

- [ ] **Step 4: Verify service locale tests pass**

Run: `mvn -Dtest=ExerciseGenerationServiceTest test`
Expected: PASS.

- [ ] **Step 5: Test the docket route's locale propagation**

Add `docketUsesSessionLocaleForGeneratedContent` in `ProseExerciseControllerTest`: request the shell with `lang=en`, pass its session to the later docket request, capture the AI request, and assert `LanguageTarget.ENGLISH`.

- [ ] **Step 6: Run controller tests**

Run: `mvn -Dtest=ProseExerciseControllerTest test`
Expected: PASS, including existing route and failure behavior.

### Task 2: Make all exercise prompts locale-aware

**Files:**
- Modify: `src/main/java/hu/elte/ntb/typeshift/ai/service/AiPromptFactory.java`
- Modify: `src/main/resources/prompts/exercises/stage-1-exam-1.md`
- Modify: `src/main/resources/prompts/exercises/stage-1-exam-2.md`
- Modify: `src/main/resources/prompts/exercises/stage-1-exam-3.md`
- Modify: `src/main/resources/prompts/exercises/stage-1-grad.md`
- Modify: `src/main/resources/prompts/exercises/stage-2-exam-1.md`
- Modify: `src/main/resources/prompts/exercises/stage-2-exam-2.md`
- Modify: `src/main/resources/prompts/exercises/stage-2-grad.md`
- Modify: `src/main/resources/prompts/exercises/stage-3-exam-1.md`
- Modify: `src/main/resources/prompts/exercises/stage-3-exam-2.md`
- Modify: `src/main/resources/prompts/exercises/stage-3-grad.md`
- Modify: `src/main/resources/prompts/exercises/stage-4-exam-1.md`
- Modify: `src/main/resources/prompts/exercises/stage-4-exam-2.md`
- Modify: `src/main/resources/prompts/exercises/stage-4-grad.md`
- Modify: `src/main/resources/prompts/exercises/stage-5-exam-1.md`
- Modify: `src/main/resources/prompts/exercises/stage-5-exam-2.md`
- Modify: `src/main/resources/prompts/exercises/stage-5-exam-3.md`
- Modify: `src/main/resources/prompts/exercises/stage-5-exam-4.md`
- Modify: `src/main/resources/prompts/exercises/stage-5-grad.md`
- Modify: `src/main/resources/prompts/exercises/task-chat-burst.md`
- Modify: `src/main/resources/prompts/exercises/task-code-comment.md`
- Modify: `src/main/resources/prompts/exercises/task-document-paragraph.md`
- Modify: `src/main/resources/prompts/exercises/task-email-reply.md`
- Test: `src/test/java/hu/elte/ntb/typeshift/ai/service/AiPromptFactoryTest.java`

**Interfaces:**
- Consumes: `AiRequest.languageTarget()` from Task 1.
- Prompt resources use `{language}`, `{keyboardLayout}`, `{layoutRule}`, `{homeRowKeys}`, `{topRowKeys}`, `{bottomRowKeys}`, `{diacriticRule}`, and `{emailSubjectLabel}` where needed.
- `AiPromptFactory` supplies those values for English, Hungarian, or Vietnamese and substitutes them after expanding the stage-five task format.

- [ ] **Step 1: Add failing locale prompt tests**

Add `localizesExerciseAndTaskPrompts` to assert that English, Hungarian, and Vietnamese prompts contain the right language and keyboard/diacritic requirements; assert English uses `Subject:`, Vietnamese uses `Chủ đề:` and Vietnamese diacritics, and Hungarian retains `Tárgy:`, QWERTZ/y-z swap, and Hungarian accents. Assert QWERTY variants do not mention a y/z swap. Add a test that every exercise and task prompt contains no unresolved `{...}` placeholder and no stale hard-coded Hungarian language instruction outside Hungarian output. For every locale's math prompts, assert the decimal comma and expression-only ` = ?` format remain present.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `mvn -Dtest=AiPromptFactoryTest test`
Expected: FAIL on the absent locale substitution and hard-coded prompt language.

- [ ] **Step 3: Implement locale substitutions in `AiPromptFactory`**

Select substitution values from `AiRequest.languageTarget()` and apply them to both the exercise resource and any inserted task resource. Use `English`, `Hungarian`, and `Vietnamese`; English/Vietnamese use QWERTY row strings and no y/z swap instruction, while Hungarian keeps its current QWERTZ row strings and y/z swap instruction. Use no required diacritics for English, Hungarian's `á é í ó ö ő ú ü ű`, and Vietnamese's `ă â đ ê ô ơ ư` with natural Vietnamese tone marks. Use `Subject:`, `Tárgy:`, and `Chủ đề:` for the three email labels respectively. Keep decimal commas in math prompts for all locales because the existing evaluator only accepts that format.

- [ ] **Step 4: Replace locale-specific hard-coding in all 22 prompt resources**

Keep each scenario, length, and output format. Replace hard-coded language names, layout names/rules, key rows, diacritic lists, and email labels with the named placeholders where applicable. For English, ask for natural English without required diacritics; for Vietnamese, require natural Vietnamese and Vietnamese-specific characters in accent-focused exercises. Preserve math-only instructions, decimal commas, and ` = ?` endings.

- [ ] **Step 5: Run prompt tests and verify all resources render**

Run: `mvn -Dtest=AiPromptFactoryTest test`
Expected: PASS for all supported languages and existing exercise/task resource coverage.

### Task 3: Run project verification

**Files:** No additional files.

**Interfaces:** Consumes the completed locale propagation and prompt substitution from Tasks 1–2.

- [ ] **Step 1: Run all tests and quality checks**

Run: `mvn verify`
Expected: BUILD SUCCESS with tests and configured static-analysis checks passing.
