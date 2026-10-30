package hu.elte.ntb.typeshift.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Warns at startup when the configured Ollama model is not installed (dev only). */
// Spring DI: the injected ObjectMapper is a shared, effectively immutable bean.
@SuppressFBWarnings("EI_EXPOSE_REP2")
@Component
@Profile("dev")
public class OllamaModelAvailabilityRunner implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(OllamaModelAvailabilityRunner.class);

  private final RestClient client;
  private final String model;
  private final ObjectMapper mapper;

  public OllamaModelAvailabilityRunner(
      RestClient.Builder builder, AiProperties props, ObjectMapper mapper) {
    this.client = builder.baseUrl(props.ollamaBaseUrl()).build();
    this.model = props.ollamaModel();
    this.mapper = mapper;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      if (!isModelAvailable()) {
        LOG.warn(
            "Configured Ollama model '{}' is not available; install it or set OLLAMA_MODEL",
            model);
      }
    } catch (JsonProcessingException | RuntimeException e) {
      LOG.warn("Could not check Ollama model availability: {}", e.getMessage());
    }
  }

  boolean isModelAvailable() throws JsonProcessingException {
    String body = client.get().uri("/api/tags").retrieve().body(String.class);
    JsonNode node = mapper.readTree(body);
    for (JsonNode modelNode : node.get("models")) {
      if (model.equals(modelNode.get("name").asText())) {
        return true;
      }
    }
    return false;
  }
}
