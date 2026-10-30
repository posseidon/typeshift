package hu.elte.ntb.typeshift.security;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the signed JWT session tokens of ADR-001: fixed HS256,
 * 30-day lifetime, subject and token id required.
 */
// Fail-fast startup: the constructor throws on a short/missing secret.
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
@Service
public class JwtTokenService {

  /** Session length locked by ADR-001. */
  static final Duration TOKEN_LIFETIME = Duration.ofDays(30);

  /** HMAC-SHA256 needs at least 256 bits of key material. */
  private static final int MIN_SECRET_BYTES = 32;

  private final SecretKey key;

  /**
   * Builds the signing key from the configured secret and refuses to start
   * without adequate key material.
   *
   * @param properties the auth properties holding the secret
   */
  public JwtTokenService(AuthProperties properties) {
    byte[] secret =
        properties.jwtSecret() == null
            ? new byte[0]
            : properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
    if (secret.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT secret missing or shorter than " + MIN_SECRET_BYTES + " bytes; set JWT_SECRET.");
    }
    this.key = Keys.hmacShaKeyFor(secret);
  }

  /**
   * Issues a token with the standard 30-day lifetime.
   *
   * @param username the token subject
   * @return the signed compact token string
   */
  public String issue(String username) {
    return issue(username, TOKEN_LIFETIME);
  }

  /**
   * Issues a token with an explicit lifetime; tests use negative durations to
   * create already-expired tokens. Production callers use the 30-day overload.
   *
   * @param username the token subject
   * @param lifetime how long the token stays valid
   * @return the signed compact token string
   */
  public String issue(String username, Duration lifetime) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(lifetime)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Verifies the signature with the fixed HS256 key and the validity window;
   * unsigned ("none") or tampered tokens never parse.
   *
   * @param token the presented compact token
   * @return the parsed claims
   * @throws io.jsonwebtoken.ExpiredJwtException when the token is expired
   * @throws io.jsonwebtoken.JwtException on any signature, format, or algorithm problem
   */
  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
