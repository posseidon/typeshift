package hu.elte.ntb.typeshift.health.controller;

import hu.elte.ntb.typeshift.health.model.HealthSnapshot;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anchor endpoint for the OpenAPI contract consumed by automated test coverage.
 */
@RestController
@Tag(name = "Health", description = "Service heartbeat")
@SuppressFBWarnings(
    value = "SPRING_ENDPOINT",
    justification = "Health endpoint is intentional monitoring surface.")
public class HealthController {

  /**
   * Reports the service heartbeat.
   *
   * @return the current service status
   */
  @Operation(summary = "Service heartbeat used for monitoring and test tooling")
  @GetMapping("/api/health")
  public HealthSnapshot health() {
    return new HealthSnapshot("up");
  }
}
