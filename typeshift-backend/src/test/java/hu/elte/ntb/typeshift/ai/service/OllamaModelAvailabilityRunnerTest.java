package hu.elte.ntb.typeshift.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import hu.elte.ntb.typeshift.config.AiProperties;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaModelAvailabilityRunnerTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final AiProperties props =
      new AiProperties("", "", "", "http://localhost:11434", "qwen2.5:14b");

  private static OllamaModelAvailabilityRunner newRunner(
      RestClient.Builder builder, ObjectMapper mapper, AiProperties props) {
    return new OllamaModelAvailabilityRunner(builder, props, mapper);
  }

  @Test
  void detectsConfiguredModel() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://localhost:11434/api/tags"))
        .andRespond(
            withSuccess(
                mapper.writeValueAsString(
                    Map.of("models", List.of(Map.of("name", "qwen2.5:14b")))),
                MediaType.APPLICATION_JSON));

    boolean available = newRunner(builder, mapper, props).isModelAvailable();

    assertThat(available).isTrue();
    server.verify();
  }

  @Test
  void detectsMissingModel() throws Exception {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server
        .expect(requestTo("http://localhost:11434/api/tags"))
        .andRespond(
            withSuccess(
                mapper.writeValueAsString(Map.of("models", List.of(Map.of("name", "other-model")))),
                MediaType.APPLICATION_JSON));

    boolean available = newRunner(builder, mapper, props).isModelAvailable();

    assertThat(available).isFalse();
    server.verify();
  }

  @Test
  void runDoesNotThrowWhenCheckFails() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo("http://localhost:11434/api/tags")).andRespond(withServerError());

    assertThatCode(() -> newRunner(builder, mapper, props).run(null)).doesNotThrowAnyException();
    server.verify();
  }
}
