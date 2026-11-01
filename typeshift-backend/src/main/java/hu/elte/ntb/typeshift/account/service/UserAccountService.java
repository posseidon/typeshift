package hu.elte.ntb.typeshift.account.service;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.learning.model.LearningProfile;
import hu.elte.ntb.typeshift.learning.repository.LearningProfileRepository;
import hu.elte.ntb.typeshift.learning.service.LearningProfileService;
import hu.elte.ntb.typeshift.profile.model.ChangePasswordOutcome;
import hu.elte.ntb.typeshift.profile.model.ProfileView;
import hu.elte.ntb.typeshift.signup.model.SignupOutcome;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers the owner: validates the fields, BCrypt-hashes the password, and
 * creates the user together with the learning profile in one transaction
 * (Stories 0.1 and 0.6). Also serves the profile view and updates (Story 0.5).
 */
@Service
public class UserAccountService {

  private static final Pattern USERNAME_PATTERN = Pattern.compile("^\\w{3,30}$");

  private static final DateTimeFormatter MEMBER_SINCE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

  private final UserAccountRepository repository;
  private final PasswordEncoder passwordEncoder;
  private final LearningProfileService learningProfileService;
  private final LearningProfileRepository learningProfiles;

  public UserAccountService(
      UserAccountRepository repository,
      PasswordEncoder passwordEncoder,
      LearningProfileService learningProfileService,
      LearningProfileRepository learningProfiles) {
    this.repository = repository;
    this.passwordEncoder = passwordEncoder;
    this.learningProfileService = learningProfileService;
    this.learningProfiles = learningProfiles;
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

  /**
   * Builds the read-only profile view for the page (Story 0.5); a missing
   * learning profile (pre-Story-0.6 account) falls back to stage 1.
   *
   * @param username the login name
   * @return the profile snapshot
   */
  @Transactional
  public ProfileView profileOf(String username) {
    UserAccount account = requireAccount(username);
    int stage =
        learningProfiles
            .findByUser(account)
            .map(LearningProfile::getCurrentStage)
            .orElse(1);
    return new ProfileView(
        account.getUsername(),
        account.getDisplayName(),
        MEMBER_SINCE.format(account.getCreatedAt()),
        stage);
  }

  /**
   * Finds the database identifier of the authenticated account.
   *
   * @param username the authenticated login name
   * @return the account identifier
   */
  @Transactional(readOnly = true)
  public long accountIdFor(String username) {
    return requireAccount(username).getId();
  }

  /**
   * Replaces the display name when a non-blank value is submitted; blank
   * means "keep the current one".
   *
   * @param username the login name
   * @param displayName the new display name, optional
   */
  @Transactional
  public void updateDisplayName(String username, String displayName) {
    UserAccount account = requireAccount(username);
    if (displayName != null && !displayName.isBlank()) {
      account.setDisplayName(displayName.trim());
      repository.save(account);
    }
  }

  /**
   * Changes the password after verifying the current one (Story 0.5); the
   * outcome carries the refusal message otherwise.
   *
   * @param username the login name
   * @param currentPassword the submitted current password
   * @param newPassword the replacement, at least 8 characters
   * @return the change outcome
   */
  @Transactional
  public ChangePasswordOutcome changePassword(
      String username, String currentPassword, String newPassword) {
    UserAccount account = requireAccount(username);
    if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
      return new ChangePasswordOutcome(false, "Current password is incorrect.");
    }
    if (!isValidPassword(newPassword)) {
      return new ChangePasswordOutcome(false, "New password must be at least 8 characters.");
    }
    account.setPasswordHash(passwordEncoder.encode(newPassword));
    repository.save(account);
    return new ChangePasswordOutcome(true, null);
  }

  private UserAccount requireAccount(String username) {
    return repository.findByUsername(username).orElseThrow(
        () -> new IllegalArgumentException("Unknown account"));
  }

  private static boolean isValidPassword(String password) {
    return password != null && password.length() >= 8;
  }
}
