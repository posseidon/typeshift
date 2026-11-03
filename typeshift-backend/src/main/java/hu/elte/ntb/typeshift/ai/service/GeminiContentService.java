package hu.elte.ntb.typeshift.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.ai.model.AiRequest;
import hu.elte.ntb.typeshift.ai.model.AiResponse;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/** Generates exercise content through Spring AI's configured chat model. */
// Spring DI: the injected ObjectMapper is a shared, effectively immutable bean.
@SuppressFBWarnings("EI_EXPOSE_REP2")
@Service
public class GeminiContentService {

  private final ChatModel chatModel;
  private final ObjectMapper mapper;

  public GeminiContentService(
      ChatModel chatModel,
      ObjectMapper mapper) {
    this.chatModel = chatModel;
    this.mapper = mapper;
  }

  public AiResponse generate(AiRequest request) {
    try {
      return parseResponse(requestContent(request));
    } catch (JsonProcessingException | RuntimeException e) {
      throw new AiProviderException("Gemini generation failed", e);
    }
  }

  private String requestContent(AiRequest request) {
    return chatModel
        .prompt()
        .
        .call(AiPromptFactory.create(request))
        .getResult()
        .getOutput()
        .getText();
  }

  private AiResponse parseResponse(String content) throws JsonProcessingException {
    String json = extractJson(Optional.ofNullable(content).orElse(""));
    return mapper.readValue(json, AiResponse.class);
  }

  // Models occasionally wrap the JSON in markdown fences or prose; extract the object.
  private static String extractJson(String content) {
    int start = content.indexOf('{');
    int end = content.lastIndexOf('}');
    return start >= 0 && end > start ? content.substring(start, end + 1) : content;
  }
}
