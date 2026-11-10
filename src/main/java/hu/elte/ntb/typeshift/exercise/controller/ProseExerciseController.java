package hu.elte.ntb.typeshift.exercise.controller;

import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import hu.elte.ntb.typeshift.exercise.service.CannedCatalog;
import hu.elte.ntb.typeshift.exercise.service.ExerciseGenerationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serves the AI-generated exercise dockets of all five stages through the shared
 * layout shell, one parameterized route for every catalog entry.
 */
@Controller
@SuppressFBWarnings(
    value = "SPRING_ENDPOINT",
    justification = "Public MVC routes are intentional application endpoints.")
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
  private final ExerciseGenerationService generation;

  public ProseExerciseController(CannedCatalog catalog, ExerciseGenerationService generation) {
    this.catalog = catalog;
    this.generation = generation;
  }

  /**
   * Renders the exercise shell instantly: readout bar, notices, loading placeholder, notes. The
   * generated docket is fetched separately so LLM latency never blocks the page.
   *
   * @param stageId station identifier
   * @param examKey catalog key within the station
   * @param model the view model
   * @return the layout template name
   * @throws ResponseStatusException with 404 when the stage and exam pair is unknown
   */
  @GetMapping("/exercise/{stageId}/{examKey}")
  public String lookup(@PathVariable int stageId, @PathVariable String examKey, Model model) {
    if (catalog.find(stageId, examKey).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    model.addAttribute("title", "title.exercise");
    model.addAttribute("contentFragment", "exercise");
    model.addAttribute("stageId", stageId);
    model.addAttribute("examKey", examKey);
    return "layout";
  }

  /**
   * Generates and serves only the exercise docket fragment; the shell loads it asynchronously.
   *
   * @param stageId station identifier
   * @param examKey catalog key within the station
   * @param locale the resolved session locale
   * @param model the view model
   * @return the docket fragment of the exercise template
   * @throws ResponseStatusException with 404 when the stage and exam pair is unknown
   */
  @GetMapping("/exercise/{stageId}/{examKey}/docket")
  public String docket(
      @PathVariable int stageId,
      @PathVariable String examKey,
      Locale locale,
      Model model) {
    CannedExercise definition =
        catalog
            .find(stageId, examKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    CannedExercise exercise = generation.generate(definition, locale);
    model.addAttribute("docketLabel", docketLabel(exercise));
    model.addAttribute("docketFraming", framing(exercise));
    model.addAttribute("dataTarget", exercise.displayText());
    model.addAttribute("dataExpected", exercise.isMath() ? exercise.expectedAnswer() : null);
    model.addAttribute("dataMode", exercise.isMath() ? "numeric" : "prose");
    model.addAttribute("taskClass", taskClass(exercise));
    model.addAttribute("stageId", stageId);
    model.addAttribute("examKey", examKey);
    return "exercise :: docket";
  }

  private static String docketLabel(CannedExercise exercise) {
    if (exercise.stageId() == 5 && exercise.taskType() != null) {
      return switch (exercise.taskType()) {
        case CHAT_BURST -> "Chat burst";
        case EMAIL_REPLY -> "Email reply";
        case DOCUMENT_PARAGRAPH -> "Document paragraph";
        case CODE_COMMENT -> "Code comment";
      };
    }
    return exercise.label();
  }

  private static String framing(CannedExercise exercise) {
    if (exercise.stageId() == 5 && exercise.taskType() != null) {
      return docketLabel(exercise) + " transcription";
    }
    return FRAMING.getOrDefault(exercise.stageId() + "/" + exercise.examKey(), exercise.label());
  }

  private static String taskClass(CannedExercise exercise) {
    return exercise.taskType() == null
        ? null
        : exercise.taskType().name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
