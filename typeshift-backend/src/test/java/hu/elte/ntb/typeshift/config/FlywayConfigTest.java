package hu.elte.ntb.typeshift.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class FlywayConfigTest {

  private static PropertySource<?> load(String name, String resource) throws IOException {
    return new YamlPropertySourceLoader().load(name, new ClassPathResource(resource)).get(0);
  }

  @Test
  void flywayOwnsSchemaWithValidate() throws IOException {
    PropertySource<?> base = load("application", "application.yml");

    assertThat(base.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    assertThat(base.getProperty("spring.flyway.enabled")).isEqualTo(true);
  }

  @Test
  void baselineMigrationExists() {
    assertThat(new ClassPathResource("db/migration/V1__baseline.sql").exists()).isTrue();
  }
}
