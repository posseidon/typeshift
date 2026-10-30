package hu.elte.ntb.typeshift.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.elte.ntb.typeshift.account.model.UserAccount;
import hu.elte.ntb.typeshift.account.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.learning.repository.LearningProfileRepository;
import hu.elte.ntb.typeshift.learning.service.LearningProfileService;
import hu.elte.ntb.typeshift.signup.model.SignupOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

  @Mock UserAccountRepository repository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock LearningProfileService learningProfileService;
  @Mock LearningProfileRepository learningProfiles;
  @InjectMocks UserAccountService service;

  @Test
  void createAccountHashesPasswordAndInitialisesLearningProfile() {
    when(repository.existsByUsername("alice")).thenReturn(false);
    when(passwordEncoder.encode("long-enough")).thenReturn("encoded");
    UserAccount saved = new UserAccount("alice", "encoded");
    when(repository.save(any(UserAccount.class))).thenReturn(saved);

    SignupOutcome outcome = service.createAccount("alice", "long-enough", null);

    assertTrue(outcome.created());
    UserAccount passed = (UserAccount) verifySaved();
    verify(learningProfileService).initializeFor(saved);
    assertEquals("encoded", passed.getPasswordHash());
  }

  private UserAccount verifySaved() {
    org.mockito.ArgumentCaptor<UserAccount> captor =
        org.mockito.ArgumentCaptor.forClass(UserAccount.class);
    verify(repository).save(captor.capture());
    return captor.getValue();
  }

  @Test
  void profileCreationFailureSurfacesAndWouldRollBack() {
    when(repository.existsByUsername("bob")).thenReturn(false);
    when(passwordEncoder.encode("long-enough")).thenReturn("encoded");
    when(repository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
    doThrow(new RuntimeException("profile boom"))
        .when(learningProfileService)
        .initializeFor(any(UserAccount.class));

    assertThrows(RuntimeException.class, () -> service.createAccount("bob", "long-enough", null));
    // The service is @Transactional: at runtime the Spring proxy rolls the
    // users insert back together with the failed learning_profiles insert.
  }

  @Test
  void duplicateUsernameIsRefusedWithMessage() {
    when(repository.existsByUsername("alice")).thenReturn(true);

    SignupOutcome outcome = service.createAccount("alice", "long-enough", null);

    assertFalse(outcome.created());
    assertEquals("Username is already taken.", outcome.error());
  }

  @Test
  void shortPasswordIsRefused() {
    when(repository.existsByUsername("alice")).thenReturn(false);

    SignupOutcome outcome = service.createAccount("alice", "short", null);

    assertFalse(outcome.created());
    assertEquals("Password must be at least 8 characters.", outcome.error());
  }

  @Test
  void badUsernameIsRefused() {
    when(repository.existsByUsername("ab")).thenReturn(false);

    SignupOutcome outcome = service.createAccount("ab", "long-enough", null);

    assertFalse(outcome.created());
    assertEquals(
        "Username must be 3 to 30 characters and contain only letters, digits, or underscores.",
        outcome.error());
  }
}
