package hu.elte.ntb.typeshift.controller;

import hu.elte.ntb.typeshift.model.SignupOutcome;
import hu.elte.ntb.typeshift.service.UserAccountService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Minimal signup route: creates an account with a BCrypt-hashed password and
 * refuses duplicate usernames.
 */
@Controller
@Validated
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
    model.addAttribute("title", "TypeShift — New Employment Record");
    model.addAttribute("contentFragment", "signup");
    return "layout";
  }

  /**
   * Creates the account and forwards to login, or re-renders signup with the refusal.
   *
   * @param username requested unique login name
   * @param password plaintext password, hashed before storage
   * @param model the view model
   * @return redirect to login on success, else the signup view
   */
  @PostMapping("/signup")
  public String signup(
      @RequestParam("username") @NotBlank String username,
      @RequestParam("password") @NotBlank String password,
      Model model) {
    SignupOutcome outcome = accounts.createAccount(username, password);
    if (outcome.created()) {
      return "redirect:/login?signup";
    }
    model.addAttribute("title", "TypeShift — New Employment Record");
    model.addAttribute("contentFragment", "signup");
    model.addAttribute("error", outcome.error());
    return "layout";
  }
}
