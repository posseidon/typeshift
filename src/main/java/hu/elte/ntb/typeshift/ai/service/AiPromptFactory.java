package hu.elte.ntb.typeshift.ai.service;

import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Selects the exercise-specific generation prompt for each request. */
public final class AiPromptFactory {

  private static final String PROMPT_DIRECTORY = "prompts/exercises/";
  private static final int SUB_EXAM_FORMAT_STAGE = 3;
  private static final int TASK_FORMAT_STAGE = 5;

  private AiPromptFactory() {}

  public static String create(AiRequest request) {
    Objects.requireNonNull(request, "request");
    String exerciseFormat = readPrompt(exercisePrompt(request));
    String formattedExercise = applyStageFormat(exerciseFormat, request);
    return createGenerationPrompt(formattedExercise, request);
  }

  private static String applyStageFormat(String exerciseFormat, AiRequest request) {
    return switch (request.stageId()) {
      case SUB_EXAM_FORMAT_STAGE ->
          exerciseFormat.replace("{subExamNumber}", Integer.toString(request.subExamNumber()));
      case TASK_FORMAT_STAGE ->
          exerciseFormat.replace("{taskFormat}", readPrompt(taskPrompt(request.taskType())));
      default -> exerciseFormat;
    };
  }

  private static String createGenerationPrompt(String exerciseFormat, AiRequest request) {
    return ("%s%n"
            + "Target language: %s."
            + "%s"
            + "Return only one JSON object with displayText, and%n"
            + "estimatedDurationSeconds."
            + " Output raw JSON only: no markdown code fences, no commentary.")
        .formatted(exerciseFormat, request.languageTarget(), focusInstruction(request));
  }

  private static String focusInstruction(AiRequest request) {
    if (request.focusKeys().isEmpty()) {
      return "";
    }
    return "%nFocus keys: %s. Include every listed character naturally in displayText.%n"
        .formatted(String.join(", ", request.focusKeys()));
  }

  private static String exercisePrompt(AiRequest request) {
    if (request.stageId() < 1 || request.stageId() > 5) {
      throw new IllegalArgumentException("Unknown exercise stage");
    }
    String examKey = switch (request.examType()) {
      case SUB_EXAM -> "exam-" + request.subExamNumber();
      case GRADUATION -> "grad";
    };
    return "stage-" + request.stageId() + "-" + examKey + ".md";
  }

  private static String taskPrompt(TaskType taskType) {
    if (taskType == null) {
      throw new IllegalArgumentException("Stage 5 exercises require a task type");
    }
    return switch (taskType) {
      case CHAT_BURST -> "task-chat-burst.md";
      case EMAIL_REPLY -> "task-email-reply.md";
      case DOCUMENT_PARAGRAPH -> "task-document-paragraph.md";
      case CODE_COMMENT -> "task-code-comment.md";
    };
  }

  private static String readPrompt(String resourceName) {
    String resourcePath = PROMPT_DIRECTORY + resourceName;
    try (InputStream input =
        AiPromptFactory.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (input == null) {
        throw new IllegalStateException("Missing AI prompt resource: " + resourcePath);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8).stripTrailing();
    } catch (IOException e) {
      throw new IllegalStateException("Could not read AI prompt resource: " + resourcePath, e);
    }
  }
}
