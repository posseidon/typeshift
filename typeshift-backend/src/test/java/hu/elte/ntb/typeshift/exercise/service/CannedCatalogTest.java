package hu.elte.ntb.typeshift.exercise.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CannedCatalogTest {

  private static final String ACCENTS = "áéíóöőúüű";

  private final CannedCatalog catalog = new CannedCatalog();

  @Test
  void stageSubExamCountsFollowAdr013() {
    int[] expected = {3, 2, 2, 2, 4};
    for (int stage = 1; stage <= 5; stage++) {
      int subExams = (int) catalog.byStage(stage).stream()
          .filter(exercise -> exercise.examType() == ExamType.SUB_EXAM)
          .count();
      assertEquals(expected[stage - 1], subExams, "stage " + stage + " sub-exam count");
    }
  }

  @Test
  void everyStageHasOneGraduationAndTheCatalogHoldsEighteenItems() {
    int total = 0;
    for (int stage = 1; stage <= 5; stage++) {
      List<CannedExercise> items = catalog.byStage(stage);
      long graduations = items.stream()
          .filter(exercise -> exercise.examType() == ExamType.GRADUATION)
          .count();
      assertEquals(1, graduations, "stage " + stage + " graduation count");
      for (CannedExercise exercise : items) {
        assertTrue(catalog.find(stage, exercise.examKey()).isPresent(),
            "find must resolve stage " + stage + " key " + exercise.examKey());
      }
      assertTrue(catalog.find(stage, "grad").isPresent(), "stage " + stage + " has grad");
      total += items.size();
    }
    assertEquals(18, total, "catalog item total");
  }

  @Test
  void unknownStageOrKeyIsEmpty() {
    assertTrue(catalog.find(9, "grad").isEmpty());
    assertTrue(catalog.find(1, "nope").isEmpty());
    assertTrue(catalog.byStage(9).isEmpty());
  }

  @Test
  void stageExamKeyPairsAreUnique() {
    Set<String> keys = new HashSet<>();
    for (CannedExercise exercise : allExercises()) {
      assertTrue(keys.add(exercise.stageId() + "/" + exercise.examKey()),
          "duplicate exam key " + exercise.stageId() + "/" + exercise.examKey());
    }
    assertEquals(18, keys.size());
  }

  @Test
  void proseExpectedAnswerMatchesDisplayText() {
    for (CannedExercise exercise : allExercises()) {
      if (exercise.contentType() == ContentType.PROSE) {
        assertEquals(exercise.displayText(), exercise.expectedAnswer(),
            exercise.examKey() + " prose answer");
      }
    }
  }

  @Test
  void mathAnswersAreCommaDecimalsAndRecomputeFromDisplayText() {
    for (CannedExercise exercise : allExercises()) {
      if (exercise.contentType() == ContentType.MATH) {
        assertTrue(exercise.expectedAnswer().contains(","),
            exercise.examKey() + " uses a comma decimal");
        assertFalse(exercise.expectedAnswer().contains("."),
            exercise.examKey() + " must not use a dot decimal");
        assertEquals(exercise.expectedAnswer(), evaluate(exercise.displayText()),
            exercise.examKey() + " recomputed answer");
      }
    }
  }

  @Test
  void stageOneDrillsCoverTheQwertzRows() {
    assertCovers(exercise(1, "exam-1"), "asdfjklé");
    assertCovers(exercise(1, "exam-2"), "qwertzőú");
    assertCovers(exercise(1, "exam-3"), "yxcvbnm-");
    assertLength(exercise(1, "exam-1"), 120, 220);
    assertLength(exercise(1, "exam-2"), 120, 220);
    assertLength(exercise(1, "exam-3"), 120, 220);
    assertTrue(exercise(1, "grad").displayText().length() >= 1000,
        "stage 1 graduation is a continuous passage of at least 1000 chars");
  }

  @Test
  void stageTwoDrillsCoverPunctuationAndAltGrSymbols() {
    assertCovers(exercise(2, "exam-1"), ".,;:-");
    assertCovers(exercise(2, "exam-2"), "@&()„”–");
    assertLength(exercise(2, "exam-1"), 150, 260);
    assertLength(exercise(2, "exam-2"), 150, 260);
  }

  @Test
  void stageFourFirstDrillReusesTheExistingEngineText() {
    assertEquals(
        "A hivatalos iratokat kérjük a főbejárat melletti iktatóba eljuttatni még a "
            + "mai nap folyamán. Az ügyintézés határideje tizenöt munkanap, kivéve ha "
            + "sürgősségi kérelmet nyújtott be az illetékes osztályvezetőnél...",
        exercise(4, "exam-1").displayText());
  }

  @Test
  void stageFourDrillsCoverTheHungarianAccentSet() {
    for (String key : List.of("exam-1", "exam-2", "grad")) {
      CannedExercise exercise = exercise(4, key);
      long distinct = ACCENTS.chars()
          .filter(accent -> exercise.displayText().indexOf(accent) >= 0)
          .count();
      assertTrue(distinct >= 6, "stage 4 " + key + " covers at least six accents, got " + distinct);
    }
    assertLength(exercise(4, "exam-1"), 200, 340);
    assertLength(exercise(4, "exam-2"), 200, 340);
    assertLength(exercise(4, "grad"), 300, 450);
  }

  @Test
  void stageFiveDrillsMatchTheirTaskTypes() {
    assertEquals(TaskType.CHAT_BURST, exercise(5, "exam-1").taskType());
    assertEquals(TaskType.EMAIL_REPLY, exercise(5, "exam-2").taskType());
    assertEquals(TaskType.DOCUMENT_PARAGRAPH, exercise(5, "exam-3").taskType());
    assertEquals(TaskType.CODE_COMMENT, exercise(5, "exam-4").taskType());
    for (int stage = 1; stage <= 4; stage++) {
      for (CannedExercise exercise : catalog.byStage(stage)) {
        assertNull(exercise.taskType(), "stage " + stage + " has no task type");
      }
    }
  }

  @Test
  void stageFiveDrillsUseTheExpectedShape() {
    for (String key : List.of("exam-1", "exam-2", "exam-3", "exam-4")) {
      assertLength(exercise(5, key), 200, 320);
    }
    assertLength(exercise(5, "grad"), 300, 420);
    String codeComment = exercise(5, "exam-4").displayText();
    assertTrue(codeComment.startsWith("//"), "code comment starts with //");
    assertFalse(codeComment.contains("`"), "code comment contains no backticks");
  }

  @Test
  void estimatedDurationIsFortyFiveSecondsPerSubExamAndThreeMinutesPerGraduation() {
    for (CannedExercise exercise : allExercises()) {
      int expected = exercise.examType() == ExamType.GRADUATION ? 180 : 45;
      assertEquals(expected, exercise.estimatedDurationSeconds(), exercise.examKey());
    }
  }

  @Test
  void contentHashAndMathFlagFollowTheEnvelope() {
    CannedExercise prose = exercise(4, "exam-1");
    assertEquals("canned-4-exam-1", prose.contentHash());
    assertFalse(prose.isMath());
    CannedExercise math = exercise(3, "exam-1");
    assertEquals("canned-3-exam-1", math.contentHash());
    assertTrue(math.isMath());
  }

  @Test
  void stageNamesFollowAdr013() {
    assertEquals("QWERTZ Core", CannedCatalog.STAGE_NAMES.get(1));
    assertEquals("Symbol Sorcerer", CannedCatalog.STAGE_NAMES.get(2));
    assertEquals("Numpad & Math", CannedCatalog.STAGE_NAMES.get(3));
    assertEquals("Hungarian Accents", CannedCatalog.STAGE_NAMES.get(4));
    assertEquals("Real-World Tasks", CannedCatalog.STAGE_NAMES.get(5));
  }

  private List<CannedExercise> allExercises() {
    List<CannedExercise> all = new ArrayList<>();
    for (int stage = 1; stage <= 5; stage++) {
      all.addAll(catalog.byStage(stage));
    }
    return all;
  }

  private CannedExercise exercise(int stageId, String examKey) {
    return catalog.find(stageId, examKey)
        .orElseThrow(() -> new AssertionError("missing " + stageId + "/" + examKey));
  }

  private static void assertCovers(CannedExercise exercise, String characters) {
    for (char character : characters.toCharArray()) {
      assertTrue(exercise.displayText().indexOf(character) >= 0,
          exercise.examKey() + " contains " + character);
    }
  }

  private static void assertLength(CannedExercise exercise, int min, int max) {
    int length = exercise.displayText().length();
    assertTrue(length >= min && length <= max,
        exercise.examKey() + " length " + length + " outside " + min + ".." + max);
  }

  private static String evaluate(String displayText) {
    String expression = displayText
        .replace(" = ?", "")
        .replace("×", "*")
        .replace("÷", "/")
        .replace("−", "-")
        .replace("–", "-")
        .trim();
    String[] tokens = expression.split("\\s+");
    List<BigDecimal> values = new ArrayList<>();
    List<String> operators = new ArrayList<>();
    int scale = 0;
    for (int i = 0; i < tokens.length; i++) {
      if (i % 2 == 0) {
        BigDecimal value = new BigDecimal(tokens[i].replace(',', '.'));
        scale = Math.max(scale, value.scale());
        values.add(value);
      } else {
        operators.add(tokens[i]);
      }
    }
    for (int i = 0; i < operators.size(); ) {
      String operator = operators.get(i);
      if ("*".equals(operator) || "/".equals(operator)) {
        BigDecimal left = values.get(i);
        BigDecimal right = values.get(i + 1);
        values.set(i, "*".equals(operator)
            ? left.multiply(right)
            : left.divide(right, scale, RoundingMode.HALF_UP));
        values.remove(i + 1);
        operators.remove(i);
      } else {
        i++;
      }
    }
    BigDecimal result = values.get(0);
    for (int i = 0; i < operators.size(); i++) {
      result = "+".equals(operators.get(i))
          ? result.add(values.get(i + 1))
          : result.subtract(values.get(i + 1));
    }
    return result.setScale(scale, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
  }
}
