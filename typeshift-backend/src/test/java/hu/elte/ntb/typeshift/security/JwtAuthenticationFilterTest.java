package hu.elte.ntb.typeshift.security;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import io.jsonwebtoken.Claims;
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
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false",
      "spring.datasource.hikari.maximum-pool-size=1",
      "typeshift.jwt-secret=test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms",
      "typeshift.cookie-secure=false"
    })
class JwtAuthenticationFilterTest {

  private static final String OWNER = "owner";

  @Autowired MockMvc mvc;
  @Autowired JwtTokenService tokens;
  @Autowired RevocationService revocations;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder encoder;

  @BeforeEach
  void seed() {
    if (!users.existsByUsername(OWNER)) {
      users.save(new UserAccount(OWNER, encoder.encode("password123"), OWNER));
    }
  }

  @Test
  void missingCookieRedirectsPageToLogin() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?auth=MISSING"));
  }

  @Test
  void missingCookieReturnsUnauthorizedForApi() throws Exception {
    mvc.perform(get("/api/whoami"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("MISSING")));
  }

  @Test
  void malformedCookieIsRejected() throws Exception {
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, "not-a-jwt")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("MALFORMED")));
  }

  @Test
  void expiredCookieIsRejected() throws Exception {
    String token = tokens.issue(OWNER, Duration.ofSeconds(-60));
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("EXPIRED")));
  }

  @Test
  void revokedCookieIsRejected() throws Exception {
    String token = tokens.issue(OWNER);
    Claims claims = tokens.parse(token);
    revocations.revoke(claims.getId(), claims.getExpiration().toInstant());
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("REVOKED")));
  }

  @Test
  void validCookieGrantsApiAccessWithUsernamePrincipal() throws Exception {
    String token = tokens.issue(OWNER);
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is(OWNER)));
  }

  @Test
  void validCookieGrantsPageAccess() throws Exception {
    String token = tokens.issue(OWNER);
    mvc.perform(get("/profile").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isOk());
  }
}
