# Spec Delta

## ADDED Requirements

### Requirement: Status derivation and precedence
Each stage's status SHALL be derived from unlock state, sub-exam score records, and graduation attempts by evaluating these rules in order and taking the first match: not unlocked -> LOCKED; any passed graduation attempt -> GRADUATED; every sub-exam number with a passed record -> SUB_EXAMS_PASSED; any sub-exam record -> IN_PROGRESS; otherwise -> UNLOCKED. Practice sessions SHALL NOT affect status.

#### Scenario: A locked stage stays locked
- **WHEN** a stage is not in the owner's unlocked set
- **THEN** its status is LOCKED regardless of any other record

#### Scenario: Graduated supersedes sub-exams passed
- **WHEN** a stage has every sub-exam passed and a passed graduation attempt
- **THEN** its status is GRADUATED, not SUB_EXAMS_PASSED

#### Scenario: All sub-exams passed resolves to sub-exams passed
- **WHEN** every sub-exam number of an unlocked stage has at least one passed record and no graduation attempt has passed
- **THEN** its status is SUB_EXAMS_PASSED

#### Scenario: Any sub-exam record starts the stage
- **WHEN** a sub-exam record exists for an unlocked stage but not every sub-exam number has a passed record
- **THEN** its status is IN_PROGRESS

#### Scenario: An untouched unlocked stage is unlocked
- **WHEN** a stage is unlocked and has no sub-exam records
- **THEN** its status is UNLOCKED

#### Scenario: Practice does not move the state machine
- **WHEN** practice sessions are recorded for a stage that has no sub-exam records
- **THEN** its status remains UNLOCKED

#### Scenario: A failed retake does not un-pass a sub-exam
- **WHEN** a sub-exam that once passed is retaken and fails
- **THEN** derivation still counts that sub-exam as passed

### Requirement: currentStage advancement
`currentStage` SHALL always equal the highest unlocked stage, SHALL advance in the same transaction as the unlock that raises it, and SHALL NOT change on graduation or on submissions to earlier stages. The active-stage marker SHALL stay separate from the five statuses.

#### Scenario: The unlock advances the marker
- **WHEN** passing the last sub-exam of Stage N unlocks Stage N+1
- **THEN** `currentStage` becomes N+1 in the same transaction as the score record

#### Scenario: Graduation does not move the marker
- **WHEN** a graduation attempt passes for a stage below `currentStage`
- **THEN** `currentStage` is unchanged

#### Scenario: Practicing an earlier stage keeps the marker
- **WHEN** a sub-exam is submitted for an earlier unlocked stage
- **THEN** `currentStage` stays at the highest unlocked stage

### Requirement: Superseding abandoned graduation attempts
Starting a graduation SHALL close any still-IN_PROGRESS attempt for the same user and stage as ABANDONED within the same transaction instead of refusing the start, leaving at most one IN_PROGRESS attempt. An ABANDONED attempt SHALL be closed, SHALL keep its component records, and SHALL permit a fresh start.

#### Scenario: A stale in-progress attempt is superseded
- **WHEN** a graduation is started while an earlier attempt for the same user and stage is still IN_PROGRESS, for example after the browser was closed mid-attempt
- **THEN** the earlier attempt is closed as ABANDONED and the new attempt is the only IN_PROGRESS attempt

#### Scenario: The superseded attempt keeps its history
- **WHEN** an IN_PROGRESS attempt is closed as ABANDONED
- **THEN** its component records remain linked to it and a new graduation start is permitted

## MODIFIED Requirements

### Requirement: Graduation eligibility
Graduation SHALL be offered only for a stage in SUB_EXAMS_PASSED or GRADUATED state, never for LOCKED, UNLOCKED, or IN_PROGRESS. Stage 1, 2, and 4 graduation SHALL be one component task passed at 95% accuracy; Stage 3 graduation SHALL require every answer exact; Stage 5 graduation SHALL be three distinct task types in sequence, all of which must pass.

#### Scenario: Graduation is gated on sub-exams
- **WHEN** a user attempts graduation for a stage whose sub-exams are not all passed
- **THEN** the attempt is not offered

#### Scenario: Stage 5 requires three passing tasks
- **WHEN** a Stage 5 graduation attempt runs
- **THEN** all three distinct task components must pass for the attempt to pass

#### Scenario: Stage 3 graduation requires exact answers
- **WHEN** a Stage 3 graduation component is submitted with any inexact answer
- **THEN** the attempt fails regardless of aggregate accuracy

### Requirement: Graduation attempt lifecycle
Graduation attempt rows SHALL be created at start and closed at finish. Failing any component SHALL end the attempt as FAILED, after which a new attempt may start. At most one IN_PROGRESS attempt SHALL exist per user and stage; duplicate submission retries SHALL NOT create duplicate component records, and a failed attempt's component records SHALL remain linked to it.

#### Scenario: A component failure fails the attempt
- **WHEN** any component task in a graduation attempt fails
- **THEN** the attempt is closed as FAILED and a new attempt may be started

#### Scenario: Only one attempt is in progress
- **WHEN** a graduation attempt is already in progress for a user and stage
- **THEN** a second concurrent attempt is not created

#### Scenario: A retried component submission does not duplicate records
- **WHEN** a graduation component submission is retried for an attempt that already holds that component's result
- **THEN** the recorded result is returned and no second component record is written
