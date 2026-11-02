package hu.elte.ntb.typeshift.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.LanguageTarget;
import hu.elte.ntb.typeshift.ai.model.TaskType;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiPromptFactoryTest {

  private final AiPromptFactory prompts = new AiPromptFactory();

  @Test
  void selectsNativeFormatsForStagesOneThroughFour() {
    assertThat(prompt(1, ExamType.SUB_EXAM, 1, null)).contains("Hungarian prose");
    assertThat(prompt(2, ExamType.SUB_EXAM, 1, null)).contains("technical-support ticket");
    assertThat(prompt(3, ExamType.SUB_EXAM, 2, null))
        .contains("ledger-style arithmetic")
        .contains("sub-exam 2");
    assertThat(prompt(4, ExamType.GRADUATION, 0, null)).contains("request letter");
  }

  @Test
  void supportsEveryStageFiveTaskType() {
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.CHAT_BURST)).contains("chat burst");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.EMAIL_REPLY)).contains("email reply");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.DOCUMENT_PARAGRAPH))
        .contains("business document paragraph");
    assertThat(prompt(5, ExamType.SUB_EXAM, 1, TaskType.CODE_COMMENT))
        .contains("technical code comment");
    assertThatThrownBy(() -> prompts.create(request(5, ExamType.SUB_EXAM, 1, null)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private String prompt(int stage, ExamType examType, int subExamNumber, TaskType taskType) {
    return prompts.create(request(stage, examType, subExamNumber, taskType));
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
