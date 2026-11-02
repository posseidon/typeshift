package hu.elte.ntb.typeshift.health.controller;

import hu.elte.ntb.typeshift.config.AiProperties;
import hu.elte.ntb.typeshift.health.model.ProviderConnectivityStatus;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * On-demand AI provider connectivity check. Dev-profile-only: prod has a single
 * user (the owner) and no developer route (0b-Q2).
 */
@RestController
@Profile("dev")
public class ProviderConnectivityController {

  private final ChatModel chatModel;
  private final String baseUrl;

  public ProviderConnectivityController(ChatModel chatModel, AiProperties props) {
    this.chatModel = chatModel;
    this.baseUrl = props.azureEndpoint();
  }

  /**
   * Probes the configured provider endpoint and reports reachability.
   *
   * @return the connectivity status
   */
  @GetMapping("/api/admin/provider-connectivity")
  public ProviderConnectivityStatus connectivity() {
    try {
      chatModel.call(new Prompt("Reply with OK."));
      return new ProviderConnectivityStatus(true, baseUrl);
    } catch (RuntimeException e) {
      return new ProviderConnectivityStatus(false, baseUrl);
    }
  }
}
