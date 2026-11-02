# ADR-010: Azure AI model

**Status:** Accepted

## Decision

- Use Spring AI's Azure OpenAI starter and configured `ChatModel` in all runtime profiles. Application code SHALL NOT construct provider REST requests.
- Use the deployment identifier configured for the endpoint; a model family name is valid only when a deployment exists under that exact name.
- Configure the endpoint, API key, and deployment name with environment variables. The deployment name has no implicit default.
- Do not add a runtime model fallback. If the deployment is unavailable, surface the provider error.

## Constraints

The configured endpoint must accept Azure OpenAI chat-completions requests for the named deployment. Structural and content validation runs before an exercise is served. Model language quality is not separately human-reviewed at runtime.
