package hu.elte.ntb.typeshift.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.elte.ntb.typeshift.model.LearningProfile;
import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.LearningProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LearningProfileServiceTest {

  @Mock
  LearningProfileRepository repository;

  @InjectMocks
  LearningProfileService service;

  @Test
  void initializeForUsesStageOneDefaults() {
    Mockito.when(repository.save(Mockito.any(LearningProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    LearningProfile profile = service.initializeFor(new UserAccount("owner", "hash"));

    assertEquals("owner", profile.getUser().getUsername());
    assertEquals(1, profile.getCurrentStage());
    assertEquals(0, profile.getTotalPracticeMinutes());
    assertNull(profile.getProgramStartDate());
    assertTrue(profile.getUnlockedStages().contains(1));
  }
}
