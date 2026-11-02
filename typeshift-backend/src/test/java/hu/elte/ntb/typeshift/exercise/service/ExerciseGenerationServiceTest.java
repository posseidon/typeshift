package hu.elte.ntb.typeshift.exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import hu.elte.ntb.typeshift.ai.service.GeminiContentService;
import hu.elte.ntb.typeshift.exercise.model.CannedExercise;
import org.mockito.ArgumentCaptor;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ExerciseGenerationServiceTest {

  @Test
  void validExerciseUsesOneModelCompletion() {
    GeminiContentService ai = mock(GeminiContentService.class);
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
    GeminiContentService ai = mock(GeminiContentService.class);
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
        .isInstanceOf(AiProviderException.class)
        .hasMessage("Gemini returned invalid exercise content");
    verify(ai, times(1)).generate(any(AiRequest.class));
  }

  @Test
  void mathRequestsOnlyParserCompatibleFocusKeys() {
    GeminiContentService ai = mock(GeminiContentService.class);
    UserAccountService accounts = mock(UserAccountService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    doReturn(List.of()).when(jdbc).query(anyString(), any(RowMapper.class), any(Object[].class));
    doReturn(List.of("a", "1", "+"))
        .when(jdbc)
        .query(
            eq(
                "SELECT key_char FROM weak_key_profile WHERE user_id = ? "
                    + "AND miss_count >= 2 ORDER BY miss_count DESC, key_char ASC LIMIT 5"),
            any(RowMapper.class),
            any(Object[].class));
    when(accounts.accountIdFor("owner")).thenReturn(7L);
    when(ai.generate(any(AiRequest.class)))
        .thenReturn(new AiResponse("1 + 2 = ?"), new AiResponse("a 1 +"));

    ExerciseGenerationService generation = new ExerciseGenerationService(ai, accounts, jdbc);
    CannedExercise mathExercise =
        new CannedExercise(
            3, ExamType.SUB_EXAM, "exam-1", "Math", ContentType.MATH, null, "", "", 45);
    CannedExercise proseExercise =
        new CannedExercise(
            1, ExamType.SUB_EXAM, "exam-1", "Prose", ContentType.PROSE, null, "", "", 45);

    CannedExercise generatedMath = generation.generate("owner", mathExercise);
    generation.generate("owner", proseExercise);

    ArgumentCaptor<AiRequest> requests = ArgumentCaptor.forClass(AiRequest.class);
    verify(ai, times(2)).generate(requests.capture());
    assertThat(requests.getAllValues().get(0).focusKeys()).containsExactly("1", "+");
    assertThat(requests.getAllValues().get(1).focusKeys()).containsExactly("a", "1", "+");
    assertThat(generatedMath.expectedAnswer()).isEqualTo("3");
  }
}
