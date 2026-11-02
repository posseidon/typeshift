# Epic 0b infrastructure plan

## Goal

Run the same Spring Boot application against local PostgreSQL with Flyway in development and Supabase PostgreSQL in production. Use Azure OpenAI for AI content generation in every profile.

## Runtime configuration

- Keep development and production database configuration in separate Spring profiles.
- Read database credentials, Azure endpoint, API key, and deployment name from environment variables.
- Use the Azure OpenAI-compatible `/openai/v1/chat/completions` endpoint and the `Phi-4-mini-instruct` deployment.
- Keep provider transport and response failures wrapped in `AiProviderException` with their cause.
- Keep the development-only provider-connectivity route pointed at `/openai/v1/models`.

## Implementation checks

- Verify Flyway migrations against PostgreSQL.
- Test Azure request routing, response mapping, missing configuration, and provider failures with mocked HTTP responses.
- Test that each stage selects its stage-native prompt and that invalid generated output is rejected before serving.
- Document the Azure environment variables in `docs/azure-ai.md`.
