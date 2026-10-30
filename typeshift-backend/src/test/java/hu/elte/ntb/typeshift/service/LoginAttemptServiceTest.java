package hu.elte.ntb.typeshift.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

  @Test
  void fourFailuresDoNotBlock() {
    LoginAttemptService service = new LoginAttemptService();
    for (int i = 0; i < 4; i++) {
      service.recordFailure("owner");
    }
    assertFalse(service.isBlocked("owner"));
  }

  @Test
  void fiveFailuresBlockForFifteenMinutes() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-29T12:00:00Z"));
    Supplier<Instant> clock = now::get;
    LoginAttemptService service = new LoginAttemptService(clock);
    for (int i = 0; i < 5; i++) {
      service.recordFailure("owner");
    }
    assertTrue(service.isBlocked("owner"));
    now.set(now.get().plus(Duration.ofMinutes(14)));
    assertTrue(service.isBlocked("owner"), "block lasts through the window");
    now.set(now.get().plus(Duration.ofMinutes(1)).plus(Duration.ofSeconds(1)));
    assertFalse(service.isBlocked("owner"), "block lifts after 15 minutes");
  }

  @Test
  void successResetsConsecutiveFailures() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.now());
    LoginAttemptService service = new LoginAttemptService(now::get);
    for (int i = 0; i < 4; i++) {
      service.recordFailure("owner");
    }
    service.recordSuccess("owner");
    assertFalse(service.isBlocked("owner"));
    for (int i = 0; i < 4; i++) {
      service.recordFailure("owner");
    }
    assertFalse(service.isBlocked("owner"), "counter restarted after success");
  }

  @Test
  void failureAfterElapsedBlockCountsFromOne() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.now());
    LoginAttemptService service = new LoginAttemptService(now::get);
    for (int i = 0; i < 5; i++) {
      service.recordFailure("owner");
    }
    now.set(now.get().plus(Duration.ofMinutes(16)));
    assertFalse(service.isBlocked("owner"));
    now.set(now.get().plus(Duration.ofMinutes(16)));
    service.recordFailure("owner");
    assertFalse(service.isBlocked("owner"), "post-block failure is a fresh count of 1");
  }

  @Test
  void failuresAreTrackedPerUsername() {
    LoginAttemptService service = new LoginAttemptService();
    for (int i = 0; i < 5; i++) {
      service.recordFailure("owner");
    }
    assertFalse(service.isBlocked("other"));
  }
}
