package hu.elte.ntb.typeshift.account.repository;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to the {@code users} table.
 */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  /**
   * Finds an account by its unique username.
   *
   * @param username the login name to look up
   * @return the matching account, if any
   */
  Optional<UserAccount> findByUsername(String username);

  /**
   * Cheap duplicate check used by the signup path.
   *
   * @param username the login name to test
   * @return true when an account with that username exists
   */
  boolean existsByUsername(String username);
}
