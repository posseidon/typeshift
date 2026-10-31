package hu.elte.ntb.typeshift.dashboard.model;

import java.math.BigDecimal;
import java.util.List;

/** Read-only dashboard values for one authenticated account. */
public record DashboardSnapshot(
    String displayName,
    int currentStage,
    String currentStageName,
    int totalPracticeMinutes,
    long practiceSessionCount,
    BigDecimal latestWpm,
    BigDecimal averageAccuracyPercent,
    long passedSubExams,
    int currentStageSubExamCount,
    List<StageProgress> stages,
    List<WpmPoint> wpmHistory,
    List<WeakKey> weakKeys,
    List<RecentScore> recentScores) {

  public DashboardSnapshot {
    stages = List.copyOf(stages);
    wpmHistory = List.copyOf(wpmHistory);
    weakKeys = List.copyOf(weakKeys);
    recentScores = List.copyOf(recentScores);
  }

  /** Stage status and attempt progress. */
  public record StageProgress(
      long stageId, String name, String status, int passedSubExams, int subExamCount) {}

  /** One persisted practice-session speed reading. */
  public record WpmPoint(String submittedAt, BigDecimal wpm) {}

  /** One persisted cumulative miss count. */
  public record WeakKey(String keyChar, int missCount) {}

  /** One persisted exam result. */
  public record RecentScore(
      String submittedAt,
      String stageName,
      String examLabel,
      BigDecimal accuracyPercent,
      boolean passed) {}
}
