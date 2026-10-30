package hu.elte.ntb.typeshift;

import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal authenticated API probe used by the auth integration tests. */
@RestController
public class AuthProbeController {

  @GetMapping("/api/whoami")
  public Map<String, String> whoami(Principal principal) {
    return Map.of("username", principal.getName());
  }
}
