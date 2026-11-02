package hu.elte.ntb.typeshift.ai.service;

import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Selects the stage-native generation prompt for each exercise request. */
@Component
public class AiPromptFactory {

  public String create(AiRequest request) {
    Objects.requireNonNull(request, "request");
    String format =
        switch (request.stageId()) {
          case 1 -> "Write natural Hungarian prose for QWERTZ key practice.";
          case 2 -> "Write a realistic technical-support ticket rich in punctuation and symbols.";
          case 3 ->
              "Write one ledger-style arithmetic expression for sub-exam "
                  + request.subExamNumber()
                  + " using only +, -, *, / and decimal commas.";
          case 4 -> "Write a formal Hungarian request letter using accented Hungarian characters.";
          case 5 -> taskFormat(request.taskType());
          default -> throw new IllegalArgumentException("Unknown exercise stage");
        };
    return ("%s%n"
            + "Target language: %s."
            + "Return only one JSON object with displayText, and%n"
            + "estimatedDurationSeconds."
            + " Output raw JSON only: no markdown code fences, no commentary.")
        .formatted(
            format,
            request.languageTarget());
  }

  private static String taskFormat(TaskType taskType) {
    if (taskType == null) {
      throw new IllegalArgumentException("Stage 5 exercises require a task type");
    }
    return switch (taskType) {
      case CHAT_BURST -> "Write an informal chat burst of one to three short sentences.";
      case EMAIL_REPLY -> "Write an email reply with a subject and a two-to-four-sentence body.";
      case DOCUMENT_PARAGRAPH -> "Write one structured business document paragraph.";
      case CODE_COMMENT -> "Write one concise technical code comment.";
    };
  }
}
