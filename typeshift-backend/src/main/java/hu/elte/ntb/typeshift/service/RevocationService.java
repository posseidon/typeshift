package hu.elte.ntb.typeshift.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hu.elte.ntb.typeshift.model.RevokedToken;
import hu.elte.ntb.typeshift.repository.RevokedTokenRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the server-side token blocklist of ADR-001: idempotent insert on
 * logout and a daily purge of entries whose original token has expired.
 */
// Spring DI: the injected repository is a singleton, not mutable internal state.
@SuppressFBWarnings("EI_EXPOSE_REP2")
@Service
public class RevocationService {

  private static final Logger LOG = LoggerFactory.getLogger(RevocationService.class);

  private final RevokedTokenRepository repository;

  public RevocationService(RevokedTokenRepository repository) {
    this.repository = repository;
  }

  /**
   * Reports whether a token id has been revoked; a missing jti claim is never
   * considered revoked.
   *
   * @param jti the token id claim, may be null
   * @return true when the id is on the blocklist
   */
  @Transactional
  public boolean isRevoked(String jti) {
    return jti != null && repository.existsByJti(jti);
  }

  /**
   * Inserts the blocklist row for a jti when not present; logout called twice
   * stays idempotent.
   *
   * @param jti the token id claim, may be null
   * @param expiresAt the original expiry of the revoked token
   */
  @Transactional
  public void revoke(String jti, Instant expiresAt) {
    if (jti == null || repository.existsByJti(jti)) {
      return;
    }
    repository.save(new RevokedToken(jti, expiresAt));
  }

  /**
   * Daily cleanup: removes blocklist rows whose original token expiry has
   * passed, keeping the table bounded per ADR-001.
   */
  @Scheduled(cron = "0 0 3 * * *")
  @Transactional
  public void purgeExpired() {
    long removed = repository.deleteByExpiresAtBefore(Instant.now());
    LOG.info("Purged {} expired revoked tokens", removed);
  }
}
