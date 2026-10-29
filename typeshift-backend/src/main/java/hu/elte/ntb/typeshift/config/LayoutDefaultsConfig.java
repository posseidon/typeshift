package hu.elte.ntb.typeshift.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Shell defaults for every Thymeleaf view: the content fragment slot falls back
 * to the shift review dashboard unless a controller overrides it.
 */
@ControllerAdvice
public class LayoutDefaultsConfig {

  /**
   * Supplies the default layout content fragment.
   *
   * @param model the model to contribute to
   */
  @ModelAttribute
  public void contributeFragment(Model model) {
    model.addAttribute("contentFragment", "index");
  }
}
