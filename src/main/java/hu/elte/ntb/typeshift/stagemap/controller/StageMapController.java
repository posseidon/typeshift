package hu.elte.ntb.typeshift.stagemap.controller;

import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import hu.elte.ntb.typeshift.exercise.service.CannedCatalog;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Serves the public map of every stage and catalog exercise. */
@Controller
@SuppressFBWarnings(
    value = "SPRING_ENDPOINT",
    justification = "Stage-map MVC routes are intentional public application endpoints.")
public class StageMapController {

  private final CannedCatalog catalog;

  public StageMapController(CannedCatalog catalog) {
    this.catalog = catalog;
  }

  /**
   * Renders the catalog as the application landing page and stage-map route.
   *
   * @param model the view model
   * @return the shared layout template
   */
  @GetMapping({"/", "/stage-map"})
  public String stageMap(Model model) {
    List<Integer> stageIds = CannedCatalog.STAGE_NAMES.keySet().stream().sorted().toList();
    Map<Integer, List<CannedExercise>> exercisesByStage = new LinkedHashMap<>();
    for (int stageId : stageIds) {
      exercisesByStage.put(stageId, catalog.byStage(stageId));
    }
    model.addAttribute("title", "title.stageMap");
    model.addAttribute("contentFragment", "stage-map");
    model.addAttribute("stageIds", stageIds);
    model.addAttribute("stageNames", CannedCatalog.STAGE_NAMES);
    model.addAttribute("exercisesByStage", exercisesByStage);
    return "layout";
  }
}
