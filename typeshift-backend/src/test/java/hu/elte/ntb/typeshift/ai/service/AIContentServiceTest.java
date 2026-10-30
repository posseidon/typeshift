package hu.elte.ntb.typeshift.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.ai.model.ContentType;
import hu.elte.ntb.typeshift.ai.model.ExamType;
import hu.elte.ntb.typeshift.ai.model.LanguageTarget;
import hu.elte.ntb.typeshift.config.AiProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AIContentServiceTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final AiProperties props =
      new AiProperties(
          "http://azure.example", "test-api-key", "gpt-4o-deploy",
          "http://localhost:11434", "qwen2.5:14b");

  private static AiRequest request() {
    return new AiRequest(
        1, ExamType.SUB_EXAM, List.of("a", "s"), List.of("h1", "h2"), LanguageTarget.HUNGARIAN, null);
  }

  @Test
  void ollamaPostsToGenerateAndMapsEnvelope() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    String envelope =
        mapper.writeValueAsString(new AiResponse(ContentType.PROSE, "abc", "abc", "sha", 60));
    server
        .expect(requestTo("http://localhost:11434/api/generate"))
        .andRespond(
            withSuccess(
                mapper.writeValueAsString(Map.of("response", envelope)),
                MediaType.APPLICATION_JSON));

    AIContentService service = new OllamaContentService(builder, props, mapper);
    AiResponse resp = service.generate(request());

    assertThat(resp.contentType()).isEqualTo(ContentType.PROSE);
    assertThat(resp.displayText()).isEqualTo("abc");
    server.verify();
  }

  @Test
  void azurePostsToDeploymentAndMapsEnvelope() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    String envelope =
        mapper.writeValueAsString(new AiResponse(ContentType.MATH, "1+1", "2", "sha2", 30));
    String body =
        mapper.writeValueAsString(
            Map.of("choices", List.of(Map.of("message", Map.of("content", envelope)))));
    server
        .expect(
            requestTo(
                "http://azure.example/openai/deployments/gpt-4o-deploy/chat/completions"))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

    AIContentService service = new AzureOpenAIContentService(builder, props, mapper);
    AiResponse resp = service.generate(request());

    assertThat(resp.contentType()).isEqualTo(ContentType.MATH);
    assertThat(resp.expectedAnswer()).isEqualTo("2");
    server.verify();
  }

  @Test
  void providerErrorBecomesAiProviderExceptionWithCause() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo("http://localhost:11434/api/generate")).andRespond(withServerError());

    AIContentService service = new OllamaContentService(builder, props, mapper);

    assertThatThrownBy(() -> service.generate(request()))
        .isInstanceOf(AiProviderException.class)
        .hasCauseInstanceOf(Exception.class);
    server.verify();
  }

  @Test
  void nonJsonResponseBecomesAiProviderException() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://localhost:11434/api/generate"))
        .andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

    AIContentService service = new OllamaContentService(builder, props, mapper);

    assertThatThrownBy(() -> service.generate(request())).isInstanceOf(AiProviderException.class);
    server.verify();
  }
}
