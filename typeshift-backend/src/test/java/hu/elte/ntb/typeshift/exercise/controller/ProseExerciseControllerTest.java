package hu.elte.ntb.typeshift.exercise.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.security.JwtTokenService;
import jakarta.servlet.http.Cookie;
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
class ProseExerciseControllerTest {

  private static final String OWNER = "owner";
  private static final String PASSWORD = "password123";

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired JwtTokenService tokens;
  @Autowired JdbcTemplate jdbc;

  private String cookieValue;

  @BeforeEach
  void seedAndCookie() {
    if (!users.existsByUsername(OWNER)) {
      users.save(new UserAccount(OWNER, encoder.encode(PASSWORD), "Ops Chief"));
    }
    cookieValue = tokens.issue(OWNER);
    jdbc.execute(
        "CREATE TABLE IF NOT EXISTS stage (id BIGINT PRIMARY KEY, name VARCHAR(64), "
            + "sub_exam_count INTEGER, graduation_name VARCHAR(128))");
    jdbc.update(
        "INSERT OR IGNORE INTO stage (id, name, sub_exam_count, graduation_name) "
            + "VALUES (1, 'QWERTZ Core', 3, 'Graduation')");
    jdbc.execute("DROP TABLE IF EXISTS practice_session");
    jdbc.execute(
        "CREATE TABLE practice_session (id INTEGER PRIMARY KEY, user_id BIGINT, "
            + "stage_id BIGINT, exercise_kind VARCHAR(8), wpm DECIMAL, cpm DECIMAL, "
            + "correct_characters INTEGER, total_characters INTEGER, duration_seconds INTEGER, "
            + "submitted_at TIMESTAMP)");
    jdbc.execute("DROP TABLE IF EXISTS score_record");
    jdbc.execute(
        "CREATE TABLE score_record (id INTEGER PRIMARY KEY, user_id BIGINT, "
            + "stage_id BIGINT, attempt_kind VARCHAR(16), sub_exam_number INTEGER, "
            + "accuracy DECIMAL, substitutions INTEGER, insertions INTEGER, deletions INTEGER, "
            + "passed BOOLEAN, submitted_at TIMESTAMP)");
  }

  private Cookie session() {
    return new Cookie(AuthCookie.NAME, cookieValue);
  }

  @Test
  void proseDocketServesStageFourText() throws Exception {
    mvc.perform(get("/exercise/4/exam-1").param("lang", "en").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("A hivatalos iratokat")))
        .andExpect(content().string(containsString("Stage 4")));
  }

  @Test
  void proseDocketServesTargetText() throws Exception {
    mvc.perform(get("/exercise/4/exam-1").param("lang", "en").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"typing-docket\"")))
        .andExpect(content().string(containsString("data-target=\"A hivatalos iratokat")))
        .andExpect(content().string(containsString("data-mode=\"prose\"")))
        .andExpect(content().string(not(containsString("data-expected"))))
        .andExpect(content().string(containsString("<textarea id=\"practice-input\"")))
        .andExpect(content().string(containsString("New copy")))
        .andExpect(content().string(containsString("Save result")));
    mvc.perform(get("/exercise/2/grad").param("lang", "en").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("magas\nTárgy: Bejelentkezési hiba")));
    mvc.perform(get("/exercise/5/exam-1").param("lang", "en").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("docket--chat-burst")));
  }

  @Test
  void completedExerciseSavesPracticeAndScoreRecords() throws Exception {
    String typedText =
        "A hivatalos iratokat kérjük a főbejárat melletti iktatóba eljuttatni még a "
            + "mai nap folyamán. Az ügyintézés határideje tizenöt munkanap, kivéve ha "
            + "sürgősségi kérelmet nyújtott be az illetékes osztályvezetőnél...";

    mvc.perform(
            post("/exercise/4/exam-1/submit")
                .cookie(session())
                .param("typedText", typedText)
                .param("elapsedSeconds", "53"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));

    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM practice_session WHERE user_id = "
                + "(SELECT id FROM app_user WHERE username = ?)",
            Integer.class,
            OWNER));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM score_record WHERE user_id = "
                + "(SELECT id FROM app_user WHERE username = ?) AND passed = TRUE",
            Integer.class,
            OWNER));
  }

  @Test
  void numericDocketServesKeypad() throws Exception {
    mvc.perform(get("/exercise/3/exam-1").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-mode=\"numeric\"")))
        .andExpect(content().string(containsString("id=\"keypad\"")));
  }

  @Test
  void numericDocketServesPadAndVerdict() throws Exception {
    mvc.perform(get("/exercise/3/exam-1").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"math-verdict\"")))
        .andExpect(content().string(containsString("id=\"practice-pad\"")))
        .andExpect(content().string(containsString("data-code=\"NumpadDecimal\"")))
        .andExpect(content().string(containsString("NumLock")));
  }

  @Test
  void proseDocketOmitsKeyPad() throws Exception {
    mvc.perform(get("/exercise/4/exam-1").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("id=\"keypad\""))))
        .andExpect(content().string(not(containsString("id=\"practice-pad\""))))
        .andExpect(content().string(not(containsString("NumLock"))));
  }

  @Test
  void graduationDocketServes() throws Exception {
    mvc.perform(get("/exercise/4/grad").cookie(session())).andExpect(status().isOk());
  }

  @Test
  void unknownStageGivesNotFound() throws Exception {
    mvc.perform(get("/exercise/9/exam-1").cookie(session())).andExpect(status().isNotFound());
  }

  @Test
  void unknownExamKeyGivesNotFound() throws Exception {
    mvc.perform(get("/exercise/4/nope").cookie(session())).andExpect(status().isNotFound());
  }

  @Test
  void oldRouteRedirectsToCurrentAssignment() throws Exception {
    mvc.perform(get("/exercise").cookie(session()))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", containsString("/exercise/4/exam-1")));
  }

  @Test
  void unauthenticatedRequestIsRebuffed() throws Exception {
    mvc.perform(get("/exercise/4/exam-1"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?auth=MISSING"));
  }
}
