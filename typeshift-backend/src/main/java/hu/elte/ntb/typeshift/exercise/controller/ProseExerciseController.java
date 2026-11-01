package hu.elte.ntb.typeshift.exercise.controller;

import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import hu.elte.ntb.typeshift.exercise.service.CannedCatalog;
import hu.elte.ntb.typeshift.exercise.service.ExerciseSubmissionService;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serves the canned exercise dockets of all five stations through the shared
 * layout shell, one parameterized route for every catalog entry.
 */
@Controller
public class ProseExerciseController {

  /** Docket framing copy shown under the docket number, keyed by stage and exam. */
  private static final Map<String, String> FRAMING = Map.ofEntries(
      Map.entry("1/exam-1", "home row drill transcription"),
      Map.entry("1/exam-2", "top row drill transcription"),
      Map.entry("1/exam-3", "bottom row drill transcription"),
      Map.entry("1/grad", "3-minute continuous QWERTZ passage"),
      Map.entry("2/exam-1", "punctuation & hyphenation transcription"),
      Map.entry("2/exam-2", "AltGr symbol transcription"),
      Map.entry("2/grad", "formatted support ticket transcription"),
      Map.entry("3/exam-1", "keypad tallying evaluation"),
      Map.entry("3/exam-2", "ledger arithmetic evaluation"),
      Map.entry("3/grad", "cashier rush evaluation"),
      Map.entry("4/exam-1", "Hungarian prose transcribing [civil service record]"),
      Map.entry("4/exam-2", "accented civic notice transcription"),
      Map.entry("4/grad", "formal request letter transcription"),
      Map.entry("5/exam-1", "chat burst transcription"),
      Map.entry("5/exam-2", "email reply composition"),
      Map.entry("5/exam-3", "document paragraph transcription"),
      Map.entry("5/exam-4", "code comment transcription"),
      Map.entry("5/grad", "three-task sequence run"));

  private final CannedCatalog catalog;
  private final ExerciseSubmissionService submissions;

  public ProseExerciseController(CannedCatalog catalog, ExerciseSubmissionService submissions) {
    this.catalog = catalog;
    this.submissions = submissions;
  }

  /**
   * Redirects the old exercise route to the current Stage 4 assignment.
   *
   * @return redirect to the Stage 4 exam-1 docket
   */
  @GetMapping("/exercise")
  public String redirectToAssignment() {
    return "redirect:/exercise/4/exam-1";
  }

  /**
   * Renders one canned exercise docket composed into the layout shell.
   *
   * @param stageId station identifier
   * @param examKey catalog key within the station
   * @param model the view model
   * @return the layout template name
   * @throws ResponseStatusException with 404 when the stage and exam pair is unknown
   */
  @GetMapping("/exercise/{stageId}/{examKey}")
  public String lookup(@PathVariable int stageId, @PathVariable String examKey, Model model) {
    CannedExercise exercise =
        catalog
            .find(stageId, examKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("title", "TypeShift — " + exercise.label());
    model.addAttribute("contentFragment", "exercise");
    model.addAttribute("shiftState", shiftState(exercise));
    model.addAttribute("docketLabel", exercise.label());
    model.addAttribute("docketFraming", framing(exercise));
    model.addAttribute("dataTarget", exercise.displayText());
    model.addAttribute("dataExpected", exercise.isMath() ? exercise.expectedAnswer() : null);
    model.addAttribute("dataMode", exercise.isMath() ? "numeric" : "prose");
    model.addAttribute("taskClass", taskClass(exercise));
    model.addAttribute("stageId", stageId);
    model.addAttribute("examKey", examKey);
    return "layout";
  }

  /**
   * Saves the measured result of one completed exercise before returning to the dashboard.
   *
   * @param stageId station identifier
   * @param examKey catalog key within the station
   * @param typedText final text entered by the learner
   * @param elapsedSeconds elapsed typing time in seconds
   * @param principal authenticated learner
   * @return redirect to the saved-results dashboard
   */
  @PostMapping("/exercise/{stageId}/{examKey}/submit")
  public String submit(
      @PathVariable int stageId,
      @PathVariable String examKey,
      @RequestParam String typedText,
      @RequestParam int elapsedSeconds,
      Principal principal) {
    submissions.submit(principal.getName(), stageId, examKey, typedText, elapsedSeconds);
    return "redirect:/";
  }

  private static String shiftState(CannedExercise exercise) {
    String kind = exercise.examType() == ExamType.GRADUATION ? " [Graduation]" : " [HU]";
    return "Shift in progress // Stage "
        + exercise.stageId()
        + ": "
        + CannedCatalog.STAGE_NAMES.get(exercise.stageId())
        + kind;
  }

  private static String framing(CannedExercise exercise) {
    return FRAMING.getOrDefault(exercise.stageId() + "/" + exercise.examKey(), exercise.label());
  }

  private static String taskClass(CannedExercise exercise) {
    return exercise.taskType() == null
        ? null
        : exercise.taskType().name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
