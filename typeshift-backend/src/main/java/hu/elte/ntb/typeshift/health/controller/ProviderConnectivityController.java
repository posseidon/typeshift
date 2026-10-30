package hu.elte.ntb.typeshift.health.controller;

import hu.elte.ntb.typeshift.config.AiProperties;
import hu.elte.ntb.typeshift.health.model.ProviderConnectivityStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * On-demand AI provider connectivity check. Dev-profile-only: prod has a single
 * user (the owner) and no developer route (0b-Q2).
 */
@RestController
@Profile("dev")
public class ProviderConnectivityController {

  private final RestClient client;
  private final String baseUrl;

  public ProviderConnectivityController(RestClient.Builder builder, AiProperties props) {
    this.client = builder.build();
    this.baseUrl = props.ollamaBaseUrl();
  }

  /**
   * Probes the configured provider endpoint and reports reachability.
   *
   * @return the connectivity status
   */
  @GetMapping("/api/admin/provider-connectivity")
  public ProviderConnectivityStatus connectivity() {
    try {
      client.get().uri(baseUrl + "/api/tags").retrieve().toBodilessEntity();
      return new ProviderConnectivityStatus(true, baseUrl);
    } catch (RuntimeException e) {
      return new ProviderConnectivityStatus(false, baseUrl);
    }
  }
}
