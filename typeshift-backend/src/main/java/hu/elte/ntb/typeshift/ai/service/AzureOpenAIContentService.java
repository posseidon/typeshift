package hu.elte.ntb.typeshift.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.config.AiProperties;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Azure OpenAI provider, active in the prod profile. */
// Fail-fast startup + Spring DI: constructor throws on missing config; ObjectMapper is a shared bean.
@SuppressFBWarnings({"CT_CONSTRUCTOR_THROW", "EI_EXPOSE_REP2"})
@Component
@Profile("prod")
public class AzureOpenAIContentService implements AIContentService {

  private static final String API_VERSION = "2024-02-01";

  private final RestClient client;
  private final String endpoint;
  private final String apiKey;
  private final String deployment;
  private final ObjectMapper mapper;

  public AzureOpenAIContentService(
      RestClient.Builder builder, AiProperties props, ObjectMapper mapper) {
    this.client = builder.build();
    this.endpoint = props.azureEndpoint();
    this.apiKey = props.azureApiKey();
    this.deployment = props.azureDeployment();
    this.mapper = mapper;
    if (endpoint == null
        || endpoint.isBlank()
        || apiKey == null
        || apiKey.isBlank()
        || deployment == null
        || deployment.isBlank()) {
      throw new IllegalStateException(
          "Azure OpenAI is not configured: AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY, and "
              + "AZURE_OPENAI_DEPLOYMENT must all be set");
    }
  }

  @Override
  public AiResponse generate(AiRequest request) {
    try {
      String prompt = mapper.writeValueAsString(request);
      String uri =
          endpoint + "/openai/deployments/" + deployment + "/chat/completions?api-version=" + API_VERSION;
      String body =
          client
              .post()
              .uri(uri)
              .header("api-key", apiKey)
              .contentType(MediaType.APPLICATION_JSON)
              .body(Map.of("messages", List.of(Map.of("role", "user", "content", prompt))))
              .retrieve()
              .body(String.class);
      JsonNode node = mapper.readTree(body);
      String content = node.get("choices").get(0).get("message").get("content").asText();
      return mapper.readValue(content, AiResponse.class);
    } catch (JsonProcessingException | RuntimeException e) {
      throw new AiProviderException("Azure OpenAI generation failed", e);
    }
  }
}
