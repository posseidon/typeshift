package hu.elte.ntb.typeshift.health.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hu.elte.ntb.typeshift.config.AiProperties;
import hu.elte.ntb.typeshift.health.model.ProviderConnectivityStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

class ProviderConnectivityControllerTest {

  private final AiProperties props =
      new AiProperties("http://azure.example", "test-api-key", "Phi-4-mini-instruct");
  private final ChatModel chatModel = mock(ChatModel.class);

  @Test
  void reportsReachableWhenProviderResponds() {
    when(chatModel.call(any(Prompt.class)))
        .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("OK")))));

    ProviderConnectivityStatus status =
        new ProviderConnectivityController(chatModel, props).connectivity();

    assertThat(status.reachable()).isTrue();
    assertThat(status.endpoint()).isEqualTo("http://azure.example");
  }

  @Test
  void reportsUnreachableWhenProviderFails() {
    when(chatModel.call(any(Prompt.class))).thenThrow(new IllegalStateException("unavailable"));

    ProviderConnectivityStatus status =
        new ProviderConnectivityController(chatModel, props).connectivity();

    assertThat(status.reachable()).isFalse();
  }
}
