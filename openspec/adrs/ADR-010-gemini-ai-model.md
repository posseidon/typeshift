# ADR-010: Gemini AI model

**Status:** Accepted

## Decision

- Use Spring AI's Google GenAI starter and configured `ChatModel` in all runtime profiles. Application code SHALL NOT construct provider REST requests.
- Use the `gemini-3.5-flash-lite` model in all runtime profiles.
- Configure the Gemini Developer API key through the `GEMINI_API_KEY` environment variable.
- Do not add a runtime model fallback. If the model is unavailable, surface the provider error.

## Constraints

The configured API key must be valid for the Gemini Developer API. Structural and content validation runs before an exercise is served. Model language quality is not separately human-reviewed at runtime.
