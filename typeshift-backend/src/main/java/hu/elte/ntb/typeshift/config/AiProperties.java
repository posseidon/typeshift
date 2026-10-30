package hu.elte.ntb.typeshift.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI provider wiring from configuration: Azure OpenAI (prod) and Ollama (dev)
 * endpoints and model names, all read from environment variables.
 *
 * @param azureEndpoint Azure OpenAI resource endpoint
 * @param azureApiKey Azure OpenAI API key
 * @param azureDeployment GPT-4o class deployment name
 * @param ollamaBaseUrl Ollama server base URL
 * @param ollamaModel Ollama model tag
 */
@ConfigurationProperties(prefix = "typeshift.ai")
public record AiProperties(
    String azureEndpoint,
    String azureApiKey,
    String azureDeployment,
    String ollamaBaseUrl,
    String ollamaModel) {}
