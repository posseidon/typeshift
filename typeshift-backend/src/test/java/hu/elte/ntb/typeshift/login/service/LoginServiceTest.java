package hu.elte.ntb.typeshift.login.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.config.AuthProperties;
import hu.elte.ntb.typeshift.login.model.LoginResult;
import hu.elte.ntb.typeshift.security.JwtTokenService;
import hu.elte.ntb.typeshift.security.RevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

  @Mock UserAccountRepository accounts;
  @Mock PasswordEncoder passwordEncoder;
  @Mock JwtTokenService tokens;
  @Mock RevocationService revocations;
  @Mock LoginAttemptService loginAttempts;
  @Mock Claims claims;
  @Mock ExpiredJwtException expiredException;

  private LoginService service;

  @BeforeEach
  void setUp() {
    service =
        new LoginService(
            accounts,
            passwordEncoder,
            tokens,
            revocations,
            loginAttempts,
            new AuthProperties("test-secret", false));
  }

  @Test
  void blockedLoginReturnsFailureWithoutProbingCredentials() {
    when(loginAttempts.isBlocked("owner")).thenReturn(true);

    LoginResult result = service.authenticate("owner", "password123");

    assertFalse(result.success());
    assertNull(result.cookie());
    verify(accounts, never()).findByUsername(any());
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  void correctCredentialsIssueCookieAndResetFailures() {
    UserAccount account = new UserAccount("owner", "hash");
    when(loginAttempts.isBlocked("owner")).thenReturn(false);
    when(accounts.findByUsername("owner")).thenReturn(Optional.of(account));
    when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
    when(tokens.issue("owner")).thenReturn("signed-token");

    LoginResult result = service.authenticate("owner", "password123");

    assertTrue(result.success());
    assertEquals("signed-token", result.cookie().getValue());
    verify(loginAttempts).recordSuccess("owner");
  }

  @Test
  void wrongPasswordRecordsFailure() {
    UserAccount account = new UserAccount("owner", "hash");
    when(loginAttempts.isBlocked("owner")).thenReturn(false);
    when(accounts.findByUsername("owner")).thenReturn(Optional.of(account));
    when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

    LoginResult result = service.authenticate("owner", "wrong");

    assertFalse(result.success());
    assertNull(result.cookie());
    verify(loginAttempts).recordFailure("owner");
  }

  @Test
  void unknownUserRecordsFailureWithoutCheckingPassword() {
    when(loginAttempts.isBlocked("ghost")).thenReturn(false);
    when(accounts.findByUsername("ghost")).thenReturn(Optional.empty());

    LoginResult result = service.authenticate("ghost", "whatever1");

    assertFalse(result.success());
    verify(loginAttempts).recordFailure("ghost");
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  void usernameIsTrimmedBeforeLookup() {
    when(loginAttempts.isBlocked("owner")).thenReturn(false);
    when(accounts.findByUsername("owner")).thenReturn(Optional.empty());

    service.authenticate("  owner  ", "password123");

    verify(accounts).findByUsername("owner");
  }

  @Test
  void logoutRevokesJtiOfValidToken() {
    Instant expiry = Instant.parse("2026-11-01T00:00:00Z");
    when(claims.getId()).thenReturn("jti-1");
    when(claims.getExpiration()).thenReturn(Date.from(expiry));
    when(tokens.parse("token")).thenReturn(claims);

    service.logout("token");

    verify(revocations).revoke("jti-1", expiry);
  }

  @Test
  void logoutRevokesJtiOfExpiredToken() {
    Instant expiry = Instant.parse("2026-11-01T00:00:00Z");
    when(claims.getId()).thenReturn("jti-1");
    when(claims.getExpiration()).thenReturn(Date.from(expiry));
    when(expiredException.getClaims()).thenReturn(claims);
    when(tokens.parse("token")).thenThrow(expiredException);

    service.logout("token");

    verify(revocations).revoke("jti-1", expiry);
  }

  @Test
  void logoutIgnoresNullToken() {
    service.logout(null);

    verify(tokens, never()).parse(any());
  }

  @Test
  void logoutSwallowsUnusableToken() {
    when(tokens.parse("bad")).thenThrow(new JwtException("unusable"));

    service.logout("bad");

    verify(revocations, never()).revoke(any(), any());
  }
}
