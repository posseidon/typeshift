package hu.elte.ntb.typeshift.repository;

import hu.elte.ntb.typeshift.model.RevokedToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to the {@code revoked_token} blocklist table.
 */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

  /**
   * Cheap blocklist probe for the token id claim.
   *
   * @param jti the token id to test
   * @return true when the id has been revoked
   */
  boolean existsByJti(String jti);

  /**
   * Bulk delete of blocklist rows whose original token expiry has passed.
   *
   * @param cutoff the instant before which rows are deleted
   * @return the number of removed rows
   */
  long deleteByExpiresAtBefore(Instant cutoff);
}
