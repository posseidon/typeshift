package hu.elte.ntb.typeshift.config;

import static org.assertj.core.api.Assertions.assertThat;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import jakarta.persistence.Table;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SchemaMigrationTest {

  private static String read(String migration) throws IOException {
    ClassPathResource resource = new ClassPathResource("db/migration/" + migration);
    try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
      return new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
    }
  }

  @Test
  void userSchemaMatchesStory11() throws IOException {
    String sql = read("V3__app_user_schema.sql");
    assertThat(sql).contains("ALTER TABLE users RENAME TO app_user");
    assertThat(sql).contains("ADD COLUMN user_id BIGINT");
    assertThat(sql).contains("CREATE INDEX idx_revoked_token_expires_at ON revoked_token (expires_at)");
  }

  @Test
  void userAccountMapsAppUser() {
    assertThat(UserAccount.class.getAnnotation(Table.class).name()).isEqualTo("app_user");
  }
}
