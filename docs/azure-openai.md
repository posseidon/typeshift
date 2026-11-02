# Azure OpenAI configuration

The application uses Spring AI's Azure OpenAI starter. For local development,
configure `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, and the exact
`AZURE_OPENAI_DEPLOYMENT` registered at that endpoint in the ignored `.env` file.
Production must provide them through its environment or secret manager. A model
name such as `gpt-4o-mini` is not necessarily the deployment name.

See [Azure AI configuration](azure-ai.md) for deployment details.
