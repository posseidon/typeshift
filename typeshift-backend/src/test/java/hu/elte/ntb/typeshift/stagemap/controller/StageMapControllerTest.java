package hu.elte.ntb.typeshift.stagemap.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.exercise.service.CannedCatalog;
import hu.elte.ntb.typeshift.learning.model.LearningProfile;
import hu.elte.ntb.typeshift.learning.repository.LearningProfileRepository;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.security.JwtTokenService;
import jakarta.servlet.http.Cookie;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
class StageMapControllerTest {

  private static final String OWNER = "owner";
  private static final String PASSWORD = "password123";

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired JwtTokenService tokens;
  @Autowired LearningProfileRepository profiles;

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
  void everyStationCardLinksToItsPracticeDockets() throws Exception {
    mvc.perform(get("/stage-map").param("lang", "en").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Practice dockets")))
        .andExpect(content().string(containsString("/exercise/1/exam-1")))
        .andExpect(content().string(containsString("/exercise/1/exam-2")))
        .andExpect(content().string(containsString("/exercise/1/exam-3")))
        .andExpect(content().string(containsString("/exercise/1/grad")))
        .andExpect(content().string(containsString("/exercise/2/exam-1")))
        .andExpect(content().string(containsString("/exercise/2/exam-2")))
        .andExpect(content().string(containsString("/exercise/2/grad")))
        .andExpect(content().string(containsString("/exercise/3/exam-1")))
        .andExpect(content().string(containsString("/exercise/3/exam-2")))
        .andExpect(content().string(containsString("/exercise/3/grad")))
        .andExpect(content().string(containsString("/exercise/4/exam-1")))
        .andExpect(content().string(containsString("/exercise/4/exam-2")))
        .andExpect(content().string(containsString("/exercise/4/grad")))
        .andExpect(content().string(containsString("/exercise/5/exam-1")))
        .andExpect(content().string(containsString("/exercise/5/exam-2")))
        .andExpect(content().string(containsString("/exercise/5/exam-3")))
        .andExpect(content().string(containsString("/exercise/5/exam-4")))
        .andExpect(content().string(containsString("/exercise/5/grad")));
  }

  @ParameterizedTest
  @MethodSource("catalogEntries")
  void everyCatalogEntryHasItsPracticeLink(int stageId, String examKey) throws Exception {
    mvc.perform(get("/stage-map").param("lang", "en").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(
            content().string(containsString("href=\"/exercise/" + stageId + "/" + examKey + "\"")));
  }

  static Stream<Arguments> catalogEntries() {
    CannedCatalog catalog = new CannedCatalog();
    return IntStream.rangeClosed(1, 5)
        .boxed()
        .flatMap(
            stage ->
                catalog.byStage(stage).stream()
                    .map(exercise -> Arguments.of(stage, exercise.examKey())));
  }

  @Test
  void freshAccountShowsStageOneActiveAndRemainderLocked() throws Exception {
    String username = "freshmap";
    users.save(new UserAccount(username, encoder.encode(PASSWORD), username));
    Cookie cookie = new Cookie(AuthCookie.NAME, tokens.issue(username));

    mvc.perform(get("/stage-map").param("lang", "en").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("STAGE 01 OF 05 DEPLOYED")))
        .andExpect(content().string(containsString("Active assignment")))
        .andExpect(content().string(containsString("Locked — requires stage 1 clearance")));
  }

  @Test
  void progressedAccountShowsStagesBeforeCurrentAsCertified() throws Exception {
    String username = "progmap";
    UserAccount account = users.save(new UserAccount(username, encoder.encode(PASSWORD), username));
    LearningProfile profile = new LearningProfile(account);
    profile.setCurrentStage(4);
    profile.setUnlockedStages(new HashSet<>(Set.of(1, 2, 3, 4)));
    profiles.save(profile);
    Cookie cookie = new Cookie(AuthCookie.NAME, tokens.issue(username));

    mvc.perform(get("/stage-map").param("lang", "en").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("STAGE 04 OF 05 DEPLOYED")))
        .andExpect(content().string(containsString("Certified record")))
        .andExpect(content().string(containsString("Active assignment")))
        .andExpect(content().string(containsString("Locked — requires stage 4 clearance")));
  }
}
