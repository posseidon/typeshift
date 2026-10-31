package hu.elte.ntb.typeshift.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.security.JwtTokenService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite::memory:",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.datasource.hikari.maximum-pool-size=1",
      "typeshift.jwt-secret=test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms",
      "typeshift.cookie-secure=false"
    })
class DashboardControllerTest {

  private static final String OWNER = "dashboard-owner";
  private static final String OTHER = "dashboard-other";
  private static final String PASSWORD = "password123";

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired JwtTokenService tokens;
  @Autowired JdbcTemplate jdbc;

  private long ownerId;
  private long otherId;
  private Cookie ownerSession;

  @BeforeEach
  void seedDatabase() {
    ensureAccount(OWNER, "Dashboard Owner");
    ensureAccount(OTHER, "Other Account");
    ownerId = userId(OWNER);
    otherId = userId(OTHER);
    ownerSession = new Cookie(AuthCookie.NAME, tokens.issue(OWNER));
    createReadTables();
    clearData();
    seedStages();
  }

  @Test
  void emptyDatabaseShowsHonestDashboard() throws Exception {
    String body = page().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    org.assertj.core.api.Assertions.assertThat(body)
        .contains(
            "No practice sessions yet",
            "No score attempts yet",
            "No weak-key data yet",
            "No WPM history yet",
            "STAGE <span>1</span>",
            "minutes",
            "<span class=\"metric-counter\">0</span>",
            "QWERTZ Core")
        .doesNotContain(
            "68",
            "97.4%",
            "142",
            "1958",
            "TS-40892",
            "#HU-40892",
            "CH-882",
            "01-992",
            "65%",
            "3/5 PASS",
            "data-wpm-point");
  }

  @Test
  void dashboardShowsOnlyCurrentOwnersPersistedMetricsAndRows() throws Exception {
    insertProfile(ownerId, 2, 27, List.of(1, 2));
    for (int index = 1; index <= 8; index++) {
      insertSession(index, ownerId, 2, 40 + index, timestamp(index));
    }
    insertScore(1, ownerId, 2, "SUB_EXAM", 1, "0.9740", true, timestamp(2), null);
    insertScore(2, ownerId, 2, "SUB_EXAM", 2, "0.9000", false, timestamp(3), null);
    insertSession(20, otherId, 5, 211.25, timestamp(9));
    insertScore(20, otherId, 5, "SUB_EXAM", 1, "0.3330", false, timestamp(9), null);
    jdbc.update(
        "INSERT INTO weak_key_profile (user_id, key_char, miss_count, updated_at) VALUES (?, ?, ?, ?)",
        ownerId,
        "ő",
        12,
        timestamp(8));
    jdbc.update(
        "INSERT INTO weak_key_profile (user_id, key_char, miss_count, updated_at) VALUES (?, ?, ?, ?)",
        otherId,
        "XQ",
        82123,
        timestamp(9));

    String body = page().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

    org.assertj.core.api.Assertions.assertThat(body)
        .contains(
            "Dashboard Owner",
            "48.00",
            "8 sessions",
            "93.7%",
            "1 / 2",
            "ő",
            "12",
            "Sub-exam 1",
            "97.4%",
            "Sub-exam 2",
            "42.00")
        .doesNotContain("211.25", "33.3%", "XQ", "82123");
    org.assertj.core.api.Assertions.assertThat(occurrences(body, "data-wpm-point")).isEqualTo(7);
    org.assertj.core.api.Assertions.assertThat(body.lastIndexOf("42.00"))
        .isLessThan(body.lastIndexOf("48.00"));
    org.assertj.core.api.Assertions.assertThat(body).doesNotContain("41.00");
  }

  @Test
  void stageIsGraduatedOnlyWhenPassingGraduationScoreExists() throws Exception {
    insertProfile(ownerId, 3, 18, List.of(1, 2, 3));

    String beforePass = page().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(beforePass).contains("CURRENT", "UNLOCKED");
    org.assertj.core.api.Assertions.assertThat(occurrences(beforePass, "GRADUATED")).isZero();
    org.assertj.core.api.Assertions.assertThat(beforePass).doesNotContain("Approved");

    jdbc.update(
        "INSERT INTO graduation_attempt (id, user_id, stage_id, status, started_at, finished_at, passed) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
        1,
        ownerId,
        1,
        "PASSED",
        timestamp(1),
        timestamp(2),
        true);
    insertScore(3, ownerId, 1, "GRADUATION", null, "0.9800", true, timestamp(2), 1L);

    String afterPass = page().andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    org.assertj.core.api.Assertions.assertThat(occurrences(afterPass, "GRADUATED")).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(afterPass).contains("QWERTZ Core", "GRADUATED");
  }

  @Test
  void dashboardRequiresAuthentication() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?auth=MISSING"));
  }

  private org.springframework.test.web.servlet.ResultActions page() throws Exception {
    return mvc.perform(get("/").cookie(ownerSession));
  }

  private void ensureAccount(String username, String displayName) {
    if (!users.existsByUsername(username)) {
      users.save(new UserAccount(username, encoder.encode(PASSWORD), displayName));
    }
  }

  private long userId(String username) {
    return jdbc.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username);
  }

  private void createReadTables() {
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS stage (id INTEGER PRIMARY KEY, name TEXT NOT NULL, "
            + "sub_exam_count INTEGER NOT NULL, graduation_name TEXT NOT NULL)");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS practice_session (id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL, "
            + "stage_id INTEGER NOT NULL, exercise_kind TEXT NOT NULL, wpm NUMERIC NOT NULL, "
            + "cpm NUMERIC NOT NULL, correct_characters INTEGER NOT NULL, total_characters INTEGER NOT NULL, "
            + "duration_seconds INTEGER NOT NULL, submitted_at TEXT NOT NULL)");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS graduation_attempt (id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL, "
            + "stage_id INTEGER NOT NULL, status TEXT NOT NULL, started_at TEXT NOT NULL, "
            + "finished_at TEXT, passed BOOLEAN NOT NULL)");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS score_record (id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL, "
            + "stage_id INTEGER NOT NULL, attempt_kind TEXT NOT NULL, sub_exam_number INTEGER, "
            + "accuracy NUMERIC NOT NULL, passed BOOLEAN NOT NULL, submitted_at TEXT NOT NULL, "
            + "graduation_attempt_id INTEGER)");
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS weak_key_profile (user_id INTEGER NOT NULL, key_char TEXT NOT NULL, "
            + "miss_count INTEGER NOT NULL, updated_at TEXT NOT NULL)");
  }

  private void clearData() {
    jdbc.update("DELETE FROM score_record");
    jdbc.update("DELETE FROM practice_session");
    jdbc.update("DELETE FROM weak_key_profile");
    jdbc.update("DELETE FROM graduation_attempt");
    jdbc.update("DELETE FROM learning_profile_unlocked_stages");
    jdbc.update("DELETE FROM learning_profile");
    jdbc.update("DELETE FROM stage");
  }

  private void seedStages() {
    jdbc.update(
        "INSERT INTO stage (id, name, sub_exam_count, graduation_name) VALUES "
            + "(1, 'QWERTZ Core', 3, 'Core passage'), "
            + "(2, 'Symbol Sorcerer', 2, 'Technical ticket'), "
            + "(3, 'Numpad and Math', 2, 'Cashier Rush'), "
            + "(4, 'Hungarian Accents', 2, 'Formal request'), "
            + "(5, 'Real-World Tasks', 4, 'Random tasks')");
  }

  private void insertProfile(long userId, int currentStage, int minutes, List<Integer> unlocked) {
    jdbc.update(
        "INSERT INTO learning_profile "
            + "(id, user_id, current_stage, total_practice_minutes, keyboard_layout, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?)",
        userId,
        userId,
        currentStage,
        minutes,
        "HU_QWERTZ",
        timestamp(1));
    for (int stage : unlocked) {
      jdbc.update(
          "INSERT INTO learning_profile_unlocked_stages (learning_profile_id, stage) VALUES (?, ?)",
          userId,
          stage);
    }
  }

  private void insertSession(long id, long userId, int stageId, double wpm, String submittedAt) {
    jdbc.update(
        "INSERT INTO practice_session "
            + "(id, user_id, stage_id, exercise_kind, wpm, cpm, correct_characters, total_characters, "
            + "duration_seconds, submitted_at) VALUES (?, ?, ?, 'PROSE', ?, 0, 0, 0, 0, ?)",
        id,
        userId,
        stageId,
        wpm,
        submittedAt);
  }

  private void insertScore(
      long id,
      long userId,
      int stageId,
      String kind,
      Integer subExamNumber,
      String accuracy,
      boolean passed,
      String submittedAt,
      Long graduationAttemptId) {
    jdbc.update(
        "INSERT INTO score_record "
            + "(id, user_id, stage_id, attempt_kind, sub_exam_number, accuracy, passed, submitted_at, "
            + "graduation_attempt_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        userId,
        stageId,
        kind,
        subExamNumber,
        accuracy,
        passed,
        submittedAt,
        graduationAttemptId);
  }

  private String timestamp(int day) {
    return "2026-10-%02dT12:00:00".formatted(day);
  }

  private int occurrences(String text, String fragment) {
    return text.split(fragment, -1).length - 1;
  }
}
