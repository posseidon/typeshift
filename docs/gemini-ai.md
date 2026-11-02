# Gemini AI configuration

TypeShift generates exercise content through Spring AI's Google GenAI chat model. The Google GenAI starter configures the client; application code does not construct provider REST requests.

Configure `GEMINI_API_KEY` with a Gemini API key from Google AI Studio. The model is `gemini-3.5-flash-lite` in every runtime profile.

For local development, put the key in the ignored repository-root `.env` file. Spring Boot imports that file as properties, and Docker Compose reads it for variable interpolation. Use `.env.example` as a placeholder-only template. Production should receive `GEMINI_API_KEY` from the deployment environment or a secret manager; the local `.env` file is not packaged with the application.

The development-only provider-connectivity endpoint probes the configured Gemini chat model and reports the Gemini API base URL. Exercise generation requires `GEMINI_API_KEY`; provider and response errors are surfaced as AI errors, exercises are not stored, and recent content hashes are retained for deduplication.
