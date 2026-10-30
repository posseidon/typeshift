package hu.elte.ntb.typeshift.ai.model;

import java.util.List;

/**
 * Generation request shared by every AI provider (ADR-004). No Telex or
 * Vietnamese fields (ADR-005).
 *
 * @param stageId stage 1 to 5
 * @param examType SUB_EXAM or GRADUATION
 * @param focusKeys up to five weak keys the exercise should emphasise
 * @param recentContentHashes last five content hashes served, for deduplication
 * @param languageTarget HUNGARIAN, ENGLISH, or MIXED
 * @param taskType Stage 5 task category, or null for stages 1 to 4
 */
public record AiRequest(
    int stageId,
    ExamType examType,
    List<String> focusKeys,
    List<String> recentContentHashes,
    LanguageTarget languageTarget,
    TaskType taskType) {

  /** Copies mutable list components so the envelope stays immutable. */
  public AiRequest {
    focusKeys = List.copyOf(focusKeys);
    recentContentHashes = List.copyOf(recentContentHashes);
  }
}
