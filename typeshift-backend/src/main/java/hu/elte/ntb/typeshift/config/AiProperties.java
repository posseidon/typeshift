package hu.elte.ntb.typeshift.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** AI provider metadata used by operational endpoints. */
@ConfigurationProperties(prefix = "typeshift.ai")
public record AiProperties(String providerEndpoint) {}
