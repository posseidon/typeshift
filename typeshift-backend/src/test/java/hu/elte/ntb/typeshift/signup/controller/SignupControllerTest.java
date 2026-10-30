package hu.elte.ntb.typeshift.signup.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
class SignupControllerTest {

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;

  @Test
  void duplicateUsernameShowsValidationMessage() throws Exception {
    if (!users.existsByUsername("taken_user")) {
      mvc.perform(
          post("/signup")
              .param("username", "taken_user")
              .param("password", "long-enough"));
    }
    mvc.perform(
            post("/signup").param("username", "taken_user").param("password", "long-enough"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Username is already taken.")));
  }

  @Test
  void badUsernameShowsValidationMessage() throws Exception {
    mvc.perform(post("/signup").param("username", "bad name!").param("password", "long-enough"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "Username must be 3 to 30 characters and contain only letters,"
                            + " digits, or underscores.")));
  }

  @Test
  void successfulSignupRedirectsToLogin() throws Exception {
    mvc.perform(
            post("/signup")
                .param("username", "fresh_user")
                .param("password", "long-enough")
                .param("displayName", "Fresh"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }
}
