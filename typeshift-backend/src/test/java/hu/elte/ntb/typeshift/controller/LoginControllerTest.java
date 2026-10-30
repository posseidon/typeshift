package hu.elte.ntb.typeshift.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.service.JwtTokenService;
import hu.elte.ntb.typeshift.service.RevocationService;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
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
class LoginControllerTest {

  private static final String OWNER = "owner";
  private static final String PASSWORD = "password123";

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired JwtTokenService tokens;
  @Autowired RevocationService revocations;

  @BeforeEach
  void seed() {
    if (!users.existsByUsername(OWNER)) {
      users.save(new UserAccount(OWNER, encoder.encode(PASSWORD), OWNER));
    }
  }

  @Test
  void failedLoginShowsGenericMessage() throws Exception {
    mvc.perform(post("/login").param("username", OWNER).param("password", "wrong-pass"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Invalid username or password.")));
  }

  @Test
  void failedLoginShowsSameMessageForUnknownUser() throws Exception {
    mvc.perform(post("/login").param("username", "ghost").param("password", "whatever1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Invalid username or password.")));
  }

  @Test
  void successfulLoginSetsSessionCookieAndRedirectsHome() throws Exception {
    mvc.perform(post("/login").param("username", OWNER).param("password", PASSWORD))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
        .andExpect(cookie().exists(AuthCookie.NAME))
        .andExpect(cookie().httpOnly(AuthCookie.NAME, true))
        .andExpect(cookie().secure(AuthCookie.NAME, false))
        .andExpect(cookie().maxAge(AuthCookie.NAME, 2592000));
  }

  @Test
  void blockedUserGetsGenericMessageEvenWithCorrectPassword() throws Exception {
    String user = "blockme";
    if (!users.existsByUsername(user)) {
      users.save(new UserAccount(user, encoder.encode(PASSWORD), user));
    }
    String wrong = "wrong-pass";
    for (int i = 0; i < 5; i++) {
      mvc.perform(post("/login").param("username", user).param("password", wrong))
          .andExpect(status().isOk());
    }
    mvc.perform(post("/login").param("username", user).param("password", PASSWORD))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Invalid username or password.")));
  }

  @Test
  void logoutRevokesTokenAndClearsCookie() throws Exception {
    String token =
        mvc.perform(post("/login").param("username", OWNER).param("password", PASSWORD))
            .andReturn()
            .getResponse()
            .getCookie(AuthCookie.NAME)
            .getValue();

    mvc.perform(get("/logout").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"))
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));

    // Reuse must fail with 401 REVOKED (Story 0.4 / 0.3 handshake).
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutWithoutCookieStillSucceeds() throws Exception {
    mvc.perform(get("/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"))
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
  }

  @Test
  void logoutWithExpiredTokenStillSucceedsAndClearsCookie() throws Exception {
    String token = tokens.issue(OWNER, Duration.ofSeconds(-60));
    String jti = expiredJti(token);
    mvc.perform(get("/logout").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"))
        .andExpect(cookie().maxAge(AuthCookie.NAME, 0));
    assertTrue(revocations.isRevoked(jti), "expired jti got revoked on logout");
  }

  private String expiredJti(String token) {
    try {
      return tokens.parse(token).getId();
    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      return e.getClaims().getId();
    }
  }
}
