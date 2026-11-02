# Gemini provider switch

## Intent

Move all active TypeShift exercise generation from Azure OpenAI to Google's Gemini API, using the supplied model choice and a replacement API key provided through `GEMINI_API_KEY`. The key already posted in chat is compromised and will not be recorded.

## Goals and constraints

- Use Spring AI's existing Google GenAI starter and `ChatModel`; preserve the current prompt, response parsing, exercise validation, and persistence behavior.
- Configure `GEMINI_API_KEY` and `gemini-3.5-flash-lite` consistently for dev and prod.
- Remove Azure-specific active configuration, names, tests, connectivity metadata, and user-facing setup documentation; update the active AI spec and ADR.
- Preserve unrelated pre-existing working-tree changes, including prompt and exercise behavior edits.
- Do not rewrite archived OpenSpec history. No provider fallback/selection, raw REST client, or additional dependency.

## Approaches

1. **Keep Spring AI Google GenAI (chosen):** correct the existing starter configuration and rename stale Azure-specific surfaces. Minimal change; retains the existing provider abstraction and app behavior.
2. **Call Gemini REST directly:** add custom HTTP request/response handling. Repeats capabilities already supplied by the starter and increases code, so not chosen.

## Implementation scope

Update shared AI properties and model configuration, the provider service name and error descriptions, connectivity status configuration, dependent tests, `.env.example`, current AI setup docs, and active OpenSpec spec/ADR. Keep the API key environment-only. Verify with backend tests and static checks available in the project.

## Acceptance

With `GEMINI_API_KEY` set, the configured Gemini `ChatModel` handles generation in every profile; no active setup/configuration path requires Azure credentials or a deployment name; tests cover the Gemini configuration and existing generation behavior.
