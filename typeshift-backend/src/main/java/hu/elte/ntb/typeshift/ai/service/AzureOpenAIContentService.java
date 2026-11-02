package hu.elte.ntb.typeshift.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import hu.elte.ntb.typeshift.config.AiProperties;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

/** Generates exercise content through Spring AI's configured chat model. */
// Spring DI: the injected ObjectMapper is a shared, effectively immutable bean.
@SuppressFBWarnings("EI_EXPOSE_REP2")
@Service
public class AzureOpenAIContentService {

  private final ChatModel chatModel;
  private final String endpoint;
  private final String apiKey;
  private final String deployment;
  private final ObjectMapper mapper;
  private final AiPromptFactory prompts;

  public AzureOpenAIContentService(
      ChatModel chatModel,
      AiProperties props,
      ObjectMapper mapper,
      AiPromptFactory prompts) {
    this.chatModel = chatModel;
    this.endpoint = normalizeEndpoint(props.azureEndpoint());
    this.apiKey = props.azureApiKey();
    this.deployment = props.azureDeployment();
    this.mapper = mapper;
    this.prompts = prompts;
  }

  public AiResponse generate(AiRequest request) {
    try {
      requireConfigured();
      String content = chatModel.call(new Prompt(prompts.create(request)))
          .getResult()
          .getOutput()
          .getText();
      return mapper.readValue(extractJson(Optional.ofNullable(content).orElse("")), AiResponse.class);
    } catch (JsonProcessingException | RuntimeException e) {
      throw new AiProviderException("Azure OpenAI generation failed", e);
    }
  }

  // Models occasionally wrap the JSON in markdown fences or prose; extract the object.
  private static String extractJson(String content) {
    int start = content.indexOf('{');
    int end = content.lastIndexOf('}');
    return start >= 0 && end > start ? content.substring(start, end + 1) : content;
  }

  private void requireConfigured() {
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

  private static String normalizeEndpoint(String value) {
    if (value == null) {
      return null;
    }
    String endpoint = value.strip();
    while (endpoint.endsWith("/")) {
      endpoint = endpoint.substring(0, endpoint.length() - 1);
    }
    return endpoint;
  }
}
