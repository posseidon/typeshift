package hu.elte.ntb.typeshift.dashboard.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.dashboard.model.DashboardSnapshot;
import hu.elte.ntb.typeshift.dashboard.model.DashboardSnapshot.RecentScore;
import hu.elte.ntb.typeshift.dashboard.model.DashboardSnapshot.StageProgress;
import hu.elte.ntb.typeshift.dashboard.model.DashboardSnapshot.WeakKey;
import hu.elte.ntb.typeshift.dashboard.model.DashboardSnapshot.WpmPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Loads persisted, owner-scoped values for the dashboard. */
// Spring DI: JdbcTemplate is a thread-safe singleton, not mutable service state.
@SuppressFBWarnings("EI_EXPOSE_REP2")
@Service
public class DashboardService {

  private final JdbcTemplate jdbc;

  /** Creates the read service. */
  public DashboardService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Loads a dashboard snapshot for the authenticated username. */
  public DashboardSnapshot loadFor(String username) {
    long userId =
        Objects.requireNonNull(
            jdbc.queryForObject("SELECT id FROM app_user WHERE username = ?", Long.class, username),
            "Authenticated account no longer exists");
    String displayName =
        jdbc.queryForObject(
            "SELECT COALESCE(display_name, username) FROM app_user WHERE id = ?",
            String.class,
            userId);
    List<ProfileValues> profiles =
        jdbc.query(
            "SELECT current_stage, total_practice_minutes FROM learning_profile WHERE user_id = ?",
            (rs, rowNum) -> new ProfileValues(rs.getInt("current_stage"), rs.getInt("total_practice_minutes")),
            userId);
    boolean hasProfile = !profiles.isEmpty();
    int currentStage = hasProfile ? profiles.get(0).currentStage() : 1;
    int totalPracticeMinutes = hasProfile ? profiles.get(0).totalPracticeMinutes() : 0;
    Set<Integer> unlockedStages =
        hasProfile
            ? new HashSet<>(
                jdbc.query(
                    "SELECT stage FROM learning_profile_unlocked_stages WHERE learning_profile_id = "
                        + "(SELECT id FROM learning_profile WHERE user_id = ?)",
                    (rs, rowNum) -> rs.getInt("stage"),
                    userId))
            : Set.of(1);

    long practiceSessionCount =
        count(
            "SELECT COUNT(*) FROM practice_session WHERE user_id = ?",
            userId);
    BigDecimal latestWpm = latestWpm(userId);
    BigDecimal averageAccuracyPercent = averageAccuracy(userId);
    Map<Long, Long> passedSubExamsByStage = passedSubExamCounts(userId);
    long passedSubExams = passedSubExamsByStage.getOrDefault((long) currentStage, 0L);
    List<StageProgress> stages = stages(userId, currentStage, unlockedStages, passedSubExamsByStage);
    String currentStageName =
        stages.stream()
            .filter(stage -> stage.stageId() == currentStage)
            .map(StageProgress::name)
            .findFirst()
            .orElse("");

    return new DashboardSnapshot(
        displayName,
        currentStage,
        currentStageName,
        totalPracticeMinutes,
        practiceSessionCount,
        latestWpm,
        averageAccuracyPercent,
        passedSubExams,
        countStagesSubExams(stages, currentStage),
        stages,
        wpmHistory(userId),
        weakKeys(userId),
        recentScores(userId));
  }

  private long count(String sql, Object... arguments) {
    Long value = jdbc.queryForObject(sql, Long.class, arguments);
    return value == null ? 0 : value;
  }

  private BigDecimal latestWpm(long userId) {
    List<BigDecimal> values =
        jdbc.query(
            "SELECT wpm FROM practice_session WHERE user_id = ? "
                + "ORDER BY submitted_at DESC, id DESC LIMIT 1",
            (rs, rowNum) -> decimal(rs, "wpm"),
            userId);
    return values.isEmpty() ? null : values.get(0);
  }

  private BigDecimal averageAccuracy(long userId) {
    List<BigDecimal> values =
        jdbc.query(
            "SELECT AVG(accuracy) AS accuracy FROM score_record WHERE user_id = ?",
            (rs, rowNum) -> decimal(rs, "accuracy"),
            userId);
    return values.isEmpty() || values.get(0) == null
        ? null
        : values.get(0).multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP);
  }

  private Map<Long, Long> passedSubExamCounts(long userId) {
    return jdbc.query(
        "SELECT stage_id, COUNT(DISTINCT sub_exam_number) AS passed_count FROM score_record "
            + "WHERE user_id = ? AND attempt_kind = 'SUB_EXAM' AND passed = TRUE GROUP BY stage_id",
        rs -> {
          Map<Long, Long> counts = new java.util.HashMap<>();
          while (rs.next()) {
            counts.put(rs.getLong("stage_id"), rs.getLong("passed_count"));
          }
          return counts;
        },
        userId);
  }

  private List<StageProgress> stages(
      long userId,
      int currentStage,
      Set<Integer> unlockedStages,
      Map<Long, Long> passedSubExamsByStage) {
    Set<Long> graduatedStages =
        new HashSet<>(
            jdbc.query(
                "SELECT DISTINCT stage_id FROM score_record WHERE user_id = ? "
                    + "AND attempt_kind = 'GRADUATION' AND passed = TRUE",
                (rs, rowNum) -> rs.getLong("stage_id"),
                userId));
    return jdbc.query(
        "SELECT id, name, sub_exam_count FROM stage ORDER BY id",
        (rs, rowNum) -> {
          long stageId = rs.getLong("id");
          String status =
              graduatedStages.contains(stageId)
                  ? "GRADUATED"
                  : stageId == currentStage
                      ? "CURRENT"
                      : unlockedStages.contains((int) stageId) ? "UNLOCKED" : "LOCKED";
          return new StageProgress(
              stageId,
              rs.getString("name"),
              status,
              passedSubExamsByStage.getOrDefault(stageId, 0L).intValue(),
              rs.getInt("sub_exam_count"));
        });
  }

  private int countStagesSubExams(List<StageProgress> stages, int stageId) {
    return stages.stream()
        .filter(stage -> stage.stageId() == stageId)
        .mapToInt(StageProgress::subExamCount)
        .findFirst()
        .orElse(0);
  }

  private List<WpmPoint> wpmHistory(long userId) {
    List<WpmPoint> points =
        jdbc.query(
            "SELECT submitted_at, wpm FROM practice_session WHERE user_id = ? "
                + "ORDER BY submitted_at DESC, id DESC LIMIT 7",
            (rs, rowNum) ->
                new WpmPoint(rs.getString("submitted_at"), decimal(rs, "wpm")),
            userId);
    Collections.reverse(points);
    return points;
  }

  private List<WeakKey> weakKeys(long userId) {
    return jdbc.query(
        "SELECT key_char, miss_count FROM weak_key_profile WHERE user_id = ? "
            + "ORDER BY miss_count DESC, key_char ASC LIMIT 5",
        (rs, rowNum) -> new WeakKey(rs.getString("key_char"), rs.getInt("miss_count")),
        userId);
  }

  private List<RecentScore> recentScores(long userId) {
    return jdbc.query(
        "SELECT score_record.submitted_at, stage.name AS stage_name, "
            + "CASE WHEN score_record.attempt_kind = 'GRADUATION' THEN 'Graduation' "
            + "ELSE 'Sub-exam ' || score_record.sub_exam_number END AS exam_label, "
            + "score_record.accuracy, score_record.passed FROM score_record "
            + "JOIN stage ON stage.id = score_record.stage_id "
            + "WHERE score_record.user_id = ? "
            + "ORDER BY score_record.submitted_at DESC, score_record.id DESC LIMIT 5",
        (rs, rowNum) ->
            new RecentScore(
                rs.getString("submitted_at"),
                rs.getString("stage_name"),
                rs.getString("exam_label"),
                decimal(rs, "accuracy").multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
                rs.getBoolean("passed")),
        userId);
  }

  private BigDecimal decimal(ResultSet resultSet, String column) throws SQLException {
    String value = resultSet.getString(column);
    return value == null ? null : new BigDecimal(value);
  }

  private record ProfileValues(int currentStage, int totalPracticeMinutes) {}
}
