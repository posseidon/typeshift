package hu.elte.ntb.typeshift.service;

import hu.elte.ntb.typeshift.model.SignupOutcome;
import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates user accounts with BCrypt-hashed passwords and refuses duplicate usernames.
 */
@Service
public class UserAccountService {

  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;

  public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Creates a new account unless the username is already taken; the outcome carries a
   * refusal message when the username already exists.
   *
   * @param username requested login name
   * @param rawPassword plaintext password as submitted, hashed before storage
   */
  @Transactional
  public SignupOutcome createAccount(String username, String rawPassword) {
    if (repository.existsByUsername(username)) {
      return new SignupOutcome(false, "Username is already taken.");
    }
    UserAccount account = new UserAccount(username, passwordEncoder.encode(rawPassword));
    repository.save(account);
    return new SignupOutcome(true, null);
  }
}
