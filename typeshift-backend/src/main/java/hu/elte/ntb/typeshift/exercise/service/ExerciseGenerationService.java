package hu.elte.ntb.typeshift.exercise.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.account.service.UserAccountService;
import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.LanguageTarget;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import hu.elte.ntb.typeshift.ai.service.AiProviderException;
import hu.elte.ntb.typeshift.ai.service.AzureOpenAIContentService;
import hu.elte.ntb.typeshift.ai.service.MathExpressionEvaluator;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Generates and validates stage-native typing exercises. */
@Service
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class ExerciseGenerationService {

  private static final int MAX_TEXT_LENGTH = 4_000;

  private final AzureOpenAIContentService ai;
  private final UserAccountService accounts;
  private final JdbcTemplate jdbc;

  public ExerciseGenerationService(
      AzureOpenAIContentService ai, UserAccountService accounts, JdbcTemplate jdbc) {
    this.ai = ai;
    this.accounts = accounts;
    this.jdbc = jdbc;
  }

  public CannedExercise generate(String username, CannedExercise definition) {
    long userId = accounts.accountIdFor(username);
    List<String> recentHashes = recentHashes(userId);
    List<String> focusKeys = focusKeys(userId);
    TaskType previousTask = previousTask(userId);
    TaskType taskType = taskType(definition, previousTask);
    AiRequest request =
        new AiRequest(
            definition.stageId(),
            definition.examType(),
            subExamNumber(definition),
            focusKeys,
            recentHashes,
            LanguageTarget.HUNGARIAN,
            taskType);
    AiResponse response = ai.generate(request);
    String expectedAnswer;
    try {
      expectedAnswer = validate(definition, response, focusKeys);
    } catch (IllegalArgumentException e) {
      throw new AiProviderException("Azure OpenAI returned invalid exercise content", e);
    }
    String contentHash = sha256(response.displayText());
    if (recentHashes.contains(contentHash)) {
      throw new AiProviderException(
          "Azure OpenAI returned content that duplicates a recent exercise",
          new IllegalArgumentException("Generated content duplicates a recent exercise"));
    }
    jdbc.update(
        "INSERT INTO generated_content_hash (user_id, content_hash, task_type) VALUES (?, ?, ?)",
        userId,
        contentHash,
        taskType == null ? null : taskType.name());
    jdbc.update(
        "DELETE FROM generated_content_hash WHERE user_id = ? AND id NOT IN "
            + "(SELECT id FROM generated_content_hash WHERE user_id = ? "
            + "ORDER BY created_at DESC, id DESC LIMIT 5)",
        userId,
        userId);
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

  private List<String> recentHashes(long userId) {
    return jdbc.query(
        "SELECT content_hash FROM generated_content_hash WHERE user_id = ? "
            + "ORDER BY created_at DESC, id DESC LIMIT 5",
        (rs, rowNum) -> rs.getString("content_hash"),
        userId);
  }

  private List<String> focusKeys(long userId) {
    return jdbc.query(
        "SELECT key_char FROM weak_key_profile WHERE user_id = ? AND miss_count >= 2 "
            + "ORDER BY miss_count DESC, key_char ASC LIMIT 5",
        (rs, rowNum) -> rs.getString("key_char"),
        userId);
  }

  private TaskType previousTask(long userId) {
    List<String> values =
        jdbc.query(
            "SELECT task_type FROM generated_content_hash WHERE user_id = ? "
                + "AND task_type IS NOT NULL ORDER BY created_at DESC, id DESC LIMIT 1",
            (rs, rowNum) -> rs.getString("task_type"),
            userId);
    return values.isEmpty() ? null : TaskType.valueOf(values.get(0));
  }

  private static TaskType taskType(CannedExercise definition, TaskType previousTask) {
    if (definition.stageId() != 5) {
      return definition.taskType();
    }
    List<TaskType> available =
        java.util.Arrays.stream(TaskType.values())
            .filter(task -> task != previousTask)
            .toList();
    return available.get(ThreadLocalRandom.current().nextInt(available.size()));
  }

  private static int subExamNumber(CannedExercise definition) {
    if (definition.examType() != ExamType.SUB_EXAM) {
      return 0;
    }
    return Integer.parseInt(definition.examKey().substring("exam-".length()));
  }

  private static String validate(
      CannedExercise definition, AiResponse response, List<String> focusKeys) {
    if (response == null
        || response.displayText() == null
        || response.displayText().isBlank()
        || response.displayText().length() > MAX_TEXT_LENGTH) {
      throw new IllegalArgumentException("Generated exercise is missing valid envelope content");
    }
    for (String focusKey : focusKeys) {
      if (!response.displayText().toLowerCase(Locale.ROOT)
          .contains(focusKey.toLowerCase(Locale.ROOT))) {
        throw new IllegalArgumentException("Generated exercise omitted a requested focus key");
      }
    }
    if (definition.contentType() == ContentType.PROSE) {
      return response.displayText();
    }
    return MathExpressionEvaluator.evaluate(response.displayText());
  }

  private static String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }
}
