# Gemini Provider Switch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use Gemini for all TypeShift exercise generation, configured by `GEMINI_API_KEY` and the `gemini-3.5-flash-lite` model.

**Architecture:** Keep Spring AI's existing Google GenAI starter and provider-neutral `ChatModel`. Remove Azure-specific names and config, keep the dev connectivity endpoint reporting the Gemini API base URL, and preserve existing prompt/generation behavior.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring AI 1.1.8, Maven, YAML, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-03-gemini-provider-switch-design.md`

## Global Constraints

- Use the existing `spring-ai-starter-model-google-genai`; add no dependency or hand-written REST client.
- Read the API key only from `GEMINI_API_KEY`; never commit a key.
- Preserve unrelated existing working-tree changes, especially prompt and exercise behavior edits.
- Do not rewrite archived OpenSpec history or old planning records.
- Keep provider failures wrapped in `AiProviderException` with their cause.

## Review Focus

- Missing key or model config must not result in a committed credential or an Azure fallback; the YAML/config test asserts the environment placeholder and fixed model.
- A Gemini transport failure must remain visible as an `AiProviderException` with cause; the content-service tests pin this behavior.
- Malformed model output must still be rejected without retry; existing content-service and exercise-generation tests cover this.
- Failed connectivity probe must report unreachable and identify the Gemini base URL; the controller test pins both outcomes.
- Archived Azure decision records must remain historical; final search checks distinguish archived references from active configuration/docs.

---

### Task 1: Gemini configuration and local setup

**Files:**
- Modify: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/ProfileConfigTest.java`
- Modify: `typeshift-backend/src/main/resources/application.yml`
- Modify: `.env.example`

**Interfaces:**
- Produces Spring AI properties `spring.ai.google.genai.api-key=${GEMINI_API_KEY:}` and `spring.ai.google.genai.chat.options.model=gemini-3.5-flash-lite` for every profile.

- [ ] **Step 1: Write the failing config test**

Update `azureAiConfigurationIsSharedByAllProfiles` to assert the Gemini API-key placeholder and model property, assert no Azure AI property is present, and retain the existing dev database/profile assertions.

- [ ] **Step 2: Run the targeted test and verify it fails**

Run: `mvn -pl typeshift-backend -Dtest=ProfileConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because the current YAML still binds the key/model to Azure names.

- [ ] **Step 3: Configure Gemini**

In `application.yml`, bind `GEMINI_API_KEY`, set the model to `gemini-3.5-flash-lite`, remove stale Azure/project/deployment properties, and keep any still-used generic model metadata pointed at the Gemini model. Replace Azure variables in `.env.example` with a placeholder for `GEMINI_API_KEY` only.

- [ ] **Step 4: Run the targeted test and verify it passes**

Run: `mvn -pl typeshift-backend -Dtest=ProfileConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS.

### Task 2: Rename the generation service and migrate callers

**Files:**
- Rename: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/ai/service/AzureOpenAIContentService.java` to `GeminiContentService.java`
- Rename: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/ai/service/AzureOpenAIContentServiceTest.java` to `GeminiContentServiceTest.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/service/ExerciseGenerationService.java`
- Modify: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/service/ExerciseGenerationServiceTest.java`

**Interfaces:**
- Produces `GeminiContentService.generate(AiRequest): AiResponse`; callers continue using the same response and error contract.

- [ ] **Step 1: Rename the service and test references**

Use the reference-aware rename for the class, update the test class/file name and exercise-service references, and rename any Azure-specific test name. Preserve all existing uncommitted prompt and math-focus-key changes.

- [ ] **Step 2: Run content and generation tests**

Run: `mvn -pl typeshift-backend -Dtest=GeminiContentServiceTest,ExerciseGenerationServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS; transport/parse errors retain causes and invalid content still makes one provider call.

- [ ] **Step 3: Update provider error descriptions**

Replace Azure-specific text in `GeminiContentService` and `ExerciseGenerationService` with Gemini-neutral/provider-neutral descriptions without changing exception types or validation behavior.

- [ ] **Step 4: Re-run the focused tests**

Run the command from Step 2.
Expected: PASS.

### Task 3: Migrate the connectivity probe

**Files:**
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/AiProperties.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/health/controller/ProviderConnectivityController.java`
- Modify: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/health/controller/ProviderConnectivityControllerTest.java`
- Modify: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/config/ProfileConfigTest.java`
- Modify: `typeshift-backend/src/main/resources/application.yml`

**Interfaces:**
- The dev-only route remains `/api/admin/provider-connectivity`; `ProviderConnectivityStatus.endpoint()` reports `https://generativelanguage.googleapis.com` on success and failure.

- [ ] **Step 1: Update connectivity tests**

Remove the Azure test properties and assert the Gemini base URL for reachable and unreachable probe results.

- [ ] **Step 2: Run the controller test and verify it fails**

Run: `mvn -pl typeshift-backend -Dtest=ProviderConnectivityControllerTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: FAIL because the controller still reads `azureEndpoint()`.

- [ ] **Step 3: Switch connectivity metadata to Gemini**

Replace Azure-only `AiProperties` fields with only the required generic endpoint property, configure it with the Gemini API base URL, and update controller wiring. Do not expose the API key in the connectivity response.

- [ ] **Step 4: Verify connectivity and configuration tests**

Run: `mvn -pl typeshift-backend -Dtest=ProviderConnectivityControllerTest,ProfileConfigTest -Dsurefire.failIfNoSpecifiedTests=false test`
Expected: PASS.

### Task 4: Update active documentation and OpenSpec

**Files:**
- Delete: `docs/azure-ai.md`
- Delete: `docs/azure-openai.md`
- Create: `docs/gemini-ai.md`
- Modify: `openspec/README.md`
- Modify: `openspec/adrs/ADR-004-ai-content-service.md`
- Rename: `openspec/adrs/ADR-010-azure-ai-model.md` to `ADR-010-gemini-ai-model.md`
- Modify: `openspec/specs/ai-content/spec.md`

- [ ] **Step 1: Write current docs and spec for Gemini**

Document `.env` setup with `GEMINI_API_KEY`, the `gemini-3.5-flash-lite` model, Spring AI Google GenAI integration, and production secret injection. Update the active spec and ADRs to match; do not change archived ADR/spec records.

- [ ] **Step 2: Search active files for stale provider references**

Run: `git grep -n -E 'AZURE_OPENAI|AZURE_OPEN_AI|azure\.openai|Azure OpenAI|Azure AI|AzureOpenAIContentService' -- . ':!openspec/changes/archive/**' ':!docs/superpowers/plans/**' ':!docs/superpowers/specs/2026-11-03-gemini-provider-switch-design.md'`
Expected: no active application/configuration/setup/spec references; the one permitted active match is the explicit negative assertion in `ProfileConfigTest` that guards against restoring the old Azure property.

### Task 5: Full verification

**Files:** All files above.

- [ ] **Step 1: Run the backend verification suite**

Run: `mvn -pl typeshift-backend -am verify`
Expected: all tests and configured Checkstyle, PMD, and SpotBugs gates pass.

- [ ] **Step 2: Review final diff and provider references**

Confirm the diff does not include `.env`, the exposed key, unrelated working-tree changes, or edits to archived records; confirm no stale Azure provider config remains active.
