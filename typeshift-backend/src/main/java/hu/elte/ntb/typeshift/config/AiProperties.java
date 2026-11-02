package hu.elte.ntb.typeshift.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Azure OpenAI endpoint and deployment configuration. */
@ConfigurationProperties(prefix = "typeshift.ai")
public record AiProperties(
    String azureEndpoint,
    String azureApiKey,
    String azureDeployment) {}
