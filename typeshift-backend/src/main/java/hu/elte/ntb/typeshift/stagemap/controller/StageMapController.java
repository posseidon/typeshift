package hu.elte.ntb.typeshift.stagemap.controller;

import hu.elte.ntb.typeshift.account.service.UserAccountService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the official stage map (career ladder) through the shared layout shell.
 */
@Controller
public class StageMapController {

  private final UserAccountService accounts;

  public StageMapController(UserAccountService accounts) {
    this.accounts = accounts;
  }

  /**
   * Renders the stage map composed into the layout shell, with each station's
   * approval state derived from the owner's current stage (ADR-013): stations
   * before the current one are certified, the current one is the active
   * assignment, and the rest stay locked.
   *
   * @param principal the authenticated owner (username)
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/stage-map")
  public String stageMap(Principal principal, Model model) {
    int currentStage = accounts.profileOf(principal.getName()).currentStage();
    model.addAttribute("title", "title.stageMap");
    model.addAttribute("contentFragment", "stage-map");
    model.addAttribute("currentStage", currentStage);
    return "layout";
  }
}
