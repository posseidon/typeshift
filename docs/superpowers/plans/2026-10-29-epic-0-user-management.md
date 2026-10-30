# Epic 0 — User Management Implementation Plan (Stories 0.1–0.6)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement complete user management for TypeShift — registration with validation, JWT login in an httpOnly cookie, token validation on every protected route, logout with token revocation plus daily blocklist purge, profile view/update, and atomic learning-profile initialisation — per ADR-001, ADR-013, ADR-014.

**Architecture:** Spring Boot 3.5 (Java 21) multi-module Maven project. `typeshift-backend` serves Thymeleaf pages (ADR-014) and REST APIs over SQLite. Custom auth (ADR-001): login issues a signed HS256 JWT (30-day) with jti, delivered in an httpOnly SameSite=Lax cookie. A servlet filter validates the cookie on every route except login/signup/logout/static assets: invalid → 401 JSON for `/api/**`, redirect to `/login` for pages. Logout inserts the jti into `revoked_token`; a daily `@Scheduled` job purges expired rows. Registration creates `users` row + `learning_profiles` row (stage 1, unlocked {1}, no program start, 0 minutes) in one transaction (ADR-013). Passwords are BCrypt cost ≥ 12.

**Tech Stack:** Java 21, Spring Boot 3.5.16 (Web, Security, Data JPA, Validation, Thymeleaf), JJWT 0.12.6, SQLite (hibernate-community-dialect), Maven, JUnit 5 + Mockito + MockMvc.

## Global Constraints

Copied verbatim from the stories and ADRs; every task implicitly includes these:

- ADR-001: 30-day JWT, httpOnly cookie, Secure flag on in prod, SameSite Lax, server-side invalidation via `revoked_token` (jti, revoked_at, expires_at), daily purge, no refresh tokens, token never in URLs or local storage.
- JWT claims: subject, jti, issued-at, expiry = issued-at + 30 days. Algorithm fixed HS256; "none" rejected. Signing secret from `JWT_SECRET` env var; never committed.
- Token validation filter: checks cookie on every route except login, registration, and static assets. Missing/malformed/expired/revoked rejected. API → 401, pages → redirect to login. Revoked = jti in `revoked_token`. Each rejection logged with a reason code; token content never logged.
- Login: username + password. One generic failure message regardless of which field was wrong. Brute force: 5 consecutive failures per username → blocked 15 minutes.
- Registration: username 3–30 chars, letters/digits/underscore only; password min 8 characters; display name optional. BCrypt cost factor 12. Duplicate username → clear validation message. Success creates user + triggers Story 0.6, then redirects to login. Plaintext never stored or logged.
- Story 0.6: learning profile row in same transaction as user; initial state current stage 1, unlocked stages = {1}, program start date empty, total practice minutes 0. Profile-creation failure rolls back user creation. (Program start date set later, at first sub-exam attempt — later epic, do not implement here, leave field null.)
- Profile (Story 0.5): shows username, display name, account creation date, current stage. Display name editable. Password change requires current password + new password ≥ 8 chars. Keyboard layout constant `HU_QWERTZ` (read-only). Interface language English only in v1 (OQ-2).
- Quality gates: `mvn verify` at repo root runs Checkstyle (google_checks, failOnViolation), PMD, SpotBugs — every task's commit must pass. 2-space indent, javadoc on public members (existing repo convention).
- Parent POM: `hu.elte.ntb.typeshift:typeshift-parent:1.0.0-SNAPSHOT`, backend artifact `typeshift-backend`. Base package `hu.elte.ntb.typeshift`. Run single tests with `mvn -f typeshift-backend/pom.xml test -Dtest=<Class> -Dsurefire.failIfNoSpecifiedTests=false`.

## Starting point (existing code inventory)

- `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/UserAccount.java` — JPA entity `users` (id, username unique, passwordHash). Has 2-arg ctor `(username, passwordHash)`.
- `model/SignupOutcome.java` — `record SignupOutcome(boolean created, String error)`.
- `repository/UserAccountRepository.java` — `findByUsername(String)`, `existsByUsername(String)`.
- `service/UserAccountService.java` — `@Transactional createAccount(username, rawPassword)` returning `SignupOutcome`, BCrypt-encodes, refuses duplicates.
- `config/SecurityConfig.java` — Spring Security form-login chain; permitAll on `/`, `/login`, `/signup`, `/css/**`, `/js/**`, swagger, `/api/health`; `PasswordEncoder` bean (default BCrypt cost).
- `controller/SignupController.java` — GET/POST `/signup`, `@NotBlank` params, redirects to `/login?signup` on success, re-renders with `error` model attr.
- `controller/LoginController.java` — GET `/login` only (form login handled POST). Model attrs: `title`, `contentFragment`, `error` (boolean).
- `service/TypeShiftUserDetailsService.java` — UserDetailsService for form login (to be deleted in Task 5).
- Frontend module `typeshift-frontend/src/main/resources/templates/`: `layout.html` (shell: `title`, `contentFragment`, `main th:replace="~{${contentFragment} :: content}"`, topnav with brand link only), `login.html`, `signup.html`, `index.html`; static `css/style.css`, `js/engine.js`.
- `application.properties`: sqlite file `typeshift.db`, `ddl-auto=update`, virtual threads on.
- **No `typeshift-backend/src/test` exists yet.** No test dependencies in backend POM.
- `application.properties` currently has no JWT/cookie settings.

## File map (final state)

Backend (`typeshift-backend/`, base `src/main/java/hu/elte/ntb/typeshift/`, tests under `src/test/java/hu/elte/ntb/typeshift/`):

| File | Action | Responsibility |
|---|---|---|
| `pom.xml` | Modify | add jjwt 0.12.6 + spring-boot-starter-test |
| `config/AuthProperties.java` | Create | JWT secret + cookie-Secure flag from properties |
| `config/SecurityConfig.java` | Modify | JWT filter chain, CSRF off, BCrypt(12), filter registration off |
| `security/AuthCookie.java` | Create | cookie name, create/clear/read helpers |
| `security/JwtAuthenticationFilter.java` | Create | cookie validation, reason codes, 401 vs redirect |
| `service/JwtTokenService.java` | Create | issue (subject, jti, iat, exp), parse |
| `model/RevokedToken.java` | Create | jti, revoked_at, expires_at entity |
| `repository/RevokedTokenRepository.java` | Create | existsByJti, deleteByExpiresAtBefore |
| `service/RevocationService.java` | Create | revoke, isRevoked, scheduled purge |
| `service/LoginAttemptService.java` | Create | 5-failures/15-min brute-force block |
| `controller/LoginController.java` | Modify | POST /login issues cookie; GET /logout revokes |
| `model/UserAccount.java` | Modify | + displayName, createdAt, updatedAt |
| `model/LearningProfile.java` | Create | stage, unlockedStages, programStartDate, practiceMinutes |
| `repository/LearningProfileRepository.java` | Create | findByUser |
| `service/LearningProfileService.java` | Create | initializeFor(user) |
| `service/UserAccountService.java` | Modify | validation, displayName, transactional profile init, profileOf, changePassword |
| `model/ChangePasswordOutcome.java` | Create | record (success, error) |
| `model/ProfileView.java` | Create | record for profile page data |
| `controller/ProfileController.java` | Create | GET /profile, display-name + password POST |
| `controller/SignupController.java` | Modify | displayName param, drop @NotBlank |
| `TypeShiftCoreApplication.java` | Modify | @EnableScheduling |
| `application.properties` | Modify | typeshift.jwt-secret, typeshift.cookie-secure |
| `service/TypeShiftUserDetailsService.java` | Delete | form login superseded |
| `src/test/...` (per-task files) | Create | unit + MockMvc integration tests |

Frontend (`typeshift-frontend/src/main/resources/templates/`): `login.html` (Modify: error text), `signup.html` (Modify: display name field), `profile.html` (Create), `layout.html` (Modify: nav links).

---

### Task 1: Add JWT and test dependencies

**Files:**
- Modify: `typeshift-backend/pom.xml`

**Interfaces:**
- Consumes: parent POM's dependency management (spring-boot-starter-parent 3.5.16 manages the test starter version).
- Produces: JJWT will be importable in Tasks 5–11; test starters available everywhere.

- [ ] **Step 1: Add JJWT and test dependencies to `typeshift-backend/pom.xml`**

Inside the existing `<dependencies>` list (e.g. right after the `hibernate-community-dialects` dependency), add:

```xml
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
      <version>0.12.6</version>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
      <version>0.12.6</version>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
```

- [ ] **Step 2: Verify the build compiles**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS (no output with -q on success).

- [ ] **Step 3: Commit**

```bash
git add typeshift-backend/pom.xml
git commit -m "build: add jjwt and test dependencies to backend"
```

---

### Task 2: Extend UserAccount with displayName and timestamps

**Files:**
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/UserAccount.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/model/UserAccountTest.java` (new — this also boots the test tree)

**Interfaces:**
- Consumes: jakarta.persistence (already used).
- Produces: `UserAccount(String username, String passwordHash, String displayName)` — used by Tasks 4, 10, 11 and by integration test seeding; getters `getDisplayName()`, `getCreatedAt()`, `getUpdatedAt()`, setters `setDisplayName(String)`, `setPasswordHash(String)` (Task 10 password change).

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/model/UserAccountTest.java`:

```java
package hu.elte.ntb.typeshift.model;

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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=UserAccountTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation error — no constructor `(String, String, String)`, no `getDisplayName()`.

- [ ] **Step 3: Rewrite `UserAccount.java` with the new fields and 3-arg constructor**

Replace the whole file with:

```java
package hu.elte.ntb.typeshift.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity holding a TypeShift account: unique username, BCrypt password
 * hash, display name, and audit timestamps.
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

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getDisplayName() {
    return displayName == null ? username : displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = defaultDisplayName(displayName);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
```

Note: `getDisplayName()` resolves null to username at the edge, so blank/absent display names render as the username on the profile page without schema changes. `defaultDisplayName` null is intentional (keeps column null; getter covers rendering).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=UserAccountTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (2 tests). Also confirm existing main sources still compile (mvn test compiles them).

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/UserAccount.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/model/UserAccountTest.java
git commit -m "feat: add display name and timestamps to user account"
```

---

### Task 3: LearningProfile entity, repository, service (Story 0.6 core)

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/LearningProfile.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/repository/LearningProfileRepository.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/LearningProfileService.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/LearningProfileServiceTest.java`

**Interfaces:**
- Consumes: `UserAccount` from Task 2.
- Produces: `LearningProfile` entity (table `learning_profiles`) and `LearningProfileService.initializeFor(UserAccount user)` → `LearningProfile`, used by Task 4. `LearningProfileRepository.findByUser(UserAccount)` used by Task 10.

Initial state per ADR-013: `currentStage = 1`, `unlockedStages = {1}`, `programStartDate = null`, `totalPracticeMinutes = 0`.

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/LearningProfileServiceTest.java`:

```java
package hu.elte.ntb.typeshift.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.elte.ntb.typeshift.model.LearningProfile;
import hu.elte.ntb.typeshift.model.UserAccount;
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=LearningProfileServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation error — classes do not exist.

- [ ] **Step 3: Create `LearningProfile.java`**

```java
package hu.elte.ntb.typeshift.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Learning state of the owner per ADR-013: the current stage, unlocked
 * stages, the program start date, and accumulated practice minutes.
 */
@Entity
@Table(name = "learning_profiles")
public class LearningProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private UserAccount user;

  @Column(nullable = false)
  private int currentStage;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "learning_profile_unlocked_stages",
      joinColumns = @JoinColumn(name = "learning_profile_id"))
  @Column(name = "stage")
  private Set<Integer> unlockedStages;

  @Column
  private Instant programStartDate;

  @Column(nullable = false)
  private int totalPracticeMinutes;

  protected LearningProfile() {}

  /**
   * Creates the initial learning profile per ADR-013: stage 1, only stage 1
   * unlocked, program start date empty, no practice minutes.
   *
   * @param user the owning account
   */
  public LearningProfile(UserAccount user) {
    this.user = user;
    this.currentStage = 1;
    this.unlockedStages = new HashSet<>();
    this.unlockedStages.add(1);
    this.programStartDate = null;
    this.totalPracticeMinutes = 0;
  }

  public Long getId() {
    return id;
  }

  public UserAccount getUser() {
    return user;
  }

  public int getCurrentStage() {
    return currentStage;
  }

  public void setCurrentStage(int currentStage) {
    this.currentStage = currentStage;
  }

  public Set<Integer> getUnlockedStages() {
    return unlockedStages;
  }

  public void setUnlockedStages(Set<Integer> unlockedStages) {
    this.unlockedStages = unlockedStages;
  }

  /** Null until the first sub-exam attempt sets it (later epic). */
  public Instant getProgramStartDate() {
    return programStartDate;
  }

  public void setProgramStartDate(Instant programStartDate) {
    this.programStartDate = programStartDate;
  }

  public int getTotalPracticeMinutes() {
    return totalPracticeMinutes;
  }

  public void setTotalPracticeMinutes(int totalPracticeMinutes) {
    this.totalPracticeMinutes = totalPracticeMinutes;
  }
}
```

- [ ] **Step 4: Create `LearningProfileRepository.java`**

```java
package hu.elte.ntb.typeshift.repository;

import hu.elte.ntb.typeshift.model.LearningProfile;
import hu.elte.ntb.typeshift.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to the {@code learning_profiles} table.
 */
public interface LearningProfileRepository extends JpaRepository<LearningProfile, Long> {

  /**
   * Finds the learning profile of an account.
   *
   * @param user the owning account
   * @return the matching profile, if any
   */
  Optional<LearningProfile> findByUser(UserAccount user);
}
```

- [ ] **Step 5: Create `LearningProfileService.java`**

```java
package hu.elte.ntb.typeshift.service;

import hu.elte.ntb.typeshift.model.LearningProfile;
import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.LearningProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the initial learning profile of a newly registered owner
 * (Story 0.6).
 */
@Service
public class LearningProfileService {

  private final LearningProfileRepository repository;

  public LearningProfileService(LearningProfileRepository repository) {
    this.repository = repository;
  }

  /**
   * Persists the starting learning profile of the account.
   *
   * @param user the owning account
   * @return the saved profile
   */
  @Transactional
  public LearningProfile initializeFor(UserAccount user) {
    return repository.save(new LearningProfile(user));
  }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=LearningProfileServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (1 test).

- [ ] **Step 7: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/LearningProfile.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/repository/LearningProfileRepository.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/LearningProfileService.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/LearningProfileServiceTest.java
git commit -m "feat: add learning profile entity and initialiser"
```

---

### Task 4: Registration creates learning profile atomically (Story 0.6 wiring + Stories 0.1 validation)

**Files:**
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/UserAccountService.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/UserAccountServiceTest.java`

**Interfaces:**
- Consumes: `LearningProfileService.initializeFor(UserAccount)` from Task 3; `SignupOutcome` (existing).
- Produces: `UserAccountService.createAccount(String username, String password, String displayName)` → `SignupOutcome`, of which `created()` carries the "user + learning profile saved in one transaction" contract. Used by Task 11's controller. Outcome error messages:
  - duplicate: `Username is already taken.`
  - bad username: `Username must be 3 to 30 characters and contain only letters, digits, or underscores.`
  - short password: `Password must be at least 8 characters.`

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/UserAccountServiceTest.java`:

```java
package hu.elte.ntb.typeshift.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.elte.ntb.typeshift.model.SignupOutcome;
import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
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
```

(`badUsernameIsRefused` covers the rule once; the regex test in Task 11's list below needs no duplicate — this is the shared check.)

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=UserAccountServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation error — `createAccount` takes 2 args, no `LearningProfileService` reference.

- [ ] **Step 3: Rewrite `UserAccountService.java`**

Replace the whole file with:

```java
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
```

Task 9 and Task 10 extend this same class with `updateDisplayName`, `changePassword`, and `profileOf` — signatures declared in those tasks. (BCrypt cost 12 is set once in Task 5's `SecurityConfig` bean; this service only uses the injected encoder.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f typeshift-backend/pom.xml test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (all existing test classes still pass; `UserAccountService` has a new constructor arg but its only caller, `SignupController`, uses Spring injection so nothing else touches it yet).

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/UserAccountService.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/UserAccountServiceTest.java
git commit -m "feat: validate registration and init learning profile in same transaction"
```

---

### Task 5: AuthProperties + JwtTokenService (Story 0.2 core)

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/AuthProperties.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/JwtTokenService.java`
- Modify: `typeshift-backend/src/main/resources/application.properties`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/JwtTokenServiceTest.java`

**Interfaces:**
- Consumes: `AuthProperties` (`typeshift.jwt-secret` via `JWT_SECRET` env, `typeshift.cookie-secure`).
- Produces:
  - `JwtTokenService.issue(String username)` → `String`; `JwtTokenService.issue(String username, java.time.Duration lifetime)` → `String` (test overload for expired tokens)
  - `JwtTokenService.parse(String token)` → `io.jsonwebtoken.Claims`; throws `JwtException` (specifically `ExpiredJwtException`, `MalformedJwtException`, or other `JwtException`) or `IllegalArgumentException`
  - `AuthProperties.jwtSecret()`, `AuthProperties.cookieSecure()`
- Properties added: `typeshift.jwt-secret=${JWT_SECRET:}` and `typeshift.cookie-secure=${COOKIE_SECURE:false}`.

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/JwtTokenServiceTest.java`:

```java
package hu.elte.ntb.typeshift.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hu.elte.ntb.typeshift.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

  private static final String SECRET = "test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms";

  private final JwtTokenService tokens =
      new JwtTokenService(new AuthProperties(SECRET, false));

  @Test
  void roundTripKeepsSubjectJtiAndTimestamps() {
    String token = tokens.issue("owner");
    Claims claims = tokens.parse(token);
    assertEquals("owner", claims.getSubject());
    assertTrue(claims.getId() != null && !claims.getId().isBlank());
    assertTrue(claims.getIssuedAt() != null);
    assertTrue(claims.getExpiration().after(claims.getIssuedAt()));
  }

  @Test
  void lifetimeIsThirtyDays() {
    Claims claims = tokens.parse(tokens.issue("owner"));
    Duration lifetime =
        Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
    assertEquals(Duration.ofDays(30).toMinutes(), lifetime.toMinutes());
  }

  @Test
  void expiredTokenIsRejected() {
    String token = tokens.issue("owner", Duration.ofSeconds(-60));
    assertThrows(ExpiredJwtException.class, () -> tokens.parse(token));
  }

  @Test
  void tamperedTokenIsRejected() {
    String token = tokens.issue("owner");
    String tampered = token.substring(0, token.length() - 2) + "xx";
    assertThrows(JwtException.class, () -> tokens.parse(tampered));
  }

  @Test
  void unsignedTokenIsRejected() {
    String unsigned = Jwts.builder().subject("owner").compact();
    assertThrows(JwtException.class, () -> tokens.parse(unsigned));
  }

  @Test
  void shortSecretFailsFastAtStartup() {
    assertThrows(
        IllegalStateException.class,
        () -> new JwtTokenService(new AuthProperties("too-short", false)));
  }

  @Test
  void missingSecretFailsFastAtStartup() {
    assertThrows(
        IllegalStateException.class, () -> new JwtTokenService(new AuthProperties("", false)));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=JwtTokenServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation error — classes missing.

- [ ] **Step 3: Create `AuthProperties.java`**

```java
package hu.elte.ntb.typeshift.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Authentication wiring loaded from configuration: the JWT signing secret
 * (injected via the {@code JWT_SECRET} environment variable, never
 * committed) and cookie flags that differ between dev and prod.
 *
 * @param jwtSecret HS256 signing secret, at least 32 characters
 * @param cookieSecure whether session cookies set the Secure flag (prod only)
 */
@ConfigurationProperties(prefix = "typeshift")
public record AuthProperties(String jwtSecret, boolean cookieSecure) {}
```

- [ ] **Step 4: Create `JwtTokenService.java`**

```java
package hu.elte.ntb.typeshift.service;

import hu.elte.ntb.typeshift.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies the signed JWT session tokens of ADR-001: fixed HS256,
 * 30-day lifetime, subject and token id required.
 */
@Service
public class JwtTokenService {

  /** Session length locked by ADR-001. */
  static final Duration TOKEN_LIFETIME = Duration.ofDays(30);

  /** HMAC-SHA256 needs at least 256 bits of key material. */
  private static final int MIN_SECRET_BYTES = 32;

  private final SecretKey key;

  /**
   * Builds the signing key from the configured secret and refuses to start
   * without adequate key material.
   *
   * @param properties the auth properties holding the secret
   */
  public JwtTokenService(AuthProperties properties) {
    byte[] secret =
        properties.jwtSecret() == null
            ? new byte[0]
            : properties.jwtSecret().getBytes(StandardCharsets.UTF_8);
    if (secret.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "JWT secret missing or shorter than " + MIN_SECRET_BYTES + " bytes; set JWT_SECRET.");
    }
    this.key = Keys.hmacShaKeyFor(secret);
  }

  /**
   * Issues a token with the standard 30-day lifetime.
   *
   * @param username the token subject
   * @return the signed compact token string
   */
  public String issue(String username) {
    return issue(username, TOKEN_LIFETIME);
  }

  /**
   * Issues a token with an explicit lifetime; tests use negative durations to
   * create already-expired tokens. Production callers use the 30-day overload.
   *
   * @param username the token subject
   * @param lifetime how long the token stays valid
   * @return the signed compact token string
   */
  public String issue(String username, Duration lifetime) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(username)
        .id(UUID.randomUUID().toString())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(lifetime)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Verifies the signature with the fixed HS256 key and the validity window;
   * unsigned ("none") or tampered tokens never parse.
   *
   * @param token the presented compact token
   * @return the parsed claims
   * @throws io.jsonwebtoken.ExpiredJwtException when the token is expired
   * @throws io.jsonwebtoken.JwtException on any signature, format, or algorithm problem
   */
  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
```

- [ ] **Step 5: Wire the properties in `application.properties`**

Append to `typeshift-backend/src/main/resources/application.properties`:

```properties
typeshift.jwt-secret=${JWT_SECRET:}
typeshift.cookie-secure=${COOKIE_SECURE:false}
```

Note: dev machines must export `JWT_SECRET` (any string ≥ 32 bytes) or the app fails fast at startup; on the production box also set `COOKIE_SECURE=true`.

- [ ] **Step 6: Run tests to verify they pass**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=JwtTokenServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (7 tests).

- [ ] **Step 7: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/AuthProperties.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/JwtTokenService.java typeshift-backend/src/main/resources/application.properties typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/JwtTokenServiceTest.java
git commit -m "feat: add jwt token service with env-based signing secret"
```

---

### Task 6: RevokedToken persistence, service, and daily purge (Story 0.4 foundation)

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/RevokedToken.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/repository/RevokedTokenRepository.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/RevocationService.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/TypeShiftCoreApplication.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/RevocationServiceTest.java`

**Interfaces:**
- Consumes: nothing from JWT yet (pure jti strings).
- Produces (used by Tasks 7, 8):
  - `RevocationService.revoke(String jti, Instant expiresAt)` — idempotent insert
  - `RevocationService.isRevoked(String jti)` → `boolean` (null-safe)
  - `RevocationService.purgeExpired()` — scheduled daily
- `RevokedToken` entity: table `revoked_token`, columns `id`, `jti` (unique, not null), `revokedAt`, `expiresAt`.
- Main application class gets `@EnableScheduling`.

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/RevocationServiceTest.java`:

```java
package hu.elte.ntb.typeshift.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hu.elte.ntb.typeshift.model.RevokedToken;
import hu.elte.ntb.typeshift.repository.RevokedTokenRepository;
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=RevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation error — classes missing.

- [ ] **Step 3: Create `RevokedToken.java`**

```java
package hu.elte.ntb.typeshift.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Server-side blocklist entry of ADR-001: a logged-out token id that must be
 * rejected until its original expiry date.
 */
@Entity
@Table(name = "revoked_token")
public class RevokedToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String jti;

  @Column(nullable = false)
  private Instant revokedAt;

  @Column(nullable = false)
  private Instant expiresAt;

  protected RevokedToken() {}

  /**
   * Creates a blocklist entry.
   *
   * @param jti the id claim of the revoked token
   * @param expiresAt the original expiry of the revoked token
   */
  public RevokedToken(String jti, Instant expiresAt) {
    this.revokedAt = Instant.now();
    this.jti = jti;
    this.expiresAt = expiresAt;
  }

  public Long getId() {
    return id;
  }

  public String getJti() {
    return jti;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }
}
```

- [ ] **Step 4: Create `RevokedTokenRepository.java`**

```java
package hu.elte.ntb.typeshift.repository;

import hu.elte.ntb.typeshift.model.RevokedToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data access to the {@code revoked_token} blocklist table.
 */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

  /**
   * Cheap blocklist probe for the token id claim.
   *
   * @param jti the token id to test
   * @return true when the id has been revoked
   */
  boolean existsByJti(String jti);

  /**
   * Bulk delete of blocklist rows whose original token expiry has passed.
   *
   * @param cutoff the instant before which rows are deleted
   * @return the number of removed rows
   */
  long deleteByExpiresAtBefore(Instant cutoff);
}
```

- [ ] **Step 5: Create `RevocationService.java`**

```java
package hu.elte.ntb.typeshift.service;

import hu.elte.ntb.typeshift.model.RevokedToken;
import hu.elte.ntb.typeshift.repository.RevokedTokenRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Maintains the server-side token blocklist of ADR-001: idempotent insert on
 * logout and a daily purge of entries whose original token has expired.
 */
@Service
public class RevocationService {

  private static final Logger LOG = LoggerFactory.getLogger(RevocationService.class);

  private final RevokedTokenRepository repository;

  public RevocationService(RevokedTokenRepository repository) {
    this.repository = repository;
  }

  /**
   * Reports whether a token id has been revoked; a missing jti claim is never
   * considered revoked.
   *
   * @param jti the token id claim, may be null
   * @return true when the id is on the blocklist
   */
  @Transactional
  public boolean isRevoked(String jti) {
    return jti != null && repository.existsByJti(jti);
  }

  /**
   * Inserts the blocklist row for a jti when not present; logout called twice
   * stays idempotent.
   *
   * @param jti the token id claim, may be null
   * @param expiresAt the original expiry of the revoked token
   */
  @Transactional
  public void revoke(String jti, Instant expiresAt) {
    if (jti == null || repository.existsByJti(jti)) {
      return;
    }
    repository.save(new RevokedToken(jti, expiresAt));
  }

  /**
   * Daily cleanup: removes blocklist rows whose original token expiry has
   * passed, keeping the table bounded per ADR-001.
   */
  @Scheduled(cron = "0 0 3 * * *")
  public void purgeExpired() {
    long removed = repository.deleteByExpiresAtBefore(Instant.now());
    LOG.info("Purged {} expired revoked tokens", removed);
  }
}
```

- [ ] **Step 6: Enable scheduling on the application class**

Modify `TypeShiftCoreApplication.java` (add imports `org.springframework.boot.autoconfigure.SpringBootApplication` already present, plus `org.springframework.scheduling.annotation.EnableScheduling`):

```java
@SpringBootApplication
@EnableScheduling
public class TypeShiftCoreApplication {
  // keep existing content unchanged
}
```

(The implementer should open the file and only add the annotation + import — do not touch the rest.)

- [ ] **Step 7: Run tests to verify they pass**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=RevocationServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/RevokedToken.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/repository/RevokedTokenRepository.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/RevocationService.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/TypeShiftCoreApplication.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/RevocationServiceTest.java
git commit -m "feat: add token revocation blocklist with daily purge"
```

---

### Task 7: LoginAttemptService — brute-force block (Story 0.2)

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/LoginAttemptService.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/LoginAttemptServiceTest.java`

**Interfaces:**
- Consumes: nothing.
- Produces (used by Task 8's `LoginController`):
  - `boolean isBlocked(String username)`
  - `void recordFailure(String username)`
  - `void recordSuccess(String username)`
- Behavior: 5 consecutive failures per username → `isBlocked` true for 15 minutes from the 5th failure; a success clears the counter; failure after an elapsed block restarts the count at 1.

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/LoginAttemptServiceTest.java`:

```java
package hu.elte.ntb.typeshift.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

  @Test
  void fourFailuresDoNotBlock() {
    LoginAttemptService service = new LoginAttemptService();
    for (int i = 0; i < 4; i++) {
      service.recordFailure("owner");
    }
    assertFalse(service.isBlocked("owner"));
  }

  @Test
  void fiveFailuresBlockForFifteenMinutes() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-29T12:00:00Z"));
    Supplier<Instant> clock = now::get;
    LoginAttemptService service = new LoginAttemptService(clock);
    for (int i = 0; i < 5; i++) {
      service.recordFailure("owner");
    }
    assertTrue(service.isBlocked("owner"));
    now.set(now.get().plus(Duration.ofMinutes(14)));
    assertTrue(service.isBlocked("owner"), "block lasts through the window");
    now.set(now.get().plus(Duration.ofMinutes(1)).plus(Duration.ofSeconds(1)));
    assertFalse(service.isBlocked("owner"), "block lifts after 15 minutes");
  }

  @Test
  void successResetsConsecutiveFailures() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.now());
    LoginAttemptService service = new LoginAttemptService(now::get);
    for (int i = 0; i < 4; i++) {
      service.recordFailure("owner");
    }
    service.recordSuccess("owner");
    assertFalse(service.isBlocked("owner"));
    for (int i = 0; i < 4; i++) {
      service.recordFailure("owner");
    }
    assertFalse(service.isBlocked("owner"), "counter restarted after success");
  }

  @Test
  void failureAfterElapsedBlockCountsFromOne() {
    AtomicReference<Instant> now = new AtomicReference<>(Instant.now());
    LoginAttemptService service = new LoginAttemptService(now::get);
    for (int i = 0; i < 5; i++) {
      service.recordFailure("owner");
    }
    now.set(now.get().plus(Duration.ofMinutes(16)));
    assertFalse(service.isBlocked("owner"));
    now.set(now.get().plus(Duration.ofMinutes(16)));
    service.recordFailure("owner");
    assertFalse(service.isBlocked("owner"), "post-block failure is a fresh count of 1");
  }

  @Test
  void failuresAreTrackedPerUsername() {
    LoginAttemptService service = new LoginAttemptService();
    for (int i = 0; i < 5; i++) {
      service.recordFailure("owner");
    }
    assertFalse(service.isBlocked("other"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=LoginAttemptServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation error — class missing.

- [ ] **Step 3: Create `LoginAttemptService.java`**

```java
package hu.elte.ntb.typeshift.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * In-memory brute-force protection (Story 0.2): five consecutive failures per
 * username block logins for fifteen minutes; successes reset the counter.
 *
 * ponytail: in-memory map, restart clears tracking state; move it to a table
 * only if attack recovery across restarts ever matters.
 */
@Service
public class LoginAttemptService {

  static final int MAX_FAILURES = 5;
  static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

  private record Attempt(int failures, Instant blockedUntil) {}

  private final Map<String, Attempt> attemptsByUser = new ConcurrentHashMap<>();
  private final Supplier<Instant> clock;

  public LoginAttemptService() {
    this(Instant::now);
  }

  /**
   * Test seam: inject a controllable clock.
   *
   * @param clock the instant supplier
   */
  LoginAttemptService(Supplier<Instant> clock) {
    this.clock = clock;
  }

  /**
   * Reports whether the username is currently inside a block window.
   *
   * @param username the attempted login name
   * @return true while blocked
   */
  public boolean isBlocked(String username) {
    Attempt attempt = attemptsByUser.get(username);
    return attempt != null
        && attempt.blockedUntil() != null
        && clock.get().isBefore(attempt.blockedUntil());
  }

  /**
   * Records one failed login; the fifth consecutive failure arms the block
   * window; a failure after an elapsed window restarts the count at one.
   *
   * @param username the attempted login name
   */
  public void recordFailure(String username) {
    attemptsByUser.compute(
        username,
        (user, attempt) -> {
          boolean stale =
              attempt != null
                  && attempt.blockedUntil() != null
                  && clock.get().isAfter(attempt.blockedUntil());
          if (stale) {
            attempt = null;
          }
          int failures = attempt == null ? 1 : attempt.failures() + 1;
          Instant blockedUntil =
              failures >= MAX_FAILURES ? clock.get().plus(BLOCK_DURATION) : null;
          return new Attempt(failures, blockedUntil);
        });
  }

  /**
   * Clears the failure counter after a successful login.
   *
   * @param username the login name that succeeded
   */
  public void recordSuccess(String username) {
    attemptsByUser.remove(username);
  }
}
```

Line length note: if `boolean stale = ...` exceeds the 100-column Checkstyle limit, wrap the expression exactly as shown above.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=LoginAttemptServiceTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/LoginAttemptService.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/LoginAttemptServiceTest.java
git commit -m "feat: add login brute-force protection"
```

---

### Task 8: AuthCookie + JWT filter + SecurityConfig rewrite (Story 0.3)

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/security/AuthCookie.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/security/JwtAuthenticationFilter.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/config/SecurityConfig.java` (rewrite)
- Delete: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/TypeShiftUserDetailsService.java`
- Create (test): `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/AuthProbeController.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/security/JwtAuthenticationFilterTest.java`

**Interfaces:**
- Consumes: `JwtTokenService.parse/issue` (Task 5), `RevocationService.isRevoked` (Task 6), `AuthProperties.cookieSecure` (read in Task 9's controller, not here).
- Produces:
  - `AuthCookie.NAME = "typeshift_token"`; `ResponseCookie create(String token, boolean secure)`; `ResponseCookie clear()`; `String read(HttpServletRequest)` → cookie value or null.
  - `JwtAuthenticationFilter` — bean `jwtAuthenticationFilter(JwtTokenService, RevocationService)` in SecurityConfig.
  - Security context: `UsernamePasswordAuthenticationToken(subject, null, List.of())` → `principal` is the **username string** (`Principal.getName()` works for controllers).
- Reason codes: `MISSING`, `MALFORMED`, `INVALID`, `EXPIRED`, `REVOKED`.
- Anonymous paths (must be identical in `SecurityConfig` permitAll and filter `shouldNotFilter` — keep the two lists in sync): `/login`, `/signup`, `/logout`, `/css/**`, `/js/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/api/health`, `/favicon.ico`. Note `/` and all other routes become **protected** (Story 0.3).

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/AuthProbeController.java` (test sources only; component-scanned because it shares the app base package):

```java
package hu.elte.ntb.typeshift;

import java.security.Principal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal authenticated API probe used by the auth integration tests. */
@RestController
public class AuthProbeController {

  @GetMapping("/api/whoami")
  public Map<String, String> whoami(Principal principal) {
    return Map.of("username", principal.getName());
  }
}
```

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/security/JwtAuthenticationFilterTest.java`:

```java
package hu.elte.ntb.typeshift.security;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.service.JwtTokenService;
import hu.elte.ntb.typeshift.service.RevocationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite::memory:",
      "spring.datasource.hikari.maximum-pool-size=1",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "typeshift.jwt-secret=test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms",
      "typeshift.cookie-secure=false"
    })
class JwtAuthenticationFilterTest {

  private static final String OWNER = "owner";

  @Autowired MockMvc mvc;
  @Autowired JwtTokenService tokens;
  @Autowired RevocationService revocations;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder encoder;

  @BeforeEach
  void seed() {
    if (!users.existsByUsername(OWNER)) {
      users.save(new UserAccount(OWNER, encoder.encode("password123"), OWNER));
    }
  }

  @Test
  void missingCookieRedirectsPageToLogin() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login?auth=MISSING"));
  }

  @Test
  void missingCookieReturnsUnauthorizedForApi() throws Exception {
    mvc.perform(get("/api/whoami"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("MISSING")));
  }

  @Test
  void malformedCookieIsRejected() throws Exception {
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, "not-a-jwt")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("MALFORMED")));
  }

  @Test
  void expiredCookieIsRejected() throws Exception {
    String token = tokens.issue(OWNER, Duration.ofSeconds(-60));
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("EXPIRED")));
  }

  @Test
  void revokedCookieIsRejected() throws Exception {
    String token = tokens.issue(OWNER);
    Claims claims = tokens.parse(token);
    revocations.revoke(claims.getId(), claims.getExpiration().toInstant());
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error", is("REVOKED")));
  }

  @Test
  void validCookieGrantsApiAccessWithUsernamePrincipal() throws Exception {
    String token = tokens.issue(OWNER);
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username", is(OWNER)));
  }

  @Test
  void validCookieGrantsPageAccess() throws Exception {
    String token = tokens.issue(OWNER);
    // 404 proves the request passed the filter (an unauthenticated request
    // would redirect/401 instead); Task 10 adds /profile and flips this
    // expectation to isOk().
    mvc.perform(get("/profile").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isNotFound());
  }
}
```

(`validCookieGrantsPageAccess` pins `/profile`, which Task 10 creates: expecting 404 proves the request passed the filter — an unauthenticated request would have been redirected or 401'd, never "no mapping". Task 10 flips the expectation to `isOk()`.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=JwtAuthenticationFilterTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation error (`AuthCookie`, filter missing). Currently the stock SecurityConfig would also fail `missingCookie...` expectations (old form-login redirects to `/login` and `/` is permitAll → these tests guard the coming rewrite).

- [ ] **Step 3: Create `security/AuthCookie.java`**

```java
package hu.elte.ntb.typeshift.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.ResponseCookie;

/**
 * The ADR-001 session cookie: httpOnly, SameSite Lax, Secure in prod, thirty
 * day lifetime; not readable from JavaScript and never placed in URLs.
 */
public final class AuthCookie {

  /** Name of the session cookie carrying the signed JWT. */
  public static final String NAME = "typeshift_token";

  private static final Duration MAX_AGE = Duration.ofDays(30);

  private AuthCookie() {}

  /**
   * Builds a fresh session cookie.
   *
   * @param token the signed compact JWT
   * @param secure whether the Secure flag must be set (prod)
   * @return the cookie value to attach to the response
   */
  public static ResponseCookie create(String token, boolean secure) {
    return ResponseCookie.from(NAME, token)
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .path("/")
        .maxAge(MAX_AGE)
        .build();
  }

  /**
   * Builds the expired cookie that clears the session from the browser.
   *
   * @return the clearing cookie
   */
  public static ResponseCookie clear() {
    return ResponseCookie.from(NAME, "")
        .httpOnly(true)
        .sameSite("Lax")
        .path("/")
        .maxAge(0)
        .build();
  }

  /**
   * Reads the session cookie from the request, if present.
   *
   * @param request the incoming request
   * @return the raw token string, or null when no cookie is present
   */
  public static String read(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (NAME.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
```

- [ ] **Step 4: Create `security/JwtAuthenticationFilter.java`**

```java
package hu.elte.ntb.typeshift.security;

import hu.elte.ntb.typeshift.service.JwtTokenService;
import hu.elte.ntb.typeshift.service.RevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the JWT session cookie on every request outside the anonymous set
 * (Story 0.3). Missing, malformed, expired, or revoked tokens are rejected
 * with a reason code: API requests get a 401 JSON body, page requests a
 * redirect to the login view. The raw token is never logged.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  /** Mirror of SecurityConfig#permitAll — keep the two lists in sync. */
  static final List<String> ANONYMOUS_PREFIXES =
      List.of(
          "/login",
          "/signup",
          "/logout",
          "/css/",
          "/js/",
          "/v3/api-docs",
          "/swagger-ui",
          "/swagger-ui.html",
          "/api/health",
          "/favicon.ico");

  private final JwtTokenService tokens;
  private final RevocationService revocations;

  /**
   * Creates the filter.
   *
   * @param tokens the JWT verifier
   * @param revocations the blocklist probe
   */
  public JwtAuthenticationFilter(JwtTokenService tokens, RevocationService revocations) {
    this.tokens = tokens;
    this.revocations = revocations;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return ANONYMOUS_PREFIXES.stream().anyMatch(path::startsWith);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String token = AuthCookie.read(request);
    if (token == null) {
      reject(request, response, "MISSING");
      return;
    }
    Claims claims;
    try {
      claims = tokens.parse(token);
    } catch (ExpiredJwtException e) {
      reject(request, response, "EXPIRED");
      return;
    } catch (MalformedJwtException e) {
      reject(request, response, "MALFORMED");
      return;
    } catch (JwtException | IllegalArgumentException e) {
      reject(request, response, "INVALID");
      return;
    }
    if (revocations.isRevoked(claims.getId())) {
      reject(request, response, "REVOKED");
      return;
    }
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, List.of()));
    chain.doFilter(request, response);
  }

  /**
   * Rejects the request with the reason code: 401 JSON for API paths, a
   * redirect to the login view for everything else. Logs the reason only.
   */
  private void reject(
      HttpServletRequest request, HttpServletResponse response, String reason)
      throws IOException {
    LOG.warn(
        "Auth rejected: {} {} reason={}",
        request.getMethod(),
        request.getRequestURI(),
        reason);
    SecurityContextHolder.clearContext();
    if (request.getRequestURI().startsWith("/api/")) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\": \"" + reason + "\"}");
      return;
    }
    response.sendRedirect("/login?auth=" + reason);
  }
}
```

- [ ] **Step 5: Rewrite `config/SecurityConfig.java`**

Replace the whole file with:

```java
package hu.elte.ntb.typeshift.config;

import hu.elte.ntb.typeshift.security.JwtAuthenticationFilter;
import hu.elte.ntb.typeshift.service.JwtTokenService;
import hu.elte.ntb.typeshift.service.RevocationService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * JWT cookie security for TypeShift (ADR-001, Story 0.3): the filter validates
 * the auth session cookie before the authorisation rules; login, signup,
 * logout, static assets, healthcare, and docs stay anonymous. CSRF is off
 * because the session lives in a SameSite=Lax httpOnly cookie of a
 * single-owner tool and templates then need no CSRF plumbing.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Wires the JWT filter ahead of the authorisation checks; signup, login,
   * logout, static assets, healthcare, and docs remain anonymous; everything
   * else needs the validated token context.
   *
   * @param http the security builder
   * @param authFilter the token validation filter
   * @return the configured filter chain
   * @throws Exception when the chain cannot be built
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter authFilter)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/login",
                        "/signup",
                        "/logout",
                        "/css/**",
                        "/js/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * Keeps the JWT filter out of the container-level filter registration so it
   * runs exactly once, inside the security chain.
   *
   * @param authFilter the token validation filter
   * @return the disabled registration wrapper
   */
  @Bean
  public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
      JwtAuthenticationFilter authFilter) {
    FilterRegistrationBean<JwtAuthenticationFilter> registration =
        new FilterRegistrationBean<>(authFilter);
    registration.setEnabled(false);
    return registration;
  }

  /**
   * Registers the token validation filter.
   *
   * @param tokens the JWT verifier
   * @param revocations the blocklist probe
   * @return the configured filter
   */
  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtTokenService tokens, RevocationService revocations) {
    return new JwtAuthenticationFilter(tokens, revocations);
  }

  /**
   * Provides the BCrypt password encoder with cost factor 12 or higher.
   *
   * @return the shared BCrypt encoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
```

- [ ] **Step 6: Delete the UserDetailsService (form-login leftover)**

```bash
rm typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/TypeShiftUserDetailsService.java
```

Then grep for `TypeShiftUserDetailsService` across `typeshift-backend/src` — expected: no references remain (it was only used implicitly by form login).

- [ ] **Step 7: Run filter tests to verify they pass**

Run: `mvn -f typeshift-backend/pom.xml test -Dsurefire.failIfNoSpecifiedTests=false -Dtest=JwtAuthenticationFilterTest`
Expected: PASS (7 tests).

- [ ] **Step 8: Commit**

```bash
git add -A typeshift-backend/src/main typeshift-backend/src/test
git commit -m "feat: enforce jwt cookie validation on protected routes"
```

---

### Task 9: Login and logout controllers (Stories 0.2 + 0.4)

**Files:**
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/controller/LoginController.java` (rewrite)
- Modify: `typeshift-frontend/src/main/resources/templates/login.html`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/controller/LoginControllerTest.java`

**Interfaces:**
- Consumes: `JwtTokenService.issue(String)` (Task 5), `RevocationService.revoke(String, Instant)` (Task 6), `LoginAttemptService` (Task 7), `AuthCookie.create/clear` (Task 8), `AuthProperties.cookieSecure()` (Task 5), `PasswordEncoder` bean (Task 8's SecurityConfig).
- Produces:
  - `POST /login` (username, password) → success: `Set-Cookie: typeshift_token=` JWT, 30-day, redirect `/`; failure: layout re-render, model `error=true`, generic message.
  - `GET /login?auth=<reason>` → error banner shown.
  - `GET /logout` → revokes jti when token present/parsable (also handles `ExpiredJwtException` by revoking with past expiry), clears cookie, always success, redirect `/login`.
- Template contract: `error` boolean + `errorText` string (nullable) in the model; login fragment renders `<p th:if="${error}" th:text="${errorText}">`.

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/controller/LoginControllerTest.java`:

```java
package hu.elte.ntb.typeshift.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.service.JwtTokenService;
import hu.elte.ntb.typeshift.service.RevocationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite::memory:",
      "spring.datasource.hikari.maximum-pool-size=1",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "typeshift.jwt-secret=test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms",
      "typeshift.cookie-secure=false"
    })
class LoginControllerTest {

  private static final String OWNER = "owner";
  private static final String PASSWORD = "password123";

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired JwtTokenService tokens;
  @Autowired RevocationService revocations;

  @BeforeEach
  void seed() {
    if (!users.existsByUsername(OWNER)) {
      users.save(new UserAccount(OWNER, encoder.encode(PASSWORD), OWNER));
    }
  }

  @Test
  void failedLoginShowsGenericMessage() throws Exception {
    mvc.perform(post("/login").param("username", OWNER).param("password", "wrong-pass"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Invalid username or password.")));
  }

  @Test
  void failedLoginShowsSameMessageForUnknownUser() throws Exception {
    mvc.perform(post("/login").param("username", "ghost").param("password", "whatever1"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Invalid username or password.")));
  }

  @Test
  void successfulLoginSetsSessionCookieAndRedirectsHome() throws Exception {
    mvc.perform(post("/login").param("username", OWNER).param("password", PASSWORD))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"))
        .andExpect(cookie().exists(AuthCookie.NAME))
        .andExpect(cookie().httpOnly(true, AuthCookie.NAME))
        .andExpect(cookie().secure(false, AuthCookie.NAME))
        .andExpect(cookie().maxAge(2592000, AuthCookie.NAME));
  }

  @Test
  void blockedUserGetsGenericMessageEvenWithCorrectPassword() throws Exception {
    String user = "blockme";
    if (!users.existsByUsername(user)) {
      users.save(new UserAccount(user, encoder.encode(PASSWORD), user));
    }
    String wrong = "wrong-pass";
    for (int i = 0; i < 5; i++) {
      mvc.perform(post("/login").param("username", user).param("password", wrong))
          .andExpect(status().isOk());
    }
    mvc.perform(post("/login").param("username", user).param("password", PASSWORD))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Invalid username or password.")));
  }

  @Test
  void logoutRevokesTokenAndClearsCookie() throws Exception {
    String token =
        mvc.perform(post("/login").param("username", OWNER).param("password", PASSWORD))
            .andReturn()
            .getResponse()
            .getCookie(AuthCookie.NAME)
            .getValue();

    mvc.perform(get("/logout").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"))
        .andExpect(cookie().maxAge(0, AuthCookie.NAME));

    // Reuse must fail with 401 REVOKED (Story 0.4 / 0.3 handshake).
    mvc.perform(get("/api/whoami").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logoutWithoutCookieStillSucceeds() throws Exception {
    mvc.perform(get("/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"))
        .andExpect(cookie().maxAge(0, AuthCookie.NAME));
  }

  @Test
  void logoutWithExpiredTokenStillSucceedsAndClearsCookie() throws Exception {
    String token = tokens.issue(OWNER, Duration.ofSeconds(-60));
    Claims claims = tokens.parse(token);
    mvc.perform(get("/logout").cookie(new Cookie(AuthCookie.NAME, token)))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"))
        .andExpect(cookie().maxAge(0, AuthCookie.NAME));
    assertTrue(revocations.isRevoked(claims.getId()), "expired jti got revoked on logout");
  }
}
```

(Why expired logout works: `parse` throws `ExpiredJwtException` carrying the claims — the controller catches it and revokes the jti with its past expiry; the reuse attempt then answers 401 REVOKED, and Story 0.4's "logout of an expired token still returns success" holds.)

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=LoginControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: compilation error (POST /login doesn't exist — 405; logout missing).

- [ ] **Step 3: Rewrite `LoginController.java`**

Replace the whole file with:

```java
package hu.elte.ntb.typeshift.controller;

import hu.elte.ntb.typeshift.config.AuthProperties;
import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.service.JwtTokenService;
import hu.elte.ntb.typeshift.service.LoginAttemptService;
import hu.elte.ntb.typeshift.service.RevocationService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Handles the login and logout flows of ADR-001: POST /login verifies the
 * credentials, throttles repeated failures, and issues the signed JWT in the
 * session cookie; GET /logout revokes the token and clears the cookie; both
 * render through the layout shell.
 */
@Controller
public class LoginController {

  private static final Logger LOG = LoggerFactory.getLogger(LoginController.class);

  private final UserAccountRepository accounts;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService tokens;
  private final RevocationService revocations;
  private final LoginAttemptService loginAttempts;
  private final AuthProperties properties;

  public LoginController(
      UserAccountRepository accounts,
      PasswordEncoder passwordEncoder,
      JwtTokenService tokens,
      RevocationService revocations,
      LoginAttemptService loginAttempts,
      AuthProperties properties) {
    this.accounts = accounts;
    this.passwordEncoder = passwordEncoder;
    this.tokens = tokens;
    this.revocations = revocations;
    this.loginAttempts = loginAttempts;
    this.properties = properties;
  }

  /**
   * Renders the login view; shows the generic failure message when a failed
   * verification or auth redirect flagged the query.
   *
   * @param authFlag reason code appended by the auth filter, if any
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/login")
  public String login(@RequestParam(value = "auth", required = false) String authFlag, Model model) {
    model.addAttribute("title", "TypeShift — Start of Shift");
    model.addAttribute("contentFragment", "login");
    if (authFlag == null) {
      model.addAttribute("error", false);
    } else {
      model.addAttribute("error", true);
      model.addAttribute("errorText", "Session expired or revoked. Please sign in again.");
    }
    return "layout";
  }

  /**
   * Verifies the credentials and issues the session cookie on success;
   * failures (including throttled blocks) show the single generic message of
   * Story 0.2.
   *
   * @param username submitted login name
   * @param password submitted plaintext password
   * @param response the response to attach the cookie to
   * @param model the view model
   * @return redirect to the shell view, or the login view with the error
   */
  @PostMapping("/login")
  public String login(
      @RequestParam String username,
      @RequestParam String password,
      HttpServletResponse response,
      Model model) {
    String submitted = username == null ? "" : username.trim();
    if (loginAttempts.isBlocked(submitted)) {
      LOG.warn("Blocked login attempt, username length={}", submitted.length());
      return renderLogin(model, "Invalid username or password.");
    }
    return accounts
        .findByUsername(submitted)
        .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()))
        .map(account -> admit(response, account))
        .orElseGet(() -> failedLogin(submitted, model));
  }

  /**
   * Ends the session: revokes the presented jti (even for expired tokens) and
   * clears the cookie; always succeeds per Story 0.4.
   *
   * @param token the presented session cookie value, may be absent
   * @param response the response to attach the clearing cookie to
   * @return redirect to the login view
   */
  @GetMapping("/logout")
  public String logout(
      @CookieValue(name = AuthCookie.NAME, required = false) String token,
      HttpServletResponse response) {
    if (token != null) {
      try {
        Claims claims = tokens.parse(token);
        revocations.revoke(claims.getId(), claims.getExpiration().toInstant());
      } catch (ExpiredJwtException e) {
        revocations.revoke(e.getClaims().getId(), e.getClaims().getExpiration().toInstant());
      } catch (JwtException | IllegalArgumentException e) {
        LOG.info("Logout attempted with unusable token");
      }
    }
    response.addHeader(HttpHeaders.SET_COOKIE, AuthCookie.clear().toString());
    return "redirect:/login";
  }

  private String admit(HttpServletResponse response, UserAccount account) {
    loginAttempts.recordSuccess(account.getUsername());
    String token = tokens.issue(account.getUsername());
    response.addHeader(
        HttpHeaders.SET_COOKIE, AuthCookie.create(token, properties.cookieSecure()).toString());
    return "redirect:/";
  }

  private String failedLogin(String submitted, Model model) {
    loginAttempts.recordFailure(submitted);
    return renderLogin(model, "Invalid username or password.");
  }

  private String renderLogin(Model model, String errorText) {
    model.addAttribute("error", true);
    model.addAttribute("errorText", errorText);
    return "layout";
  }
}
```

Checkstyle line-length note: `revocations.revoke(e.getClaims()...)` and the `admit` addHeader call may exceed 100 columns — wrap exactly as shown.

- [ ] **Step 4: Update `login.html` error paragraph**

In `typeshift-frontend/src/main/resources/templates/login.html`, replace:

```html
    <p th:if="${error}" class="error" role="alert">Wrong username or password.</p>
```

with:

```html
    <p th:if="${error}" class="error" role="alert" th:text="${errorText} ?: 'Invalid username or password.'"></p>
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=LoginControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (7 tests). The `validCookieGrantsPageAccess` expectation stays `isNotFound` until Task 10 creates `/profile`.

- [ ] **Step 6: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/controller/LoginController.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/controller/LoginControllerTest.java typeshift-frontend/src/main/resources/templates/login.html typeshift-backend/src/test/java/hu/elte/ntb/typeshift/security/JwtAuthenticationFilterTest.java
git commit -m "feat: issue jwt cookie on login and revoke it on logout"
```

---

### Task 10: Profile view and update (Story 0.5)

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/ProfileView.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/ChangePasswordOutcome.java`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/UserAccountService.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/controller/ProfileController.java`
- Create: `typeshift-frontend/src/main/resources/templates/profile.html`
- Modify: `typeshift-frontend/src/main/resources/templates/layout.html`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/controller/ProfileControllerTest.java`

**Interfaces:**
- Consumes: `UserAccountRepository`, `PasswordEncoder`, `LearningProfileRepository.findByUser(UserAccount)` (Task 3), `AuthCookie` (Task 8), JWT principal = username string.
- Produces:
  - `UserAccountService.profileOf(String username)` → `ProfileView`
  - `UserAccountService.updateDisplayName(String username, String displayName)` (no-arg-void outcome, redirect after)
  - `UserAccountService.changePassword(String username, String currentPassword, String newPassword)` → `ChangePasswordOutcome` with messages: `Current password is incorrect.` / `New password must be at least 8 characters.`
  - `ProfileView(String username, String displayName, String memberSince, int currentStage)` — `memberSince` is `account.createdAt` formatted `yyyy-MM-dd` (UTC)
  - `ChangePasswordOutcome(boolean success, String error)`
  - Routes: `GET /profile`, `POST /profile/display-name`, `POST /profile/password`
  - Template contract: flat model attributes `username`, `displayName`, `memberSince`, `currentStage` (avoids relying on SpEL record accessors)

- [ ] **Step 1: Write the failing test**

Create `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/controller/ProfileControllerTest.java`:

```java
package hu.elte.ntb.typeshift.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.model.UserAccount;
import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import hu.elte.ntb.typeshift.security.AuthCookie;
import hu.elte.ntb.typeshift.service.JwtTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite::memory:",
      "spring.datasource.hikari.maximum-pool-size=1",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "typeshift.jwt-secret=test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms",
      "typeshift.cookie-secure=false"
    })
class ProfileControllerTest {

  private static final String OWNER = "owner";
  private static final String PASSWORD = "password123";

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired JwtTokenService tokens;

  private String cookieValue;

  @BeforeEach
  void seedAndCookie() {
    if (!users.existsByUsername(OWNER)) {
      users.save(new UserAccount(OWNER, encoder.encode(PASSWORD), "Ops Chief"));
    }
    cookieValue = tokens.issue(OWNER);
  }

  private Cookie session() {
    return new Cookie(AuthCookie.NAME, cookieValue);
  }

  @Test
  void profilePageShowsStatusData() throws Exception {
    mvc.perform(get("/profile").cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("owner")))
        .andExpect(content().string(containsString("Member since")))
        .andExpect(content().string(containsString("HU_QWERTZ")));
  }

  @Test
  void displayNameUpdatePersists() throws Exception {
    mvc.perform(post("/profile/display-name").param("displayName", "Chief").cookie(session()))
        .andExpect(status().is3xxRedirection());
    mvc.perform(get("/profile").cookie(session()))
        .andExpect(content().string(containsString("Chief")));
  }

  @Test
  void wrongCurrentPasswordShowsMessage() throws Exception {
    String user = "wrongpass_user";
    if (!users.existsByUsername(user)) {
      users.save(new UserAccount(user, encoder.encode(PASSWORD), user));
    }
    cookieValue = tokens.issue(user);
    mvc.perform(
            post("/profile/password")
                .param("currentPassword", "not-my-pass")
                .param("newPassword", "brand-new-pass")
                .cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Current password is incorrect.")));
  }

  @Test
  void shortNewPasswordShowsMessage() throws Exception {
    String user = "shortpass_user";
    if (!users.existsByUsername(user)) {
      users.save(new UserAccount(user, encoder.encode(PASSWORD), user));
    }
    cookieValue = tokens.issue(user);
    mvc.perform(
            post("/profile/password")
                .param("currentPassword", PASSWORD)
                .param("newPassword", "short")
                .cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("New password must be at least 8 characters.")));
  }

  @Test
  void passwordChangeSucceedsWithCorrectCurrentPassword() throws Exception {
    String user = "changepass_user";
    if (!users.existsByUsername(user)) {
      users.save(new UserAccount(user, encoder.encode(PASSWORD), user));
    }
    cookieValue = tokens.issue(user);
    mvc.perform(
            post("/profile/password")
                .param("currentPassword", PASSWORD)
                .param("newPassword", "brand-new-pass")
                .cookie(session()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Password changed.")));
    // Follow-up login with the new password proves the hash was swapped.
    mvc.perform(post("/login").param("username", user).param("password", "brand-new-pass"))
        .andExpect(status().is3xxRedirection());
  }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -f typeshift-backend/pom.xml test -Dtest=ProfileControllerTest -Dsurefire.failIfNoSpecifiedTests=false`
Expected: FAIL — 404 for `/profile` routes (filter lets the cookie through, Spring has no mapping yet).

- [ ] **Step 3: Create model records**

`typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/ProfileView.java`:

```java
package hu.elte.ntb.typeshift.model;

/**
 * Read-only snapshot of the owner profile for the view (Story 0.5).
 *
 * @param username unique login name
 * @param displayName shown name
 * @param memberSince account creation date formatted yyyy-MM-dd
 * @param currentStage current stage number from the learning profile
 */
public record ProfileView(String username, String displayName, String memberSince, int currentStage) {}
```

`typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/ChangePasswordOutcome.java`:

```java
package hu.elte.ntb.typeshift.model;

/**
 * Result of an owner password change.
 *
 * @param success whether the password was changed
 * @param error message when the change was refused, otherwise null
 */
public record ChangePasswordOutcome(boolean success, String error) {}
```

- [ ] **Step 4: Extend `UserAccountService.java`**

Add imports: `hu.elte.ntb.typeshift.model.ChangePasswordOutcome`, `LearningProfile`, `LearningProfileRepository`, `ProfileView`, `java.time.format.DateTimeFormatter`, `java.time.ZoneOffset`. Add field + constructor arg:

```java
  private static final DateTimeFormatter MEMBER_SINCE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

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
```

Add the three public methods (before `isValidPassword`):

```java
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
```

Also open `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/service/UserAccountServiceTest.java` and add one mock field for the new dependency (otherwise `@InjectMocks` fills the 4-arg constructor with null):

```java
  @Mock LearningProfileRepository learningProfiles;
```

plus the import `hu.elte.ntb.typeshift.repository.LearningProfileRepository`. No other test change is needed.

- [ ] **Step 5: Create `ProfileController.java`**

```java
package hu.elte.ntb.typeshift.controller;

import hu.elte.ntb.typeshift.model.ChangePasswordOutcome;
import hu.elte.ntb.typeshift.model.ProfileView;
import hu.elte.ntb.typeshift.service.UserAccountService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Owner profile (Story 0.5): read-only status data, editable display name,
 * and password change requiring the current password.
 */
@Controller
public class ProfileController {

  private final UserAccountService accounts;

  public ProfileController(UserAccountService accounts) {
    this.accounts = accounts;
  }

  /**
   * Renders the profile view through the layout shell.
   *
   * @param principal the authenticated owner (username)
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/profile")
  public String view(Principal principal, Model model) {
    model.addAttribute("title", "TypeShift — Profile");
    model.addAttribute("contentFragment", "profile");
    addProfileData(principal.getName(), model);
    return "layout";
  }

  /**
   * Saves the display name and returns to the profile view.
   *
   * @param displayName the new display name, optional
   * @param principal the authenticated owner
   * @return redirect back to the profile view
   */
  @PostMapping("/profile/display-name")
  public String updateDisplayName(@RequestParam String displayName, Principal principal) {
    accounts.updateDisplayName(principal.getName(), displayName);
    return "redirect:/profile";
  }

  /**
   * Changes the password after verification; re-renders the profile with the
   * outcome banner on success or refusal.
   *
   * @param currentPassword the submitted current password
   * @param newPassword the replacement password
   * @param principal the authenticated owner
   * @param model the view model
   * @return the layout template name (profile fragment)
   */
  @PostMapping("/profile/password")
  public String changePassword(
      @RequestParam String currentPassword,
      @RequestParam String newPassword,
      Principal principal,
      Model model) {
    ChangePasswordOutcome outcome =
        accounts.changePassword(principal.getName(), currentPassword, newPassword);
    model.addAttribute("title", "TypeShift — Profile");
    model.addAttribute("contentFragment", "profile");
    model.addAttribute(
        "passwordMessage", outcome.success() ? "Password changed." : outcome.error());
    model.addAttribute("passwordError", !outcome.success());
    addProfileData(principal.getName(), model);
    return "layout";
  }

  private void addProfileData(String username, Model model) {
    ProfileView profile = accounts.profileOf(username);
    model.addAttribute("username", profile.username());
    model.addAttribute("displayName", profile.displayName());
    model.addAttribute("memberSince", profile.memberSince());
    model.addAttribute("currentStage", profile.currentStage());
  }
}
```

- [ ] **Step 6: Create `profile.html`**

```html
<!DOCTYPE html>
<html lang="hu" xmlns:th="http://www.thymeleaf.org">
<head>
  <title>TypeShift — Profile</title>
</head>
<body>
  <section th:fragment="content">
    <h1>Employment record</h1>
    <p th:if="${passwordMessage}" th:classappend="${passwordError ? 'error' : 'ok'}" th:text="${passwordMessage}"></p>
    <dl>
      <dt>Username</dt>
      <dd th:text="${username}"></dd>
      <dt>Member since</dt>
      <dd th:text="${memberSince}"></dd>
      <dt>Current stage</dt>
      <dd th:text="${currentStage}"></dd>
      <dt>Keyboard layout</dt>
      <dd>HU_QWERTZ</dd>
      <dt>Interface language</dt>
      <dd>English</dd>
    </dl>

    <h2>Display name</h2>
    <form th:action="@{/profile/display-name}" method="post">
      <label for="displayName">Display name</label>
      <input id="displayName" name="displayName" type="text" th:value="${displayName}">
      <button type="submit">Save</button>
    </form>

    <h2>Change password</h2>
    <form th:action="@{/profile/password}" method="post">
      <label for="currentPassword">Current password</label>
      <input id="currentPassword" name="currentPassword" type="password" autocomplete="current-password" required>
      <label for="newPassword">New password (min 8 characters)</label>
      <input id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" required>
      <button type="submit">Change password</button>
    </form>
  </section>
</body>
</html>
```

- [ ] **Step 7: Add nav links to `layout.html`**

In `typeshift-frontend/src/main/resources/templates/layout.html`, replace the header block:

```html
  <header class="topnav">
    <a class="brand" th:href="@{/}">TypeShift</a>
  </header>
```

with:

```html
  <header class="topnav">
    <a class="brand" th:href="@{/}">TypeShift</a>
    <nav>
      <a th:href="@{/profile}">Profile</a>
      <a th:href="@{/logout}">Logout</a>
    </nav>
  </header>
```

- [ ] **Step 8: Flip the Task 8 page-access expectation**

Open `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/security/JwtAuthenticationFilterTest.java`, change `validCookieGrantsPageAccess` from `status().isNotFound()` to `status().isOk()`, and drop its explanatory comment.

- [ ] **Step 9: Run tests to verify they pass**

Run: `mvn -f typeshift-backend/pom.xml test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS (all suites — Profile changes also prove the filter+auth path end-to-end).

- [ ] **Step 10: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/ProfileView.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/model/ChangePasswordOutcome.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/service/UserAccountService.java typeshift-backend/src/main/java/hu/elte/ntb/typeshift/controller/ProfileController.java typeshift-frontend/src/main/resources/templates/profile.html typeshift-frontend/src/main/resources/templates/layout.html typeshift-backend/src/test/java/hu/elte/ntb/typeshift/controller/ProfileControllerTest.java
git commit -m "feat: add profile view with display name and password change"
```

---

### Task 11: Signup form completion — display name field (Story 0.1 last AC)

**Files:**
- Modify: `typeshift-frontend/src/main/resources/templates/signup.html`
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/controller/SignupController.java`

**Interfaces:**
- Consumes: `UserAccountService.createAccount(username, password, displayName)` — already updated in Task 4.
- Produces: signup POST accepting `displayName`; duplicate-username and validation messages pass through the existing `error` model attr; redirect target becomes `/login` (Story 0.1 "redirects to login on success").

- [ ] **Step 1: Update `signup.html`**

Replace:

```html
    <p th:if="${error}" class="error" role="alert" th:text="${error}"></p>
```

unchanged; then after the password input and before the button, add:

```html
      <label for="displayName">Display name (optional)</label>
      <input id="displayName" name="displayName" type="text" autocomplete="nickname">
```

- [ ] **Step 2: Update `SignupController.java`**

Replace the file with:

```java
package hu.elte.ntb.typeshift.controller;

import hu.elte.ntb.typeshift.model.SignupOutcome;
import hu.elte.ntb.typeshift.service.UserAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Signup route (Story 0.1): creates the account with its learning profile
 * atomically (Story 0.6) and forwards to the login view on success.
 */
@Controller
public class SignupController {

  private final UserAccountService accounts;

  public SignupController(UserAccountService accounts) {
    this.accounts = accounts;
  }

  /**
   * Renders the signup view through the layout shell.
   *
   * @param model the view model
   * @return the layout template name
   */
  @GetMapping("/signup")
  public String signupView(Model model) {
    model.addAttribute("title", "TypeShift — New Employment Record");
    model.addAttribute("contentFragment", "signup");
    return "layout";
  }

  /**
   * Creates the account and forwards to login, or re-renders signup with the
   * refusal message.
   *
   * @param username requested unique login name
   * @param password plaintext password, hashed before storage
   * @param displayName optional profile display name
   * @param model the view model
   * @return redirect to login on success, else the signup view
   */
  @PostMapping("/signup")
  public String signup(
      @RequestParam("username") String username,
      @RequestParam("password") String password,
      @RequestParam(value = "displayName", required = false) String displayName,
      Model model) {
    SignupOutcome outcome = accounts.createAccount(username, password, displayName);
    if (outcome.created()) {
      return "redirect:/login";
    }
    model.addAttribute("title", "TypeShift — New Employment Record");
    model.addAttribute("contentFragment", "signup");
    model.addAttribute("error", outcome.error());
    return "layout";
  }
}
```

(Removed `@Validated`/`@NotBlank` — Story 0.1 validation now lives in `UserAccountService` with exact copy per AC.)

- [ ] **Step 3: Add a controller-level signup test**

Append to `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/controller/` — create `SignupControllerTest.java`:

```java
package hu.elte.ntb.typeshift.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hu.elte.ntb.typeshift.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite::memory:",
      "spring.datasource.hikari.maximum-pool-size=1",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "typeshift.jwt-secret=test-secret-key-at-least-40-bytes-for-hmac-sha256-algorithms",
      "typeshift.cookie-secure=false"
    })
class SignupControllerTest {

  @Autowired MockMvc mvc;
  @Autowired UserAccountRepository users;

  @Test
  void duplicateUsernameShowsValidationMessage() throws Exception {
    if (!users.existsByUsername("taken_user")) {
      mvc.perform(
          post("/signup")
              .param("username", "taken_user")
              .param("password", "long-enough"));
    }
    mvc.perform(
            post("/signup").param("username", "taken_user").param("password", "long-enough"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Username is already taken.")));
  }

  @Test
  void badUsernameShowsValidationMessage() throws Exception {
    mvc.perform(post("/signup").param("username", "bad name!").param("password", "long-enough"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "Username must be 3 to 30 characters and contain only letters,"
                            + " digits, or underscores.")));
  }

  @Test
  void successfulSignupRedirectsToLogin() throws Exception {
    mvc.perform(
            post("/signup")
                .param("username", "fresh_user")
                .param("password", "long-enough")
                .param("displayName", "Fresh"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }
}
```

(Username uniqueness across test classes uses distinct literals; each class gets its own SQLite in-memory DB instance anyway.)

- [ ] **Step 4: Run all tests**

Run: `mvn -f typeshift-backend/pom.xml test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add typeshift-frontend/src/main/resources/templates/signup.html typeshift-backend/src/main/java/hu/elte/ntb/typeshift/controller/SignupController.java typeshift-backend/src/test/java/hu/elte/ntb/typeshift/controller/SignupControllerTest.java
git commit -m "feat: complete signup form with display name and validation copy"
```

---

### Task 12: Full verification, quality gates, and acceptance-criteria sweep

**Files:**
- Modify: none (verification-only task; fix whatever it surfaces)

**Interfaces:**
- Consumes: everything above.
- Produces: verified build + explicit AC coverage table below.

- [ ] **Step 1: Full verify**

Run: `mvn verify`
Expected: BUILD SUCCESS including Checkstyle, PMD, SpotBugs (failOnViolation=true at parent); all tests green. Fix any gate violation — repeat per-task test runs to locate regressions:

Run: `mvn -f typeshift-backend/pom.xml test -Dsurefire.failIfNoSpecifiedTests=false`
Expected: PASS.

- [ ] **Step 2: Manual smoke (dev box)**

```bash
export JWT_SECRET="some-dev-secret-at-least-32-chars-flat"
mvn -f typeshift-backend/pom.xml spring-boot:run
```

Then in a browser: open `http://localhost:8080` → redirected to `/login`; signup a user incl. optional display name; login; land on `/`; check `/profile` fields; change display name; change password; logout; confirm the old token no longer grants `/`. Cookie in devtools: `typeshift_token`, HttpOnly, SameSite=Lax.

- [ ] **Step 3: Acceptance-criteria coverage check**

| AC (story) | Covered by |
|---|---|
| 0.1 form fields + rules | Task 4 (validation), Task 11 (form) |
| 0.1 BCrypt cost ≥ 12, no plaintext | Task 8 step 5 (`BCryptPasswordEncoder(12)`), encoding in Task 4 |
| 0.1 duplicate username message | Task 4 + Task 11 tests |
| 0.1 success triggers Story 0.6 + redirect | Task 4 (transactional init), Task 11 (redirect) |
| 0.2 login endpoint + generic failure | Task 9 + tests |
| 0.2 JWT subject/jti/iat/exp 30 days | Task 5 + tests |
| 0.2 secret env var, never committed | Task 5 (AuthProperties, app.properties) |
| 0.2 httpOnly/Secure/SameSite Lax cookie | Task 8 (`AuthCookie`), Task 9 test asserts flags |
| 0.2 token never in URLs/localStorage | cookie-only transport; `AuthCookie` is the single token set/read point |
| 0.2 brute-force 5 fails/15 min | Task 7 + Task 9 test |
| 0.3 filter on all routes minus login/signup/static | Task 8 (filter + lists) |
| 0.3 missing/malformed/expired/revoked rejected | Task 8 tests (MISSING/MALFORMED/EXPIRED/REVOKED) |
| 0.3 401 vs redirect | Task 8 tests |
| 0.3 revoked = jti in revoked_token | Task 6 + Task 8 REVOKED test |
| 0.3 fixed algorithm, "none" rejected | Task 5 (`HS256` explicit, unsigned-token test) |
| 0.3 rejection logged with reason, no token | Task 8 filter logging |
| 0.4 logout inserts jti with revoked_at + expires_at | Task 9 (`logout`), Task 6 entity |
| 0.4 cookie cleared | Task 9 test (maxAge 0) |
| 0.4 daily purge of expired rows | Task 6 (`@Scheduled` cron) |
| 0.4 reuse after logout → 401 | Task 9 `logoutRevokesTokenAndClearsCookie` |
| 0.4 logout of revoked/expired still success | Task 9 tests (`logoutWithoutCookieStillSucceeds`, `logoutWithExpiredTokenStillSucceedsAndClearsCookie`) |
| 0.5 profile shows username/displayName/date/stage | Task 10 |
| 0.5 display name editable | Task 10 |
| 0.5 password change needs current + ≥ 8 new | Task 10 tests |
| 0.5 keyboard HU_QWERTZ read-only | Task 10 template |
| 0.5 English-only interface | Task 10 template label |
| 0.6 profile row same transaction as user | Task 4 (`@Transactional`, service call inside) |
| 0.6 initial stage 1 / {1} / empty date / 0 min | Task 3 + test |
| 0.6 program start date left empty | Task 3 entity + null initializer (documented; set later at first sub-exam attempt) |
| 0.6 rollback on profile failure | Task 4 (`profileCreationFailureSurfacesAndWouldRollBack`) |

Any row without a green check after implementation → fix before finishing.

- [ ] **Step 4: Final commit (if anything remains uncommitted)**

```bash
git status
git add -A
git commit -m "test: complete epic 0 user management verification"
```

---

## Self-review record

1. **Spec coverage:** every AC of stories 0.1–0.6 maps to a task row in Task 12's table above. Program-start-date-on-first-attempt explicitly deferred to a later epic (Story 0.6 wording; field initialized null).
2. **Placeholders:** none — all steps carry full code or exact commands.
3. **Type consistency:** `createAccount(String, String, String)` (Tasks 4, 10, 11); `RevocationService.revoke(String, Instant)` consistent across 6/8/9; `AuthCookie.NAME` consistent 8/9/10; Task 10 exposes the `ProfileView` fields as flat model attributes consumed by `profile.html` (`${username}`, `${displayName}`, `${memberSince}`, `${currentStage}`); JWT principal is the username string throughout (Task 8 → Task 9/10 controllers).
4. **Known simplifications (deliberate):** in-memory brute-force map (restart clears it — ponytail-marked in Task 7); CSRF disabled relying on SameSite=Lax httpOnly cookie (rationale in Task 8's SecurityConfig javadoc); user-existence is not re-verified on each request (token claims trusted; revocation covers logout).
