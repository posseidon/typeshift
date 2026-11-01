package hu.elte.ntb.typeshift.exercise.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.account.service.UserAccountService;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists completed canned exercise attempts for the authenticated learner. */
@Service
// Spring DI: JdbcTemplate is a thread-safe singleton, not mutable service state.
@SuppressFBWarnings("EI_EXPOSE_REP2")
public class ExerciseSubmissionService {

  private static final BigDecimal PASSING_ACCURACY = new BigDecimal("0.95");

  private final CannedCatalog catalog;
  private final UserAccountService accounts;
  private final JdbcTemplate jdbc;

  public ExerciseSubmissionService(
      CannedCatalog catalog, UserAccountService accounts, JdbcTemplate jdbc) {
    this.catalog = catalog;
    this.accounts = accounts;
    this.jdbc = jdbc;
  }

  /**
   * Calculates authoritative results from the submitted text and writes the completed attempt.
   *
   * @param username authenticated learner
   * @param stageId exercise stage
   * @param examKey catalog exam key
   * @param typedText final entered text
   * @param elapsedSeconds measured elapsed time
   */
  @Transactional
  public void submit(
      String username, int stageId, String examKey, String typedText, int elapsedSeconds) {
    if (elapsedSeconds < 1 || elapsedSeconds > 86_400) {
      throw new IllegalArgumentException("Elapsed time must be between 1 second and 24 hours");
    }
    CannedExercise exercise =
        catalog
            .find(stageId, examKey)
            .orElseThrow(() -> new IllegalArgumentException("Unknown exercise"));
    String expected = exercise.expectedAnswer();
    String submitted = typedText == null ? "" : typedText;
    if (submitted.length() > expected.length()) {
      throw new IllegalArgumentException("Submitted text exceeds the exercise length");
    }

    EditCounts edits = editCounts(expected, submitted);
    int totalCharacters = expected.length();
    int correctCharacters = Math.max(0, totalCharacters - edits.distance());
    BigDecimal accuracy =
        totalCharacters == 0
            ? BigDecimal.ONE
            : BigDecimal.valueOf(correctCharacters)
                .divide(BigDecimal.valueOf(totalCharacters), 4, RoundingMode.HALF_UP);
    BigDecimal minutes =
        BigDecimal.valueOf(elapsedSeconds)
            .divide(BigDecimal.valueOf(60), 8, RoundingMode.HALF_UP);
    BigDecimal cpm =
        minutes.signum() == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(correctCharacters).divide(minutes, 2, RoundingMode.HALF_UP);
    BigDecimal wpm = cpm.divide(BigDecimal.valueOf(5), 2, RoundingMode.HALF_UP);
    long userId = accounts.accountIdFor(username);
    OffsetDateTime submittedAt = OffsetDateTime.now(ZoneOffset.UTC);

    jdbc.update(
        "INSERT INTO practice_session (user_id, stage_id, exercise_kind, wpm, cpm, "
            + "correct_characters, total_characters, duration_seconds, submitted_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        userId,
        stageId,
        exercise.isMath() ? "MATH" : "PROSE",
        wpm,
        cpm,
        correctCharacters,
        totalCharacters,
        elapsedSeconds,
        submittedAt);
    if (exercise.examType() == hu.elte.ntb.typeshift.ai.model.ExamType.SUB_EXAM) {
      jdbc.update(
          "INSERT INTO score_record (user_id, stage_id, attempt_kind, sub_exam_number, "
              + "accuracy, substitutions, insertions, deletions, passed, submitted_at) "
              + "VALUES (?, ?, 'SUB_EXAM', ?, ?, ?, ?, ?, ?, ?)",
          userId,
          stageId,
          subExamNumber(examKey),
          accuracy,
          edits.substitutions(),
          edits.insertions(),
          edits.deletions(),
          accuracy.compareTo(PASSING_ACCURACY) >= 0,
          submittedAt);
    }
  }

  private static int subExamNumber(String examKey) {
    return Integer.parseInt(examKey.substring("exam-".length()));
  }

  private static EditCounts editCounts(String expected, String submitted) {
    int[][] distances = new int[expected.length() + 1][submitted.length() + 1];
    for (int expectedIndex = 0; expectedIndex <= expected.length(); expectedIndex++) {
      distances[expectedIndex][0] = expectedIndex;
    }
    for (int submittedIndex = 0; submittedIndex <= submitted.length(); submittedIndex++) {
      distances[0][submittedIndex] = submittedIndex;
    }
    for (int expectedIndex = 1; expectedIndex <= expected.length(); expectedIndex++) {
      for (int submittedIndex = 1; submittedIndex <= submitted.length(); submittedIndex++) {
        int substitution =
            distances[expectedIndex - 1][submittedIndex - 1]
                + (expected.charAt(expectedIndex - 1) == submitted.charAt(submittedIndex - 1)
                    ? 0
                    : 1);
        int insertion = distances[expectedIndex][submittedIndex - 1] + 1;
        int deletion = distances[expectedIndex - 1][submittedIndex] + 1;
        distances[expectedIndex][submittedIndex] = Math.min(substitution, Math.min(insertion, deletion));
      }
    }
    return countEdits(expected, submitted, distances);
  }

  private static EditCounts countEdits(String expected, String submitted, int[][] distances) {
    int expectedIndex = expected.length();
    int submittedIndex = submitted.length();
    int substitutions = 0;
    int insertions = 0;
    int deletions = 0;
    while (expectedIndex > 0 || submittedIndex > 0) {
      if (expectedIndex > 0
          && submittedIndex > 0
          && distances[expectedIndex][submittedIndex]
              == distances[expectedIndex - 1][submittedIndex - 1]
                  + (expected.charAt(expectedIndex - 1) == submitted.charAt(submittedIndex - 1)
                      ? 0
                      : 1)) {
        if (expected.charAt(expectedIndex - 1) != submitted.charAt(submittedIndex - 1)) {
          substitutions++;
        }
        expectedIndex--;
        submittedIndex--;
      } else if (submittedIndex > 0
          && distances[expectedIndex][submittedIndex]
              == distances[expectedIndex][submittedIndex - 1] + 1) {
        insertions++;
        submittedIndex--;
      } else {
        deletions++;
        expectedIndex--;
      }
    }
    return new EditCounts(substitutions, insertions, deletions);
  }

  private record EditCounts(int substitutions, int insertions, int deletions) {
    int distance() {
      return substitutions + insertions + deletions;
    }
  }
}
