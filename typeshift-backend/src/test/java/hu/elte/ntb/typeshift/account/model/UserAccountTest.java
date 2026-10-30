package hu.elte.ntb.typeshift.account.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class UserAccountTest {

  @Test
  void constructorKeepsGivenDisplayName() {
    UserAccount account = new UserAccount("alice", "hash", "Alice");
    assertEquals("alice", account.getUsername());
    assertEquals("hash", account.getPasswordHash());
    assertEquals("Alice", account.getDisplayName());
    assertNotNull(account.getCreatedAt());
    assertNotNull(account.getUpdatedAt());
  }

  @Test
  void blankDisplayNameFallsBackToUsername() {
    UserAccount account = new UserAccount("alice", "hash", null);
    assertEquals("alice", account.getDisplayName());
    account.setDisplayName(" ");
    assertEquals("alice", account.getDisplayName());
  }
}
