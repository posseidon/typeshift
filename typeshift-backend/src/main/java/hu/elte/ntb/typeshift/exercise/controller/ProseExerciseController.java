package hu.elte.ntb.typeshift.exercise.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the prose exercise screen through the shared layout shell.
 */
@Controller
public class ProseExerciseController {

  /**
   * Renders the exercise view composed into the layout shell.
   *
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/exercise")
  public String exercise(Model model) {
    model.addAttribute("title", "TypeShift — Prose Exercise");
    model.addAttribute("contentFragment", "exercise");
    return "layout";
  }
}
