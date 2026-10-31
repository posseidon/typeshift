package hu.elte.ntb.typeshift.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity holding a TypeShift account: unique username, BCrypt password
 * hash, display name, and audit timestamps.
 */
@Entity
@Table(name = "app_user")
@Getter
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  @Setter
  private String passwordHash;

  @Column
  private String displayName;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected UserAccount() {}

  /**
   * Creates an account for persistence; the hash must already be BCrypt-encoded.
   *
   * @param username unique login name
   * @param passwordHash BCrypt password hash, never a plaintext password
   * @param displayName profile display name; blank falls back to the username
   */
  public UserAccount(String username, String passwordHash, String displayName) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.displayName = defaultDisplayName(displayName);
    Instant created = Instant.now();
    this.createdAt = created;
    this.updatedAt = created;
  }

  /**
   * Convenience constructor; the display name defaults to the username.
   *
   * @param username unique login name
   * @param passwordHash BCrypt password hash
   */
  public UserAccount(String username, String passwordHash) {
    this(username, passwordHash, username);
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (updatedAt == null) {
      updatedAt = createdAt;
    }
    if (displayName == null || displayName.isBlank()) {
      displayName = username;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  private static String defaultDisplayName(String displayName) {
    if (displayName == null || displayName.isBlank()) {
      return null;
    }
    return displayName.trim();
  }

  public String getDisplayName() {
    return displayName == null ? username : displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = defaultDisplayName(displayName);
  }
}
