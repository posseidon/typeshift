# ADR-004: AI content generation

**Status:** Accepted

## Context

Typing exercises need stage-specific prose, task formats, and arithmetic. Generated exercises are served per request and exercise text is not persisted.

## Request contract

The request contains:

- `stageId` and `examType`
- `subExamNumber` for sub-exams
- up to five `focusKeys`
- the user's last five `recentContentHashes`
- `languageTarget`
- a Stage 5 `taskType` when applicable

The response contains:

- `contentType` (`PROSE` or `MATH`)
- `displayText` and `expectedAnswer`
- `estimatedDurationSeconds`

`PROSE` uses `displayText` as `expectedAnswer`. `MATH` uses a decimal result recomputed by the application.
The application, not the model, computes the SHA-256 content hash from `displayText`.

## Decision

One `GeminiContentService` selects a stage-native prompt and sends it through Spring AI's configured Google GenAI `ChatModel`. The starter uses `GEMINI_API_KEY` and the `gemini-3.5-flash-lite` model. Application code does not construct provider REST requests. No runtime provider selection is used.

The service returns the request/response contract above and wraps provider failures in `AiProviderException`, retaining the original cause. Exercises are not persisted; the application retains only per-user content hashes for deduplication.

## Validation

- Prose must be non-empty, within the configured exercise length bound, and include requested focus keys.
- Math is reparsed and recomputed with Java `BigDecimal`; a mismatch rejects the exercise.
- Content hashes must not match any of the user's five most recently served hashes.
- Each exercise makes exactly one model call. Invalid or duplicate output raises an AI error without automatic regeneration.
