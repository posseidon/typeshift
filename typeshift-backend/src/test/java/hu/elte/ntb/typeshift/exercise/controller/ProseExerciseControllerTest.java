package hu.elte.ntb.typeshift.exercise.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

  private String cookieValue;

  @BeforeEach
  void seedAndCookie() {
    if (!users.existsByUsername(OWNER)) {
      users.save(new UserAccount(OWNER, encoder.encode(PASSWORD), "Ops Chief"));
    }
    cookieValue = tokens.issue(OWNER);
  }

  private Cookie session() {
    return new Cookie(AuthCookie.NAME, cookieValue);
  }

  @Test
  void proseDocketServesStageFourText() throws Exception {
    mvc.perform(get("/exercise/4/exam-1").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("A hivatalos iratokat")))
        .andExpect(content().string(containsString("Stage 4")));
  }

  @Test
  void numericDocketServesKeypad() throws Exception {
    mvc.perform(get("/exercise/3/exam-1").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-mode=\"numeric\"")))
        .andExpect(content().string(containsString("id=\"keypad\"")));
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
