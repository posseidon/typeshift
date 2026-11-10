package hu.elte.ntb.typeshift.exercise.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.ai.model.LanguageTarget;
import hu.elte.ntb.typeshift.ai.service.AiProviderException;
import hu.elte.ntb.typeshift.ai.service.GeminiContentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.ai.google.genai.api-key=test-key")
@AutoConfigureMockMvc
class ProseExerciseControllerTest {

  @Autowired MockMvc mvc;
  @MockBean GeminiContentService ai;

  @Test
  void shellPageIsPublicAndServesLoadingPlaceholder() throws Exception {
    mvc.perform(get("/exercise/4/exam-1").param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("hx-get=\"/exercise/4/exam-1/docket\"")))
        .andExpect(content().string(containsString("id=\"exercise-loading\"")))
        .andExpect(content().string(containsString("htmx:beforeSwap")))
        .andExpect(content().string(containsString("Stage 4")))
        .andExpect(content().string(not(containsString("data-target"))));
  }

  @Test
  void publicDocketServesAiGeneratedText() throws Exception {
    when(ai.generate(any(AiRequest.class))).thenReturn(new AiResponse("Generated practice text."));

    mvc.perform(get("/exercise/4/exam-1/docket").param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"typing-docket\"")))
        .andExpect(content().string(containsString(
            "id=\"docket-title\">Hungarian prose transcribing [civil service record]</h2>")))
        .andExpect(content().string(containsString("data-target=\"Generated practice text.\"")))
        .andExpect(content().string(containsString("data-mode=\"prose\"")))
        .andExpect(content().string(containsString("<textarea id=\"practice-input\"")));
  }

  @Test
  void completionReturnsToStageMapWithoutSubmittingResults() throws Exception {
    when(ai.generate(any(AiRequest.class))).thenReturn(new AiResponse("Practice prose."));

    mvc.perform(get("/exercise/4/exam-1/docket").param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("method=\"get\"")))
        .andExpect(content().string(containsString("action=\"/stage-map\"")))
        .andExpect(content().string(containsString("Quit exercise")))
        .andExpect(content().string(not(containsString("name=\"typedText\""))))
        .andExpect(content().string(not(containsString("name=\"elapsedSeconds\""))))
        .andExpect(content().string(not(containsString("name=\"exerciseToken\""))));
  }

  @Test
  void providerFailureKeepsThePublicShellRetryable() throws Exception {
    when(ai.generate(any(AiRequest.class))).thenThrow(new AiProviderException("provider offline", null));

    mvc.perform(get("/exercise/4/exam-1").param("lang", "en"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-failed=")))
        .andExpect(content().string(containsString("data-retry=")))
        .andExpect(content().string(containsString("id=\"exercise-loading\"")));
    assertThatThrownBy(() -> mvc.perform(get("/exercise/4/exam-1/docket")).andReturn())
        .hasRootCauseInstanceOf(AiProviderException.class);
  }

  @Test
  void removedResultSubmissionRouteIsNotAvailable() throws Exception {
    mvc.perform(post("/exercise/4/exam-1/submit"))
        .andExpect(status().isNotFound());
  }

  @Test
  void numericDocketServesKeypad() throws Exception {
    when(ai.generate(any(AiRequest.class))).thenReturn(new AiResponse("1 + 2 = ?"));

    mvc.perform(get("/exercise/3/exam-1/docket"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("data-mode=\"numeric\"")))
        .andExpect(content().string(containsString(
            "id=\"docket-title\">Keypad orientation &amp; decimal tallying</h2>")))
        .andExpect(content().string(containsString("id=\"keypad\"")));
  }

  @Test
  void proseDocketOmitsKeyPad() throws Exception {
    when(ai.generate(any(AiRequest.class))).thenReturn(new AiResponse("Practice prose."));

    mvc.perform(get("/exercise/4/exam-1/docket"))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("id=\"keypad\""))))
        .andExpect(content().string(not(containsString("id=\"practice-pad\""))))
        .andExpect(content().string(not(containsString("NumLock"))));
  }

  @Test
  void unknownStageAndExamAreNotFound() throws Exception {
    mvc.perform(get("/exercise/9/exam-1")).andExpect(status().isNotFound());
    mvc.perform(get("/exercise/4/nope")).andExpect(status().isNotFound());
    mvc.perform(get("/exercise/9/exam-1/docket")).andExpect(status().isNotFound());
    mvc.perform(get("/exercise/4/nope/docket")).andExpect(status().isNotFound());
  }

  @Test
  void legacyExerciseRouteIsNotFound() throws Exception {
    mvc.perform(get("/exercise")).andExpect(status().isNotFound());
  }

  @Test
  void docketUsesSessionLocaleForGeneratedContent() throws Exception {
    when(ai.generate(any(AiRequest.class))).thenReturn(new AiResponse("Practice text."));

    MockHttpSession session =
        (MockHttpSession) mvc.perform(get("/exercise/4/exam-1").param("lang", "en"))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession();

    mvc.perform(get("/exercise/4/exam-1/docket").session(session))
        .andExpect(status().isOk());

    ArgumentCaptor<AiRequest> request = ArgumentCaptor.forClass(AiRequest.class);
    verify(ai).generate(request.capture());
    assertThat(request.getValue().languageTarget()).isEqualTo(LanguageTarget.ENGLISH);
  }
}
