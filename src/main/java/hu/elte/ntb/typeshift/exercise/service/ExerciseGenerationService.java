package hu.elte.ntb.typeshift.exercise.service;

import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.LanguageTarget;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import hu.elte.ntb.typeshift.ai.service.AiProviderException;
import hu.elte.ntb.typeshift.ai.service.GeminiContentService;
import hu.elte.ntb.typeshift.ai.service.MathExpressionEvaluator;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import java.util.List;
import java.util.Locale;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;

/** Generates and validates stage-native typing exercises. */
@Service
public class ExerciseGenerationService {

  private static final int MAX_TEXT_LENGTH = 4_000;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final GeminiContentService ai;

  public ExerciseGenerationService(GeminiContentService ai) {
    this.ai = ai;
  }

  public CannedExercise generate(CannedExercise definition) {
    return generate(definition, new Locale("hu"));
  }

  public CannedExercise generate(CannedExercise definition, Locale locale) {
    TaskType taskType = taskType(definition);
    LanguageTarget target = mapLocaleToLanguageTarget(locale);
    AiRequest request =
        new AiRequest(
            definition.stageId(),
            definition.examType(),
            subExamNumber(definition),
            List.of(),
            List.of(),
            target,
            taskType);
    AiResponse response = ai.generate(request);
    String expectedAnswer;
    try {
      expectedAnswer = validate(definition, response);
    } catch (IllegalArgumentException e) {
      throw new AiProviderException("Gemini returned invalid exercise content", e);
    }
    return new CannedExercise(
        definition.stageId(),
        definition.examType(),
        definition.examKey(),
        definition.label(),
        definition.contentType(),
        taskType,
        response.displayText(),
        expectedAnswer,
        definition.estimatedDurationSeconds());
  }

  private static TaskType taskType(CannedExercise definition) {
    if (definition.stageId() != 5) {
      return definition.taskType();
    }
    TaskType[] available = TaskType.values();
    return available[RANDOM.nextInt(available.length)];
  }

  private static int subExamNumber(CannedExercise definition) {
    if (definition.examType() != ExamType.SUB_EXAM) {
      return 0;
    }
    return Integer.parseInt(definition.examKey().substring("exam-".length()));
  }

  private static LanguageTarget mapLocaleToLanguageTarget(Locale locale) {
    String lang = locale.getLanguage();
    return switch (lang) {
      case "en" -> LanguageTarget.ENGLISH;
      case "hu" -> LanguageTarget.HUNGARIAN;
      default -> LanguageTarget.VIETNAMESE;
    };
  }

  private static String validate(CannedExercise definition, AiResponse response) {
    if (response == null
        || response.displayText() == null
        || response.displayText().isBlank()
        || response.displayText().length() > MAX_TEXT_LENGTH) {
      throw new IllegalArgumentException("Generated exercise is missing valid envelope content");
    }
    if (definition.contentType() == ContentType.PROSE) {
      return response.displayText();
    }
    return MathExpressionEvaluator.evaluate(response.displayText());
  }

}
