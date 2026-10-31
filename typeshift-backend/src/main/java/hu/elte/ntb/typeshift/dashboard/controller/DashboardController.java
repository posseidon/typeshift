package hu.elte.ntb.typeshift.dashboard.controller;

import hu.elte.ntb.typeshift.dashboard.service.DashboardService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the shift review dashboard through the shared layout shell.
 */
@Controller
public class DashboardController {

  private final DashboardService dashboardService;

  /** Creates the dashboard controller. */
  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  /**
   * Renders the index view composed into the layout shell.
   *
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/")
  public String index(Model model, Principal principal) {
    model.addAttribute("dashboard", dashboardService.loadFor(principal.getName()));
    model.addAttribute("title", "TypeShift — Shift Review");
    model.addAttribute("contentFragment", "index");
    return "layout";
  }
}
