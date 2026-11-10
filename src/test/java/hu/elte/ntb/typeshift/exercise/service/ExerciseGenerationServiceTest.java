package hu.elte.ntb.typeshift.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.service.AiProviderException;
import hu.elte.ntb.typeshift.ai.service.GeminiContentService;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class ExerciseGenerationServiceTest {

  @Test
  void validExerciseUsesOneModelCompletion() {
    GeminiContentService ai = mock(GeminiContentService.class);
    when(ai.generate(any(AiRequest.class)))
        .thenReturn(new AiResponse("Árvíztűrő tükörfúrógép."));

    ExerciseGenerationService generation = new ExerciseGenerationService(ai);
    CannedExercise exercise =
        new CannedExercise(
            1, ExamType.SUB_EXAM, "exam-2", "Test", ContentType.PROSE, null, "", "", 45);

    CannedExercise generated = generation.generate(exercise);

    assertThat(generated.displayText()).isEqualTo("Árvíztűrő tükörfúrógép.");
    ArgumentCaptor<AiRequest> request = ArgumentCaptor.forClass(AiRequest.class);
    verify(ai).generate(request.capture());
    assertThat(request.getValue().focusKeys()).isEmpty();
    assertThat(request.getValue().recentContentHashes()).isEmpty();
  }

  @Test
  void invalidExerciseFailsWithoutRegeneration() {
    GeminiContentService ai = mock(GeminiContentService.class);
    when(ai.generate(any(AiRequest.class)))
        .thenReturn(new AiResponse("not math"));

    ExerciseGenerationService generation = new ExerciseGenerationService(ai);
    CannedExercise exercise =
        new CannedExercise(
            1, ExamType.SUB_EXAM, "exam-2", "Test", ContentType.MATH, null, "", "", 45);

    assertThatThrownBy(() -> generation.generate(exercise))
        .isInstanceOf(AiProviderException.class)
        .hasMessage("Gemini returned invalid exercise content");
    verify(ai, times(1)).generate(any(AiRequest.class));
  }

  @Test
  void oversizedProseFailsWithoutRegeneration() {
    GeminiContentService ai = mock(GeminiContentService.class);
    when(ai.generate(any(AiRequest.class))).thenReturn(new AiResponse("x".repeat(4_001)));

    ExerciseGenerationService generation = new ExerciseGenerationService(ai);
    CannedExercise exercise =
        new CannedExercise(
            1, ExamType.SUB_EXAM, "exam-1", "Prose", ContentType.PROSE, null, "", "", 45);

    assertThatThrownBy(() -> generation.generate(exercise))
        .isInstanceOf(AiProviderException.class)
        .hasMessage("Gemini returned invalid exercise content");
    verify(ai, times(1)).generate(any(AiRequest.class));
  }

  @Test
  void stageFiveChoosesATaskWithoutHistory() {
    GeminiContentService ai = mock(GeminiContentService.class);
    when(ai.generate(any(AiRequest.class))).thenReturn(new AiResponse("A task for practice."));
    ExerciseGenerationService generation = new ExerciseGenerationService(ai);
    CannedExercise definition =
        new CannedExercise(
            5, ExamType.SUB_EXAM, "exam-1", "Chat", ContentType.PROSE, null, "", "", 45);

    CannedExercise generated = generation.generate(definition);

    ArgumentCaptor<AiRequest> request = ArgumentCaptor.forClass(AiRequest.class);
    verify(ai).generate(request.capture());
    assertThat(generated.taskType()).isNotNull();
    assertThat(request.getValue().taskType()).isEqualTo(generated.taskType());
    assertThat(request.getValue().focusKeys()).isEmpty();
    assertThat(request.getValue().recentContentHashes()).isEmpty();
    assertThat(generated.expectedAnswer()).isEqualTo("A task for practice.");
  }
}
