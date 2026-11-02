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
  void geminiAiConfigurationIsSharedByAllProfiles() throws IOException {
    PropertySource<?> shared = load("application", "application.yml");
    PropertySource<?> dev = load("application-dev", "application-dev.yml");

    assertThat(shared.getProperty("spring.ai.google.genai.api-key"))
        .isEqualTo("${GEMINI_API_KEY:}");
    assertThat(shared.getProperty("spring.ai.google.genai.chat.options.model"))
        .isEqualTo("gemini-3.5-flash-lite");
    assertThat(shared.getProperty("app.chat.model-name"))
        .isEqualTo("${spring.ai.google.genai.chat.options.model}");
    assertThat(shared.getProperty("typeshift.ai.provider-endpoint"))
        .isEqualTo("https://generativelanguage.googleapis.com");
    assertThat(shared.getProperty("spring.ai.azure.openai.api-key")).isNull();
    assertThat(shared.getProperty("typeshift.ai.azure-endpoint")).isNull();
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
