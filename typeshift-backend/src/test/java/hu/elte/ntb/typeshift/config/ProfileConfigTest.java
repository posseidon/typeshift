package hu.elte.ntb.typeshift.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ProfileConfigTest {

  private static PropertySource<?> load(String name, String resource) throws IOException {
    return new YamlPropertySourceLoader().load(name, new ClassPathResource(resource)).get(0);
  }

  @Test
  void azureAiConfigurationIsSharedByAllProfiles() throws IOException {
    PropertySource<?> shared = load("application", "application.yml");
    PropertySource<?> dev = load("application-dev", "application-dev.yml");

    assertThat(shared.getProperty("spring.ai.azure.openai.endpoint"))
        .isEqualTo("${AZURE_OPENAI_ENDPOINT:${AZURE_OPEN_AI_ENDPOINT:https://localhost}}");
    assertThat(shared.getProperty("spring.ai.azure.openai.api-key"))
        .isEqualTo("${AZURE_OPENAI_API_KEY:${AZURE_OPEN_AI_KEY:}}");
    assertThat(shared.getProperty("spring.ai.azure.openai.chat.options.deployment-name"))
        .isEqualTo("${AZURE_OPENAI_DEPLOYMENT:${AZURE_OPEN_AI_DEPLOYMENT:}}");
    assertThat(shared.getProperty("typeshift.ai.azure-endpoint"))
        .isEqualTo("${spring.ai.azure.openai.endpoint}");
    assertThat(shared.getProperty("typeshift.ai.azure-api-key"))
        .isEqualTo("${spring.ai.azure.openai.api-key}");
    assertThat(shared.getProperty("typeshift.ai.azure-deployment"))
        .isEqualTo("${spring.ai.azure.openai.chat.options.deployment-name}");
    assertThat(shared.getProperty("app.chat.model-name"))
        .isEqualTo("${spring.ai.azure.openai.chat.options.deployment-name}");
    assertThat(dev.getProperty("spring.config.import"))
        .isEqualTo("optional:file:../.env[.properties]");
    assertThat(dev.getProperty("spring.datasource.url"))
        .isEqualTo("${DEV_DATABASE_URL}");
  }

  @Test
  void devAndProdNeverShareDatabase() throws IOException {
    PropertySource<?> dev = load("application-dev", "application-dev.yml");
    PropertySource<?> prod = load("application-prod", "application-prod.yml");

    assertThat(dev.getProperty("spring.datasource.url"))
        .isNotEqualTo(prod.getProperty("spring.datasource.url"));
  }
}
