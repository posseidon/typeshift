package hu.elte.ntb.typeshift.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
// JPA entity: getters expose persisted fields by design; defensive copies
// would break Hibernate collection tracking.
@SuppressFBWarnings("EI_EXPOSE_REP")
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
  @SuppressFBWarnings("EI_EXPOSE_REP2")
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

  @SuppressFBWarnings("EI_EXPOSE_REP2")
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
