package hu.elte.ntb.typeshift.login.service;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.config.AuthProperties;
import hu.elte.ntb.typeshift.login.model.LoginResult;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.security.JwtTokenService;
import hu.elte.ntb.typeshift.security.RevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authenticates login and ends sessions for ADR-001: verifies the credentials,
 * throttles repeated failures, issues the signed JWT in the session cookie,
 * and revokes the token on logout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginService {

  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService tokens;
  private final RevocationService revocations;
  private final LoginAttemptService loginAttempts;
  private final AuthProperties properties;

  /**
   * Verifies the credentials and builds the session cookie on success;
   * failures (including throttled blocks) return the generic refusal of
   * Story 0.2.
   *
   * @param username submitted login name
   * @param password submitted plaintext password
   * @return the login result carrying the cookie on success
   */
  public LoginResult authenticate(String username, String password) {
    String submitted = username == null ? "" : username.trim();
    if (loginAttempts.isBlocked(submitted)) {
      LOG.warn("Blocked login attempt, username length={}", submitted.length());
      return new LoginResult(false, null);
    }
    return accounts
        .findByUsername(submitted)
        .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()))
        .map(this::admit)
        .orElseGet(() -> failedLogin(submitted));
  }

  /**
   * Ends the session: revokes the presented jti (even for expired tokens);
   * always succeeds per Story 0.4.
   *
   * @param token the presented session cookie value, may be null
   */
  public void logout(String token) {
    if (token == null) {
      return;
    }
    try {
      Claims claims = tokens.parse(token);
      revocations.revoke(claims.getId(), claims.getExpiration().toInstant());
    } catch (ExpiredJwtException e) {
      revocations.revoke(e.getClaims().getId(), e.getClaims().getExpiration().toInstant());
    } catch (JwtException | IllegalArgumentException e) {
      LOG.info("Logout attempted with unusable token");
    }
  }

  private LoginResult admit(UserAccount account) {
    loginAttempts.recordSuccess(account.getUsername());
    String token = tokens.issue(account.getUsername());
    return new LoginResult(true, AuthCookie.create(token, properties.cookieSecure()));
  }

  private LoginResult failedLogin(String submitted) {
    loginAttempts.recordFailure(submitted);
    return new LoginResult(false, null);
  }
}
