package hu.elte.ntb.typeshift.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the shift review dashboard through the shared layout shell.
 */
@Controller
public class IndexController {

  /**
   * Renders the index view composed into the layout shell.
   *
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("title", "TypeShift — Shift Review");
    model.addAttribute("contentFragment", "index");
    return "layout";
  }
}
