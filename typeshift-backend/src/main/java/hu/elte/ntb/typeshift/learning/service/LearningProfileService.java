package hu.elte.ntb.typeshift.learning.service;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.learning.model.LearningProfile;
import hu.elte.ntb.typeshift.learning.repository.LearningProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the initial learning profile of a newly registered owner
 * (Story 0.6).
 */
@Service
public class LearningProfileService {

  private final LearningProfileRepository repository;

  public LearningProfileService(LearningProfileRepository repository) {
    this.repository = repository;
  }

  /**
   * Persists the starting learning profile of the account.
   *
   * @param user the owning account
   * @return the saved profile
   */
  @Transactional
  public LearningProfile initializeFor(UserAccount user) {
    return repository.save(new LearningProfile(user));
  }
}
