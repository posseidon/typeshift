package hu.elte.ntb.typeshift.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.elte.ntb.typeshift.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

  private static final String SECRET = "test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms";

  private final JwtTokenService tokens =
      new JwtTokenService(new AuthProperties(SECRET, false));

  @Test
  void roundTripKeepsSubjectJtiAndTimestamps() {
    String token = tokens.issue("owner");
    Claims claims = tokens.parse(token);
    assertEquals("owner", claims.getSubject());
    assertTrue(claims.getId() != null && !claims.getId().isBlank());
    assertTrue(claims.getIssuedAt() != null);
    assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
  }

  @Test
  void lifetimeIsThirtyDays() {
    Claims claims = tokens.parse(tokens.issue("owner"));
    Duration lifetime =
        Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
    assertEquals(Duration.ofDays(30).toMinutes(), lifetime.toMinutes());
  }

  @Test
  void expiredTokenIsRejected() {
    String token = tokens.issue("owner", Duration.ofSeconds(-60));
    assertThrows(ExpiredJwtException.class, () -> tokens.parse(token));
  }

  @Test
  void tamperedTokenIsRejected() {
    String token = tokens.issue("owner");
    String tampered = token.substring(0, token.length() - 2) + "xx";
    assertThrows(JwtException.class, () -> tokens.parse(tampered));
  }

  @Test
  void unsignedTokenIsRejected() {
    String unsigned = Jwts.builder().subject("owner").compact();
    assertThrows(JwtException.class, () -> tokens.parse(unsigned));
  }

  @Test
  void shortSecretFailsFastAtStartup() {
    assertThrows(
        IllegalStateException.class,
        () -> new JwtTokenService(new AuthProperties("too-short", false)));
  }

  @Test
  void missingSecretFailsFastAtStartup() {
    assertThrows(
        IllegalStateException.class, () -> new JwtTokenService(new AuthProperties("", false)));
  }
}
