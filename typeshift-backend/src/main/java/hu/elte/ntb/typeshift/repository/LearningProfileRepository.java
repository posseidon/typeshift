package hu.elte.ntb.typeshift.repository;

import hu.elte.ntb.typeshift.model.LearningProfile;
import hu.elte.ntb.typeshift.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to the {@code learning_profiles} table.
 */
public interface LearningProfileRepository extends JpaRepository<LearningProfile, Long> {

  /**
   * Finds the learning profile of an account.
   *
   * @param user the owning account
   * @return the matching profile, if any
   */
  Optional<LearningProfile> findByUser(UserAccount user);
}
