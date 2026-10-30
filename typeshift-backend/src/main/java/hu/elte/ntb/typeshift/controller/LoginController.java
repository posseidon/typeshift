package hu.elte.ntb.typeshift.controller;

import hu.elte.ntb.typeshift.config.AuthProperties;
import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.service.JwtTokenService;
import hu.elte.ntb.typeshift.service.LoginAttemptService;
import hu.elte.ntb.typeshift.service.RevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles the login and logout flows of ADR-001: POST /login verifies the
 * credentials, throttles repeated failures, and issues the signed JWT in the
 * session cookie; GET /logout revokes the token and clears the cookie; both
 * render through the layout shell.
 */
@Controller
public class LoginController {

  private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService tokens;
  private final RevocationService revocations;
  private final LoginAttemptService loginAttempts;
  private final AuthProperties properties;

  public LoginController(
      UserAccountRepository accounts,
      PasswordEncoder passwordEncoder,
      JwtTokenService tokens,
      RevocationService revocations,
      LoginAttemptService loginAttempts,
      AuthProperties properties) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.tokens = tokens;
    this.revocations = revocations;
    this.loginAttempts = loginAttempts;
    this.properties = properties;
  }

  /**
   * Renders the login view; shows the generic failure message when a failed
   * verification or auth redirect flagged the query.
   *
   * @param authFlag reason code appended by the auth filter, if any
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/login")
  public String login(@RequestParam(value = "auth", required = false) String authFlag, Model model) {
    model.addAttribute("title", "TypeShift — Start of Shift");
    model.addAttribute("contentFragment", "login");
    if (authFlag == null) {
      model.addAttribute("error", false);
    } else {
      model.addAttribute("error", true);
      model.addAttribute("errorText", "Session expired or revoked. Please sign in again.");
    }
    return "layout";
  }

  /**
   * Verifies the credentials and issues the session cookie on success;
   * failures (including throttled blocks) show the single generic message of
   * Story 0.2.
   *
   * @param username submitted login name
   * @param password submitted plaintext password
   * @param response the response to attach the cookie to
   * @param model the view model
   * @return redirect to the shell view, or the login view with the error
   */
  @PostMapping("/login")
  public String login(
      @RequestParam String username,
      @RequestParam String password,
      HttpServletResponse response,
      Model model) {
    String submitted = username == null ? "" : username.trim();
    if (loginAttempts.isBlocked(submitted)) {
      LOG.warn("Blocked login attempt, username length={}", submitted.length());
      return renderLogin(model, "Invalid username or password.");
    }
    return accounts
        .findByUsername(submitted)
        .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()))
        .map(account -> admit(response, account))
        .orElseGet(() -> failedLogin(submitted, model));
  }

  /**
   * Ends the session: revokes the presented jti (even for expired tokens) and
   * clears the cookie; always succeeds per Story 0.4.
   *
   * @param token the presented session cookie value, may be absent
   * @param response the response to attach the clearing cookie to
   * @return redirect to the login view
   */
  @GetMapping("/logout")
  public String logout(
      @CookieValue(name = AuthCookie.NAME, required = false) String token,
      HttpServletResponse response) {
    if (token != null) {
      try {
        Claims claims = tokens.parse(token);
        revocations.revoke(claims.getId(), claims.getExpiration().toInstant());
      } catch (ExpiredJwtException e) {
        revocations.revoke(e.getClaims().getId(), e.getClaims().getExpiration().toInstant());
      } catch (JwtException | IllegalArgumentException e) {
        LOG.info("Logout attempted with unusable token");
      }
    }
    response.addHeader(HttpHeaders.SET_COOKIE, AuthCookie.clear().toString());
    return "redirect:/login";
  }

  private String admit(HttpServletResponse response, UserAccount account) {
    loginAttempts.recordSuccess(account.getUsername());
    String token = tokens.issue(account.getUsername());
    response.addHeader(
        HttpHeaders.SET_COOKIE, AuthCookie.create(token, properties.cookieSecure()).toString());
    return "redirect:/";
  }

  private String failedLogin(String submitted, Model model) {
    loginAttempts.recordFailure(submitted);
    return renderLogin(model, "Invalid username or password.");
  }

  private String renderLogin(Model model, String errorText) {
    model.addAttribute("title", "TypeShift — Start of Shift");
    model.addAttribute("contentFragment", "login");
    model.addAttribute("error", true);
    model.addAttribute("errorText", errorText);
    return "layout";
  }
}
