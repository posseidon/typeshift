package hu.elte.ntb.typeshift.health.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import hu.elte.ntb.typeshift.config.AiProperties;
import hu.elte.ntb.typeshift.health.model.ProviderConnectivityStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProviderConnectivityControllerTest {

  private final AiProperties props =
      new AiProperties("", "", "", "http://localhost:11434", "qwen2.5:14b");

  @Test
  void reportsReachableWhenProviderResponds() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo("http://localhost:11434/api/tags")).andRespond(withSuccess());

    ProviderConnectivityStatus status =
        new ProviderConnectivityController(builder, props).connectivity();

    assertThat(status.reachable()).isTrue();
    assertThat(status.endpoint()).isEqualTo("http://localhost:11434");
    server.verify();
  }

  @Test
  void reportsUnreachableWhenProviderFails() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(requestTo("http://localhost:11434/api/tags")).andRespond(withServerError());

    ProviderConnectivityStatus status =
        new ProviderConnectivityController(builder, props).connectivity();

    assertThat(status.reachable()).isFalse();
    server.verify();
  }
}
