package hu.elte.ntb.typeshift.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.config.AiProperties;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Ollama provider, active in the dev profile. */
// Spring DI: the injected ObjectMapper is a shared, effectively immutable bean.
@SuppressFBWarnings("EI_EXPOSE_REP2")
@Component
@Profile("dev")
public class OllamaContentService implements AIContentService {

  private final RestClient client;
  private final String model;
  private final ObjectMapper mapper;

  public OllamaContentService(RestClient.Builder builder, AiProperties props, ObjectMapper mapper) {
    this.client = builder.baseUrl(props.ollamaBaseUrl()).build();
    this.model = props.ollamaModel();
    this.mapper = mapper;
  }

  @Override
  public AiResponse generate(AiRequest request) {
    try {
      String prompt = mapper.writeValueAsString(request);
      String body =
          client
              .post()
              .uri("/api/generate")
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("model", model, "prompt", prompt, "stream", false))
              .retrieve()
              .body(String.class);
      JsonNode node = mapper.readTree(body);
      return mapper.readValue(node.get("response").asText(), AiResponse.class);
    } catch (JsonProcessingException | RuntimeException e) {
      throw new AiProviderException("Ollama generation failed", e);
    }
  }
}
