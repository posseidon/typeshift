package hu.elte.ntb.typeshift.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.LanguageTarget;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AiPromptFactoryTest {

  @Test
  void selectsNativeFormatsForStagesOneThroughFour() {
    assertThat(prompt(1, ExamType.SUB_EXAM, 1, null)).contains("Home row grounding");
    assertThat(prompt(2, ExamType.SUB_EXAM, 1, null))
        .contains("Standard punctuation and hyphenation");
    assertThat(prompt(3, ExamType.SUB_EXAM, 2, null))
        .contains("Ledger arithmetic & balancing")
        .contains("sub-exam 2");
    assertThat(prompt(4, ExamType.GRADUATION, 0, null))
        .contains("Formal Hungarian request letter");
    assertThat(prompt(1, ExamType.GRADUATION, 0, null))
        .contains("3-minute continuous QWERTZ passage");
    assertThat(prompt(2, ExamType.GRADUATION, 0, null))
        .contains("Formatted technical support ticket");
    assertThat(prompt(3, ExamType.GRADUATION, 0, null))
        .contains("Cashier Rush math evaluation")
        .doesNotContain("sub-exam 0");
  }

  @Test
  void everyCatalogExerciseHasMarkdownPromptResource() {
    int[] subExamCounts = {3, 2, 2, 2, 4};
    for (int stage = 1; stage <= subExamCounts.length; stage++) {
      for (int subExam = 1; subExam <= subExamCounts[stage - 1]; subExam++) {
        assertThat(exercisePrompt(stage, "exam-" + subExam).exists())
            .as("stage %s exam-%s prompt resource", stage, subExam)
            .isTrue();
      }
      assertThat(exercisePrompt(stage, "grad").exists())
          .as("stage %s graduation prompt resource", stage)
          .isTrue();
    }
  }

  @Test
  void rejectsExerciseIdentityWithoutPromptResource() {
    assertThatThrownBy(() -> AiPromptFactory.create(request(1, ExamType.SUB_EXAM, 4, null)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void stationAndTaskPromptsFollowSpecifications() {
    assertThat(topicPrompt(1, ExamType.SUB_EXAM, 1, null, "Home row grounding"))
        .contains("a s d f j k l é");
    assertThat(topicPrompt(1, ExamType.SUB_EXAM, 2, null, "Top row & QWERTZ keys"))
        .contains("q w e r t z u i o p ő ú");
    assertThat(topicPrompt(1, ExamType.SUB_EXAM, 3, null, "Bottom row baseline"))
        .contains("y x c v b n m")
        .contains("a comma, a hyphen, and a period naturally");
    assertThat(topicPrompt(1, ExamType.GRADUATION, 0, null, "3-minute continuous QWERTZ passage"))
        .contains("1,000–4,000 characters");

    assertThat(topicPrompt(2, ExamType.SUB_EXAM, 1, null, "Standard punctuation and hyphenation"))
        .contains("periods, commas, semicolons, colons, and hyphens");
    assertThat(topicPrompt(2, ExamType.SUB_EXAM, 2, null, "AltGr symbols, brackets & quotes"))
        .contains("@", "&", "„", "”", "–");
    assertThat(topicPrompt(2, ExamType.GRADUATION, 0, null, "Formatted technical support ticket"))
        .contains("labeled fields");

    assertThat(topicPrompt(3, ExamType.SUB_EXAM, 1, null, "Numeric home row & keypad orientation"))
        .contains("In `displayText`, include only the arithmetic expression")
        .contains("sub-exam 1")
        .contains("finish exactly with ` = ?`")
        .contains("division must terminate exactly");
    assertThat(topicPrompt(3, ExamType.SUB_EXAM, 2, null, "Ledger arithmetic & balancing"))
        .contains("In `displayText`, include only the arithmetic expression")
        .contains("sub-exam 2")
        .contains("more challenging");
    assertThat(topicPrompt(3, ExamType.GRADUATION, 0, null, "Cashier Rush math evaluation"))
        .contains("In `displayText`, include only the arithmetic expression")
        .contains(" = ?")
        .contains("non-terminating divisions")
        .doesNotContain("sub-exam 0");

    assertThat(topicPrompt(4, ExamType.SUB_EXAM, 1, null, "Accent grounding"))
        .contains("á é í ó ö ő ú ü ű")
        .contains("at least six distinct letters");
    assertThat(topicPrompt(4, ExamType.SUB_EXAM, 2, null, "Formal wording drill"))
        .contains("á é í ó ö ő ú ü ű")
        .contains("at least six distinct letters");
    assertThat(topicPrompt(4, ExamType.GRADUATION, 0, null, "Formal Hungarian request letter"))
        .contains("á é í ó ö ő ú ü ű")
        .contains("at least six distinct letters");

    assertThat(topicPrompt(5, ExamType.SUB_EXAM, 1, TaskType.CHAT_BURST, "Informal bureau chat dispatch"))
        .contains("200–320 characters");
    assertThat(topicPrompt(
            5,
            ExamType.SUB_EXAM,
            2,
            TaskType.EMAIL_REPLY,
            "Inter-departmental memorandum / email"))
        .contains("200–320 characters");
    assertThat(topicPrompt(
            5,
            ExamType.SUB_EXAM,
            3,
            TaskType.DOCUMENT_PARAGRAPH,
            "Official petition & business dossier"))
        .contains("200–320 characters");
    assertThat(topicPrompt(
            5,
            ExamType.SUB_EXAM,
            4,
            TaskType.CODE_COMMENT,
            "System technical code annotation"))
        .contains("200–320 characters");
    assertThat(topicPrompt(
            5,
            ExamType.GRADUATION,
            0,
            TaskType.EMAIL_REPLY,
            "Three random tasks in sequence"))
        .contains("one B1-level Hungarian task")
        .contains("300–420 characters");

    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.CHAT_BURST))
        .contains("1–3 short sentences total")
        .contains("plain speaker labels")
        .contains("Do not prefix lines with hyphens or other Markdown bullets");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.EMAIL_REPLY))
        .contains("subject line")
        .contains("body of 2–4 clear sentences");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.DOCUMENT_PARAGRAPH))
        .contains("business document paragraph");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.CODE_COMMENT))
        .contains("//")
        .contains("no executable code")
        .contains("no backticks");
  }

  @Test
  void addsOnlyRequestedFocusKeysToPrompt() {
    AiRequest focusedRequest =
        new AiRequest(
            4,
            ExamType.SUB_EXAM,
            1,
            List.of("ő", "@"),
            List.of(),
            LanguageTarget.HUNGARIAN,
            null);

    String focusedPrompt = AiPromptFactory.create(focusedRequest);
    assertThat(focusedPrompt)
        .contains("Focus keys: ő, @")
        .contains("Include every listed character");
    assertThat(prompt(4, ExamType.SUB_EXAM, 1, null)).doesNotContain("Focus keys:");
  }

  @Test
  void supportsEveryStageFiveTaskType() {
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.CHAT_BURST)).contains("chat burst");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.EMAIL_REPLY)).contains("email reply");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.DOCUMENT_PARAGRAPH))
        .contains("business document paragraph");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.CODE_COMMENT))
        .contains("technical code annotation");
    assertThatThrownBy(() -> AiPromptFactory.create(request(5, ExamType.SUB_EXAM, 1, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void localizesExerciseAndTaskPrompts() {
    // English prompts
    String englishHomeRow = promptForLanguage(1, ExamType.SUB_EXAM, 1, null, LanguageTarget.ENGLISH);
    assertThat(englishHomeRow)
        .contains("English")
        .contains("a s d f j k l ;")
        .doesNotContain("QWERTZ")
        .doesNotContain("`y`/`z` swap");
    
    String englishTopRow = promptForLanguage(1, ExamType.SUB_EXAM, 2, null, LanguageTarget.ENGLISH);
    assertThat(englishTopRow)
        .contains("q w e r t y u i o p [ ]")
        .doesNotContain("`y`/`z` swap");
    
    String englishAccent = promptForLanguage(4, ExamType.SUB_EXAM, 1, null, LanguageTarget.ENGLISH);
    assertThat(englishAccent)
        .contains("English")
        .doesNotContain("á é í ó ö ő ú ü ű");
    
    String englishEmail = promptForLanguage(5, ExamType.SUB_EXAM, 1, TaskType.EMAIL_REPLY, LanguageTarget.ENGLISH);
    assertThat(englishEmail)
        .contains("Subject:")
        .doesNotContain("Tárgy:");

    // Hungarian prompts
    String hungarianHomeRow = promptForLanguage(1, ExamType.SUB_EXAM, 1, null, LanguageTarget.HUNGARIAN);
    assertThat(hungarianHomeRow)
        .contains("Hungarian")
        .contains("a s d f j k l é");
    
    String hungarianTopRow = promptForLanguage(1, ExamType.SUB_EXAM, 2, null, LanguageTarget.HUNGARIAN);
    assertThat(hungarianTopRow)
        .contains("q w e r t z u i o p ő ú")
        .contains("`y`/`z` swap");
    
    String hungarianAccent = promptForLanguage(4, ExamType.SUB_EXAM, 1, null, LanguageTarget.HUNGARIAN);
    assertThat(hungarianAccent)
        .contains("Hungarian")
        .contains("á é í ó ö ő ú ü ű");
    
    String hungarianEmail = promptForLanguage(5, ExamType.SUB_EXAM, 1, TaskType.EMAIL_REPLY, LanguageTarget.HUNGARIAN);
    assertThat(hungarianEmail)
        .contains("Tárgy:")
        .doesNotContain("Subject:");

    // Vietnamese prompts
    String vietnameseHomeRow = promptForLanguage(1, ExamType.SUB_EXAM, 1, null, LanguageTarget.VIETNAMESE);
    assertThat(vietnameseHomeRow)
        .contains("Vietnamese")
        .contains("a s d f j k l ;")
        .doesNotContain("QWERTZ")
        .doesNotContain("`y`/`z` swap");
    
    String vietnameseAccent = promptForLanguage(4, ExamType.SUB_EXAM, 1, null, LanguageTarget.VIETNAMESE);
    assertThat(vietnameseAccent)
        .contains("Vietnamese")
        .contains("ă â đ ê ô ơ ư");
    
    String vietnameseEmail = promptForLanguage(5, ExamType.SUB_EXAM, 1, TaskType.EMAIL_REPLY, LanguageTarget.VIETNAMESE);
    assertThat(vietnameseEmail)
        .contains("Chủ đề:")
        .doesNotContain("Subject:")
        .doesNotContain("Tárgy:");
  }

  @Test
  void allPromptsHaveNoUnresolvedPlaceholders() {
    LanguageTarget[] targets = {LanguageTarget.ENGLISH, LanguageTarget.HUNGARIAN, LanguageTarget.VIETNAMESE};
    int[] subExamCounts = {3, 2, 2, 2, 4};
    
    for (LanguageTarget target : targets) {
      for (int stage = 1; stage <= subExamCounts.length; stage++) {
        for (int subExam = 1; subExam <= subExamCounts[stage - 1]; subExam++) {
          TaskType taskType = (stage == 5) ? TaskType.CHAT_BURST : null;
          String prompt = promptForLanguage(stage, ExamType.SUB_EXAM, subExam, taskType, target);
          assertNoPlaceholders(prompt, "stage %s exam-%s %s prompt".formatted(stage, subExam, target));
        }
        TaskType gradTaskType = (stage == 5) ? TaskType.EMAIL_REPLY : null;
        String gradPrompt = promptForLanguage(stage, ExamType.GRADUATION, 0, gradTaskType, target);
        assertNoPlaceholders(gradPrompt, "stage %s graduation %s prompt".formatted(stage, target));
      }
      
      // Test all stage 5 task prompts
      assertNoPlaceholders(
          promptForLanguage(5, ExamType.SUB_EXAM, 1, TaskType.CHAT_BURST, target),
          "stage 5 CHAT_BURST %s prompt".formatted(target));
      assertNoPlaceholders(
          promptForLanguage(5, ExamType.SUB_EXAM, 1, TaskType.EMAIL_REPLY, target),
          "stage 5 EMAIL_REPLY %s prompt".formatted(target));
      assertNoPlaceholders(
          promptForLanguage(5, ExamType.SUB_EXAM, 1, TaskType.DOCUMENT_PARAGRAPH, target),
          "stage 5 DOCUMENT_PARAGRAPH %s prompt".formatted(target));
      assertNoPlaceholders(
          promptForLanguage(5, ExamType.SUB_EXAM, 1, TaskType.CODE_COMMENT, target),
          "stage 5 CODE_COMMENT %s prompt".formatted(target));
    }
  }

  @Test
  void mathPromptsRetainDecimalCommasForAllLocales() {
    LanguageTarget[] targets = {LanguageTarget.ENGLISH, LanguageTarget.HUNGARIAN, LanguageTarget.VIETNAMESE};
    
    for (LanguageTarget target : targets) {
      String mathPrompt1 = promptForLanguage(3, ExamType.SUB_EXAM, 1, null, target);
      assertThat(mathPrompt1)
          .as("stage 3 exam-1 %s math prompt", target)
          .contains("digits, commas, spaces")
          .contains(" = ?");
      
      String mathPrompt2 = promptForLanguage(3, ExamType.SUB_EXAM, 2, null, target);
      assertThat(mathPrompt2)
          .as("stage 3 exam-2 %s math prompt", target)
          .contains(" = ?");
      
      String mathGrad = promptForLanguage(3, ExamType.GRADUATION, 0, null, target);
      assertThat(mathGrad)
          .as("stage 3 graduation %s math prompt", target)
          .contains(" = ?");
    }
  }

  private String promptForLanguage(
      int stage, ExamType examType, int subExamNumber, TaskType taskType, LanguageTarget target) {
    AiRequest req = new AiRequest(
        stage,
        examType,
        subExamNumber,
        List.of(),
        List.of(),
        target,
        taskType);
    return AiPromptFactory.create(req);
  }

  private void assertNoPlaceholders(String prompt, String description) {
    assertThat(prompt)
        .as(description)
        .doesNotContain("{language}")
        .doesNotContain("{keyboardLayout}")
        .doesNotContain("{layoutRule}")
        .doesNotContain("{homeRowKeys}")
        .doesNotContain("{topRowKeys}")
        .doesNotContain("{bottomRowKeys}")
        .doesNotContain("{diacriticRule}")
        .doesNotContain("{emailSubjectLabel}");
  }

  private String prompt(int stage, ExamType examType, int subExamNumber, TaskType taskType) {
    return AiPromptFactory.create(request(stage, examType, subExamNumber, taskType));
  }

  private String topicPrompt(
      int stage, ExamType examType, int subExamNumber, TaskType taskType, String topic) {
    String prompt = prompt(stage, examType, subExamNumber, taskType);
    assertThat(prompt).contains(topic, "B1-level Hungarian");
    return prompt;
  }

  private static ClassPathResource exercisePrompt(int stage, String examKey) {
    return new ClassPathResource("prompts/exercises/stage-" + stage + "-" + examKey + ".md");
  }

  private static AiRequest request(
      int stage, ExamType examType, int subExamNumber, TaskType taskType) {
    return new AiRequest(
        stage,
        examType,
        subExamNumber,
        List.of(),
        List.of(),
        LanguageTarget.HUNGARIAN,
        taskType);
  }
}
