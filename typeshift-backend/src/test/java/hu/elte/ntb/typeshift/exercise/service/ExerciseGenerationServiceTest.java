package hu.elte.ntb.typeshift.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.elte.ntb.typeshift.account.service.UserAccountService;
import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.service.AiProviderException;
import hu.elte.ntb.typeshift.ai.service.AzureOpenAIContentService;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ExerciseGenerationServiceTest {

  @Test
  void validExerciseUsesOneAzureCompletion() {
    AzureOpenAIContentService ai = mock(AzureOpenAIContentService.class);
    UserAccountService accounts = mock(UserAccountService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    doReturn(List.of()).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    when(accounts.accountIdFor("owner")).thenReturn(7L);
    when(ai.generate(any(AiRequest.class)))
        .thenReturn(new AiResponse("Árvíztűrő tükörfúrógép."));

    ExerciseGenerationService generation = new ExerciseGenerationService(ai, accounts, jdbc);
    CannedExercise exercise =
        new CannedExercise(
            1, ExamType.SUB_EXAM, "exam-2", "Test", ContentType.PROSE, null, "", "", 45);

    CannedExercise generated = generation.generate("owner", exercise);

    assertThat(generated.displayText()).isEqualTo("Árvíztűrő tükörfúrógép.");
    verify(ai, times(1)).generate(any(AiRequest.class));
  }

  @Test
  void invalidExerciseFailsWithoutRegeneration() {
    AzureOpenAIContentService ai = mock(AzureOpenAIContentService.class);
    UserAccountService accounts = mock(UserAccountService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    doReturn(List.of()).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    when(accounts.accountIdFor("owner")).thenReturn(7L);
    when(ai.generate(any(AiRequest.class)))
        .thenReturn(new AiResponse("not math"));

    ExerciseGenerationService generation = new ExerciseGenerationService(ai, accounts, jdbc);
    CannedExercise exercise =
        new CannedExercise(
            1, ExamType.SUB_EXAM, "exam-2", "Test", ContentType.MATH, null, "", "", 45);

    assertThatThrownBy(() -> generation.generate("owner", exercise))
        .isInstanceOf(AiProviderException.class);
    verify(ai, times(1)).generate(any(AiRequest.class));
  }
}
