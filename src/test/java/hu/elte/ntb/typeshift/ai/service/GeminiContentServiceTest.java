package hu.elte.ntb.typeshift.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.LanguageTarget;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

class GeminiContentServiceTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final ChatModel chatModel = mock(ChatModel.class);

  private static AiRequest request() {
    return new AiRequest(
        1,
        ExamType.SUB_EXAM,
        1,
        List.of("a", "s"),
        List.of("h1", "h2"),
        LanguageTarget.HUNGARIAN,
        null);
  }

  @Test
  void springAiChatModelResponseMapsToEnvelope() throws Exception {
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(response(mapper.writeValueAsString(new AiResponse("1+1"))));

    GeminiContentService service = new GeminiContentService(ChatClient.builder(chatModel), mapper);
    AiResponse resp = service.generate(request());

    assertThat(resp.displayText()).isEqualTo("1+1");
  }

  @Test
  void providerErrorBecomesAiProviderExceptionWithCause() {
    when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("provider error"));

    GeminiContentService service = new GeminiContentService(ChatClient.builder(chatModel), mapper);

    assertThatThrownBy(() -> service.generate(request()))
        .isInstanceOf(AiProviderException.class)
        .hasMessage("Gemini generation failed")
        .hasCauseInstanceOf(Exception.class);
  }

  @Test
  void nonJsonResponseBecomesAiProviderException() throws Exception {
    when(chatModel.call(any(Prompt.class))).thenReturn(response("not-json"));

    GeminiContentService service = new GeminiContentService(ChatClient.builder(chatModel), mapper);

    assertThatThrownBy(() -> service.generate(request())).isInstanceOf(AiProviderException.class);
  }

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void generatedPromptAndContentAreNotLogged(CapturedOutput output) throws Exception {
    String generatedContent = "unique-generated-exercise-content";
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(response(mapper.writeValueAsString(new AiResponse(generatedContent))));

    GeminiContentService service = new GeminiContentService(ChatClient.builder(chatModel), mapper);

    service.generate(request());

    assertThat(output.getAll())
        .doesNotContain("Write natural Hungarian prose for QWERTZ key practice.")
        .doesNotContain(generatedContent);
  }

  @Test
  void markdownFencedJsonResponseParses() throws Exception {
    String json = mapper.writeValueAsString(new AiResponse("text"));
    when(chatModel.call(any(Prompt.class))).thenReturn(response("```json\n" + json + "\n```"));

    GeminiContentService service = new GeminiContentService(ChatClient.builder(chatModel), mapper);
    AiResponse resp = service.generate(request());

    assertThat(resp.displayText()).isEqualTo("text");
  }

  private static ChatResponse response(String content) {
    return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
  }
}
