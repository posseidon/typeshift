package hu.elte.ntb.typeshift.login.controller;

import hu.elte.ntb.typeshift.login.model.LoginResult;
import hu.elte.ntb.typeshift.login.service.LoginService;
import hu.elte.ntb.typeshift.security.AuthCookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles the login and logout routes of ADR-001, delegating the credential
 * verification, throttling, token issue, and revocation to {@link LoginService}.
 */
@Controller
@RequiredArgsConstructor
public class LoginController {

  private final LoginService logins;

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
   * Verifies the credentials through the login service and sets the session
   * cookie on success; failures re-render the login view with the error.
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
    LoginResult result = logins.authenticate(username, password);
    if (!result.success()) {
      return renderLogin(model, "Invalid username or password.");
    }
    response.addHeader(HttpHeaders.SET_COOKIE, result.cookie().toString());
    return "redirect:/";
  }

  /**
   * Ends the session: delegates token revocation and clears the cookie;
   * always succeeds per Story 0.4.
   *
   * @param token the presented session cookie value, may be absent
   * @param response the response to attach the clearing cookie to
   * @return redirect to the login view
   */
  @GetMapping("/logout")
  public String logout(
      @CookieValue(name = AuthCookie.NAME, required = false) String token,
      HttpServletResponse response) {
    logins.logout(token);
    response.addHeader(HttpHeaders.SET_COOKIE, AuthCookie.clear().toString());
    return "redirect:/login";
  }

  private String renderLogin(Model model, String errorText) {
    model.addAttribute("title", "TypeShift — Start of Shift");
    model.addAttribute("contentFragment", "login");
    model.addAttribute("error", true);
    model.addAttribute("errorText", errorText);
    return "layout";
  }
}
