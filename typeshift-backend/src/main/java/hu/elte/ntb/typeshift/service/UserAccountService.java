package hu.elte.ntb.typeshift.service;

import hu.elte.ntb.typeshift.model.SignupOutcome;
import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers the owner: validates the fields, BCrypt-hashes the password, and
 * creates the user together with the learning profile in one transaction
 * (Stories 0.1 and 0.6).
 */
@Service
public class UserAccountService {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^\\w{3,30}$");

  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final LearningProfileService learningProfileService;

  public UserAccountService(
      UserAccountRepository repository,
      PasswordEncoder passwordEncoder,
      LearningProfileService learningProfileService) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.learningProfileService = learningProfileService;
  }

  /**
   * Creates the account with its learning profile unless the username is
   * already taken or the fields violate the rules; the outcome carries a
   * refusal message otherwise.
   *
   * @param username requested login name, 3-30 word characters
   * @param password plaintext password, at least 8 characters, hashed before storage
   * @param displayName optional profile display name
   * @return the signup outcome
   */
  @Transactional
  public SignupOutcome createAccount(String username, String password, String displayName) {
    String candidate = username == null ? "" : username.trim();
    if (repository.existsByUsername(candidate)) {
      return new SignupOutcome(false, "Username is already taken.");
    }
    if (!USERNAME_PATTERN.matcher(candidate).matches()) {
      return new SignupOutcome(
          false, "Username must be 3 to 30 characters and contain only letters, digits, or underscores.");
    }
    if (!isValidPassword(password)) {
      return new SignupOutcome(false, "Password must be at least 8 characters.");
    }
    UserAccount account = new UserAccount(candidate, passwordEncoder.encode(password), displayName);
    UserAccount saved = repository.save(account);
    learningProfileService.initializeFor(saved);
    return new SignupOutcome(true, null);
  }

  private static boolean isValidPassword(String password) {
    return password != null && password.length() >= 8;
  }
}
