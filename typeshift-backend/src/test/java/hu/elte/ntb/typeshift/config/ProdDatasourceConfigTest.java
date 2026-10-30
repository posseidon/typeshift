package hu.elte.ntb.typeshift.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ProdDatasourceConfigTest {

  @Test
  void prodProfilePointsAtSupabaseWithSmallPool() throws IOException {
    PropertySource<?> props =
        new YamlPropertySourceLoader()
            .load("application-prod", new ClassPathResource("application-prod.yml"))
            .get(0);

    assertThat(props.getProperty("spring.datasource.url")).isEqualTo("${DATABASE_URL}");
    assertThat(props.getProperty("spring.datasource.username")).isEqualTo("${DB_USERNAME}");
    assertThat(props.getProperty("spring.datasource.password")).isEqualTo("${DB_PASSWORD}");
    assertThat(props.getProperty("spring.datasource.hikari.maximum-pool-size")).isEqualTo(2);
  }
}
