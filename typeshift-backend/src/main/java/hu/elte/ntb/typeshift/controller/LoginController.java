package hu.elte.ntb.typeshift.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Renders the form login view through the layout shell.
 */
@Controller
public class LoginController {

  /**
   * Renders the login view and reports failed attempts.
   *
   * @param error present after a failed login attempt
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/login")
  public String login(
      @RequestParam(value = "error", required = false) String error, Model model) {
    model.addAttribute("title", "TypeShift — Start of Shift");
    model.addAttribute("contentFragment", "login");
    model.addAttribute("error", error != null);
    return "layout";
  }
}
