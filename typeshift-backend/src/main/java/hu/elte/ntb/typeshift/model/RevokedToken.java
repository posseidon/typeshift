package hu.elte.ntb.typeshift.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Server-side blocklist entry of ADR-001: a logged-out token id that must be
 * rejected until its original expiry date.
 */
@Entity
@Table(name = "revoked_token")
public class RevokedToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String jti;

  @Column(nullable = false)
  private Instant revokedAt;

  @Column(nullable = false)
  private Instant expiresAt;

  protected RevokedToken() {}

  /**
   * Creates a blocklist entry.
   *
   * @param jti the id claim of the revoked token
   * @param expiresAt the original expiry of the revoked token
   */
  public RevokedToken(String jti, Instant expiresAt) {
    this.revokedAt = Instant.now();
    this.jti = jti;
    this.expiresAt = expiresAt;
  }

  public Long getId() {
    return id;
  }

  public String getJti() {
    return jti;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
