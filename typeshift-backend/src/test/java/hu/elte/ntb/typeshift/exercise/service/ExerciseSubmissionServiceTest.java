package hu.elte.ntb.typeshift.exercise.service;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.elte.ntb.typeshift.account.service.UserAccountService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class ExerciseSubmissionServiceTest {

  @Test
  void bindsSubmittedAtAsPostgresCompatibleOffsetDateTime() {
    CannedCatalog catalog = new CannedCatalog();
    UserAccountService accounts = org.mockito.Mockito.mock(UserAccountService.class);
    JdbcTemplate jdbc = org.mockito.Mockito.mock(JdbcTemplate.class);
    when(accounts.accountIdFor("owner")).thenReturn(1L);
    ExerciseSubmissionService submissions = new ExerciseSubmissionService(catalog, accounts, jdbc);
    String typedText = catalog.find(1, "exam-1").orElseThrow().expectedAnswer();

    submissions.submit("owner", 1, "exam-1", typedText, 53);

    ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);
    verify(jdbc).update(startsWith("INSERT INTO practice_session"), arguments.capture());
    assertInstanceOf(OffsetDateTime.class, arguments.getValue()[8]);
  }
}
