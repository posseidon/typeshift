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
  void learningProfileSchemaMatchesStory12() throws IOException {
    String sql = read("V4__learning_profile_schema.sql");
    assertThat(sql).contains("ALTER TABLE learning_profiles RENAME TO learning_profile");
    assertThat(sql).contains("ADD COLUMN keyboard_layout");
    assertThat(sql).contains("CHECK (current_stage BETWEEN 1 AND 5)");
    assertThat(sql).contains("CREATE UNIQUE INDEX");
  }

  @Test
  void stageSchemaMatchesStory13() throws IOException {
    String sql = read("V5__stage_reference.sql");
    assertThat(sql).contains("CREATE TABLE stage (");
    assertThat(sql).contains("id BIGINT");
    assertThat(sql).contains("INSERT INTO stage");
    assertThat(sql).contains("Numpad and Math");
    assertThat(sql).contains("Hungarian Accents");
  }

  @Test
  void practiceSessionSchemaMatchesStory14() throws IOException {
    String sql = read("V6__practice_session.sql");
    assertThat(sql).contains("CREATE TABLE practice_session (");
    assertThat(sql).contains("exercise_kind");
    assertThat(sql).contains("CREATE INDEX idx_practice_session_user_submitted");
  }

  @Test
  void scoreRecordSchemaMatchesStory15() throws IOException {
    String sql = read("V7__score_record.sql");
    assertThat(sql).contains("CREATE TABLE score_record (");
    assertThat(sql).contains("attempt_kind");
    assertThat(sql).contains("(attempt_kind = 'SUB_EXAM' AND sub_exam_number IS NOT NULL)");
    assertThat(sql).contains("(attempt_kind = 'GRADUATION' AND sub_exam_number IS NULL)");
  }

  @Test
  void weakKeySchemaMatchesStory16() throws IOException {
    String sql = read("V8__weak_key_profile.sql");
    assertThat(sql).contains("CREATE TABLE weak_key_profile (");
    assertThat(sql).contains("PRIMARY KEY (user_id, key_char)");
    assertThat(sql).contains("CREATE INDEX idx_weak_key_user_miss");
  }

  @Test
  void graduationAttemptSchemaMatchesStory17() throws IOException {
    String sql = read("V9__graduation_attempt.sql");
    assertThat(sql).contains("CREATE TABLE graduation_attempt (");
    assertThat(sql).contains("'IN_PROGRESS', 'PASSED', 'FAILED'");
    assertThat(sql).contains("ADD COLUMN graduation_attempt_id");
    assertThat(sql).contains("graduation_attempt_id IS NULL OR attempt_kind = 'GRADUATION'");
  }

  @Test
  void scoreGraduationLinkBecomesMandatory() throws IOException {
    String sql = read("V10__score_graduation_link.sql");
    assertThat(sql).contains("DROP CONSTRAINT ck_score_record_graduation_link");
    assertThat(sql).contains("(attempt_kind = 'GRADUATION' AND graduation_attempt_id IS NOT NULL)");
    assertThat(sql).contains("(attempt_kind = 'SUB_EXAM' AND graduation_attempt_id IS NULL)");
  }

  @Test
  void graduationLifecycleTightening() throws IOException {
    String sql = read("V11__graduation_lifecycle_tightening.sql");
    assertThat(sql).contains("DROP CONSTRAINT ck_graduation_attempt_status");
    assertThat(sql).contains("'IN_PROGRESS', 'PASSED', 'FAILED', 'ABANDONED'");
    assertThat(sql).contains("ADD COLUMN component_index INTEGER");
    assertThat(sql).contains("(attempt_kind = 'SUB_EXAM' AND component_index IS NULL)");
    assertThat(sql).contains("(attempt_kind = 'GRADUATION' AND component_index BETWEEN 1 AND 3)");
    assertThat(sql).contains("ON score_record (graduation_attempt_id, component_index)");
    assertThat(sql).contains("WHERE graduation_attempt_id IS NOT NULL");
    assertThat(sql).contains("ON graduation_attempt (user_id, stage_id)");
    assertThat(sql).contains("WHERE status = 'IN_PROGRESS'");
  }

  @Test
  void userAccountMapsAppUser() {
    assertThat(UserAccount.class.getAnnotation(Table.class).name()).isEqualTo("app_user");
  }
}
