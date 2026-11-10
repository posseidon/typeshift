package hu.elte.ntb.typeshift.exercise.model;

import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.TaskType;

/**
 * One canned exercise of the station catalog, shaped like the ADR-004 payload.
 *
 * @param stageId station identifier, 1 through 5
 * @param examType sub-exam or graduation
 * @param examKey catalog key within the station
 * @param label human-readable exercise label
 * @param contentType prose to type or a math result to enter
 * @param taskType Stage 5 task category, null outside Stage 5
 * @param displayText text the user types
 * @param expectedAnswer PROSE = displayText, MATH = comma-decimal result
 * @param estimatedDurationSeconds expected typing duration in seconds
 */
public record CannedExercise(
    int stageId,
    ExamType examType,
    String examKey,
    String label,
    ContentType contentType,
    TaskType taskType,
    String displayText,
    String expectedAnswer,
    int estimatedDurationSeconds) {

  /**
   * Returns the placeholder content hash of this canned payload.
   *
   * @return "canned-" followed by the stage id and the exam key
   */
  public String contentHash() {
    return "canned-" + stageId + "-" + examKey;
  }

  /**
   * Tells whether this exercise expects a numeric answer.
   *
   * @return true when the content type is MATH
   */
  public boolean isMath() {
    return contentType == ContentType.MATH;
  }
}
