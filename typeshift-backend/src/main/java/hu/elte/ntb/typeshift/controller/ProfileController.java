package hu.elte.ntb.typeshift.controller;

import hu.elte.ntb.typeshift.model.ChangePasswordOutcome;
import hu.elte.ntb.typeshift.model.ProfileView;
import hu.elte.ntb.typeshift.service.UserAccountService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Owner profile (Story 0.5): read-only status data, editable display name,
 * and password change requiring the current password.
 */
@Controller
public class ProfileController {

  private final UserAccountService accounts;

  public ProfileController(UserAccountService accounts) {
    this.accounts = accounts;
  }

  /**
   * Renders the profile view through the layout shell.
   *
   * @param principal the authenticated owner (username)
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/profile")
  public String view(Principal principal, Model model) {
    model.addAttribute("title", "TypeShift — Profile");
    model.addAttribute("contentFragment", "profile");
    addProfileData(principal.getName(), model);
    return "layout";
  }

  /**
   * Saves the display name and returns to the profile view.
   *
   * @param displayName the new display name, optional
   * @param principal the authenticated owner
   * @return redirect back to the profile view
   */
  @PostMapping("/profile/display-name")
  public String updateDisplayName(@RequestParam String displayName, Principal principal) {
    accounts.updateDisplayName(principal.getName(), displayName);
    return "redirect:/profile";
  }

  /**
   * Changes the password after verification; re-renders the profile with the
   * outcome banner on success or refusal.
   *
   * @param currentPassword the submitted current password
   * @param newPassword the replacement password
   * @param principal the authenticated owner
   * @param model the view model
   * @return the layout template name (profile fragment)
   */
  @PostMapping("/profile/password")
  public String changePassword(
      @RequestParam String currentPassword,
      @RequestParam String newPassword,
      Principal principal,
      Model model) {
    ChangePasswordOutcome outcome =
        accounts.changePassword(principal.getName(), currentPassword, newPassword);
    model.addAttribute("title", "TypeShift — Profile");
    model.addAttribute("contentFragment", "profile");
    model.addAttribute(
        "passwordMessage", outcome.success() ? "Password changed." : outcome.error());
    model.addAttribute("passwordError", !outcome.success());
    addProfileData(principal.getName(), model);
    return "layout";
  }

  private void addProfileData(String username, Model model) {
    ProfileView profile = accounts.profileOf(username);
    model.addAttribute("username", profile.username());
    model.addAttribute("displayName", profile.displayName());
    model.addAttribute("memberSince", profile.memberSince());
    model.addAttribute("currentStage", profile.currentStage());
  }
}
