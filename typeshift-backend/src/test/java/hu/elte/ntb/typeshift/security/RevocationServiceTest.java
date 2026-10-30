package hu.elte.ntb.typeshift.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevocationServiceTest {

  @Mock RevokedTokenRepository repository;

  @InjectMocks RevocationService service;

  @Test
  void revokeInsertsRowWithRevokedAtAndExpiry() {
    Instant before = Instant.now();
    Instant expiry = before.plusSeconds(3600);

    service.revoke("jti-1", expiry);

    ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
    verify(repository).save(captor.capture());
    assertEquals("jti-1", captor.getValue().getJti());
    assertEquals(expiry, captor.getValue().getExpiresAt());
    Instant revokedAt = captor.getValue().getRevokedAt();
    assertTrue(!revokedAt.isBefore(before) && revokedAt.isBefore(before.plusSeconds(60)));
  }

  @Test
  void revokeIsIdempotent() {
    when(repository.existsByJti("jti-1")).thenReturn(false).thenReturn(true);

    service.revoke("jti-1", Instant.now().plusSeconds(60));
    service.revoke("jti-1", Instant.now().plusSeconds(60));

    verify(repository, times(1)).save(any(RevokedToken.class));
  }

  @Test
  void revokeIgnoresNullJti() {
    service.revoke(null, Instant.now());
    verify(repository, never()).save(any(RevokedToken.class));
  }

  @Test
  void isRevokedNullSafeFalse() {
    assertFalse(service.isRevoked(null));
  }

  @Test
  void isRevokedConsultsRepository() {
    when(repository.existsByJti("jti-1")).thenReturn(true);

    assertTrue(service.isRevoked("jti-1"));
  }
}
