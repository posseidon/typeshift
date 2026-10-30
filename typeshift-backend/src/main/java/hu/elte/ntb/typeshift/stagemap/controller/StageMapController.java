package hu.elte.ntb.typeshift.stagemap.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the official stage map (career ladder) through the shared layout shell.
 */
@Controller
public class StageMapController {

  /**
   * Renders the stage map view composed into the layout shell.
   *
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/stage-map")
  public String stageMap(Model model) {
    model.addAttribute("title", "TypeShift — Official Stage Map");
    model.addAttribute("contentFragment", "stage-map");
    return "layout";
  }
}
