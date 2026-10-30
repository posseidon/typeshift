package hu.elte.ntb.typeshift.profile.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
      "spring.datasource.hikari.maximum-pool-size=1",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "typeshift.jwt-secret=test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms",
      "typeshift.cookie-secure=false"
    })
class ProfileControllerTest {

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
  void profilePageShowsStatusData() throws Exception {
    mvc.perform(get("/profile").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("owner")))
        .andExpect(content().string(containsString("Member since")))
        .andExpect(content().string(containsString("HU_QWERTZ")));
  }

  @Test
  void displayNameUpdatePersists() throws Exception {
    mvc.perform(post("/profile/display-name").param("displayName", "Chief").cookie(session()))
        .andExpect(status().is3xxRedirection());
    mvc.perform(get("/profile").cookie(session()))
        .andExpect(content().string(containsString("Chief")));
  }

  @Test
  void wrongCurrentPasswordShowsMessage() throws Exception {
    String user = "wrongpass_user";
    if (!users.existsByUsername(user)) {
      users.save(new UserAccount(user, encoder.encode(PASSWORD), user));
    }
    cookieValue = tokens.issue(user);
    mvc.perform(
            post("/profile/password")
                .param("currentPassword", "not-my-pass")
                .param("newPassword", "brand-new-pass")
                .cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Current password is incorrect.")));
  }

  @Test
  void shortNewPasswordShowsMessage() throws Exception {
    String user = "shortpass_user";
    if (!users.existsByUsername(user)) {
      users.save(new UserAccount(user, encoder.encode(PASSWORD), user));
    }
    cookieValue = tokens.issue(user);
    mvc.perform(
            post("/profile/password")
                .param("currentPassword", PASSWORD)
                .param("newPassword", "short")
                .cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("New password must be at least 8 characters.")));
  }

  @Test
  void passwordChangeSucceedsWithCorrectCurrentPassword() throws Exception {
    String user = "changepass_user";
    if (!users.existsByUsername(user)) {
      users.save(new UserAccount(user, encoder.encode(PASSWORD), user));
    }
    cookieValue = tokens.issue(user);
    mvc.perform(
            post("/profile/password")
                .param("currentPassword", PASSWORD)
                .param("newPassword", "brand-new-pass")
                .cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Password changed.")));
    // Follow-up login with the new password proves the hash was swapped.
    mvc.perform(post("/login").param("username", user).param("password", "brand-new-pass"))
        .andExpect(status().is3xxRedirection());
  }
}
