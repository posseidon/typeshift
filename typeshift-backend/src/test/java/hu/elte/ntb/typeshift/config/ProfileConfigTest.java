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
  void devProfilePointsAtLocalPostgresAndOllama() throws IOException {
    PropertySource<?> dev = load("application-dev", "application-dev.yml");

    assertThat(dev.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://127.0.0.1:5432/typeshift");
    assertThat(dev.getProperty("typeshift.ai.ollama-base-url"))
        .isEqualTo("${OLLAMA_BASE_URL:http://localhost:11434}");
    assertThat(dev.getProperty("typeshift.ai.ollama-model")).isEqualTo("${OLLAMA_MODEL:qwen2.5:14b}");
  }

  @Test
  void devAndProdNeverShareDatabase() throws IOException {
    PropertySource<?> dev = load("application-dev", "application-dev.yml");
    PropertySource<?> prod = load("application-prod", "application-prod.yml");

    assertThat(dev.getProperty("spring.datasource.url"))
        .isNotEqualTo(prod.getProperty("spring.datasource.url"));
  }
}
