# Async exercise loading design

**Intent:** The exercise GET currently calls the LLM synchronously before rendering, so the browser shows nothing for up to seconds (or fails after a 10-second provider connect timeout). Render the exercise shell immediately and load the generated docket asynchronously once the LLM responds.

**Approach (approved: Option A — htmx fragment fetch):**
- `GET /exercise/{stageId}/{examKey}` renders instantly: readout bar, notices, loading placeholder, notes grid — no generation call.
- New `GET /exercise/{stageId}/{examKey}/docket` returns the exercise docket fragment via Thymeleaf (`exercise :: docket`): generated text, keypad/textarea, task class, docket label/framing, and the submission form with a fresh token.
- The placeholder performs `hx-get`, `hx-trigger="load"`, `hx-swap="outerHTML"`.
- `engine.js` initializes the typing canvas after `htmx:afterSwap` (refactor: wrap init so it runs on that event; keep the existing immediate init for server-rendered content resilience).
- On `htmx:responseError` (e.g. `AiProviderException` → 500), show an inline error with a Retry button in place of the placeholder.

**Constraints/non-goals:** Prompt-only behavior aside, this changes Java routing and the exercise template. No change to generation logic, validation, tokens, submission, catalog, task-type selection, or provider timeout settings. Provider connectivity failures still fail generation; they surface as a retryable in-page error instead of a slow/unstyled page. No new dependencies.

**Tests:** Update `ProseExerciseControllerTest`: the main GET renders the shell without `data-target` and without calling generation; the docket GET serves the generated text and calls generation. Full `mvn -pl typeshift-backend -am verify` must pass.
