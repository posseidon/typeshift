package hu.elte.ntb.typeshift.login.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * In-memory brute-force protection (Story 0.2): five consecutive failures per
 * username block logins for fifteen minutes; successes reset the counter.
 *
 * shortcut: in-memory map, restart clears tracking state; move to a table only
 * if attack recovery across restarts ever matters.
 */
@Service
public class LoginAttemptService {

  static final int MAX_FAILURES = 5;
  static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

  private record Attempt(int failures, Instant blockedUntil) {}

  private final Map<String, Attempt> attemptsByUser = new ConcurrentHashMap<>();
  private final Supplier<Instant> clock;

  public LoginAttemptService() {
    this(Instant::now);
  }

  /**
   * Test seam: inject a controllable clock.
   *
   * @param clock the instant supplier
   */
  LoginAttemptService(Supplier<Instant> clock) {
    this.clock = clock;
  }

  /**
   * Reports whether the username is currently inside a block window.
   *
   * @param username the attempted login name
   * @return true while blocked
   */
  public boolean isBlocked(String username) {
    Attempt attempt = attemptsByUser.get(username);
    return attempt != null
        && attempt.blockedUntil() != null
        && clock.get().isBefore(attempt.blockedUntil());
  }

  /**
   * Records one failed login; the fifth consecutive failure arms the block
   * window; a failure after an elapsed window restarts the count at one.
   *
   * @param username the attempted login name
   */
  public void recordFailure(String username) {
    attemptsByUser.compute(
        username,
        (user, attempt) -> {
          boolean stale =
              attempt != null
                  && attempt.blockedUntil() != null
                  && clock.get().isAfter(attempt.blockedUntil());
          if (stale) {
            attempt = null;
          }
          int failures = attempt == null ? 1 : attempt.failures() + 1;
          Instant blockedUntil =
              failures >= MAX_FAILURES ? clock.get().plus(BLOCK_DURATION) : null;
          return new Attempt(failures, blockedUntil);
        });
  }

  /**
   * Clears the failure counter after a successful login.
   *
   * @param username the login name that succeeded
   */
  public void recordSuccess(String username) {
    attemptsByUser.remove(username);
  }
}
