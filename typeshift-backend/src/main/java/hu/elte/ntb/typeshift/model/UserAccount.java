package hu.elte.ntb.typeshift.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA entity holding a TypeShift account: unique username and BCrypt password hash.
 */
@Entity
@Table(name = "users")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String passwordHash;

  protected UserAccount() {}

  /**
   * Creates an account for persistence; the hash must already be BCrypt-encoded.
   *
   * @param username unique login name
   * @param passwordHash BCrypt password hash, never a plaintext password
   */
  public UserAccount(String username, String passwordHash) {
    this.username = username;
    this.passwordHash = passwordHash;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }
}
