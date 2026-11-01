package hu.elte.ntb.typeshift.signup.controller;

import hu.elte.ntb.typeshift.account.service.UserAccountService;
import hu.elte.ntb.typeshift.signup.model.SignupOutcome;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Signup route (Story 0.1): creates the account with its learning profile
 * atomically (Story 0.6) and forwards to the login view on success.
 */
@Controller
public class SignupController {

  private final UserAccountService accounts;

  public SignupController(UserAccountService accounts) {
    this.accounts = accounts;
  }

  /**
   * Renders the signup view through the layout shell.
   *
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/signup")
  public String signupView(Model model) {
    model.addAttribute("title", "title.signup");
    model.addAttribute("contentFragment", "signup");
    return "layout";
  }

  /**
   * Creates the account and forwards to login, or re-renders signup with the
   * refusal message.
   *
   * @param username requested unique login name
   * @param password plaintext password, hashed before storage
   * @param displayName optional profile display name
   * @param model the view model
   * @return redirect to login on success, else the signup view
   */
  @PostMapping("/signup")
  public String signup(
      @RequestParam("username") String username,
      @RequestParam("password") String password,
      @RequestParam(value = "displayName", required = false) String displayName,
      Model model) {
    SignupOutcome outcome = accounts.createAccount(username, password, displayName);
    if (outcome.created()) {
      return "redirect:/login";
    }
    model.addAttribute("title", "title.signup");
    model.addAttribute("contentFragment", "signup");
    model.addAttribute("error", signupErrorKey(outcome.error()));
    return "layout";
  }

  private static String signupErrorKey(String error) {
    return switch (error) {
      case "Username is already taken." -> "signup.usernameTaken";
      case "Username must be 3 to 30 characters and contain only letters, digits, or underscores." ->
          "signup.usernameInvalid";
      case "Password must be at least 8 characters." -> "signup.passwordInvalid";
      default -> error;
    };
  }
}
