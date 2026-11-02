# Azure AI configuration

TypeShift generates typing exercises through Spring AI's Azure OpenAI chat model. The Azure OpenAI starter configures the client; application code does not construct provider REST requests. There is no local model service.

Configure these environment variables:

| Variable | Purpose |
|---|---|
| `AZURE_OPENAI_ENDPOINT` | Azure resource endpoint or an Azure OpenAI-compatible gateway |
| `AZURE_OPENAI_API_KEY` | API key for the Azure resource |
| `AZURE_OPENAI_DEPLOYMENT` | Exact deployment name registered at the configured endpoint |

For local development, put these values in the ignored repository-root `.env`
file. Spring Boot imports that file as properties, and Docker Compose reads it
for variable interpolation. Use `.env.example` as a placeholder-only template.
Production should receive the same variables from the deployment environment or
secret manager; the local `.env` file is not packaged with the application.

Set the deployment variable to the exact deployment identifier registered at the configured endpoint. A model family name such as `gpt-4o-mini` works only if the endpoint has a deployment with that exact name. The development-only provider-connectivity endpoint sends a short chat prompt through the same Spring AI model.

When using a gateway, it must support the Azure OpenAI deployment-specific chat API used by Spring AI's Azure starter; an OpenAI-v1-only endpoint is not interchangeable.

Exercise generation requires a configured endpoint, key, and deployment. Provider and response errors are surfaced as AI errors; exercises are not stored, while recent content hashes are retained for deduplication.
