# stage-progression Specification

## Purpose

Decide pass or fail for each sub-exam and graduation attempt, and move users through the five-stage program via a one-way unlock state machine. (Epic 2.)

> **Implementation status: PLANNED.** The database tables (`score_record`, `graduation_attempt`, `learning_profile.unlocked_stages`) exist from the data-model capability, but no scoring, unlock, or graduation-transition logic is implemented yet. Every requirement below is a target, not current behavior.

## Requirements

### Requirement: Sub-exam pass and fail derivation
A sub-exam SHALL be passed when server-derived accuracy is 0.95 or higher; Stage 3 numeric sub-exams SHALL pass only when every answer is exact. The result SHALL be derived server-side from the submitted text, never from client claims. Each submitted attempt SHALL write one score record. A sub-exam may be retaken immediately with no cooldown, and repeating an already-passed sub-exam SHALL NOT change its earlier passed state.

#### Scenario: Accurate submission passes
- **WHEN** a sub-exam is submitted with accuracy at or above 0.95
- **THEN** the attempt is recorded as passed

#### Scenario: Below-threshold submission fails
- **WHEN** a sub-exam is submitted with accuracy below 0.95
- **THEN** the attempt is recorded as failed and can be retaken immediately

#### Scenario: Stage 3 requires exact answers
- **WHEN** a Stage 3 numeric sub-exam is submitted with any inexact answer
- **THEN** the attempt fails regardless of aggregate accuracy

> **Status: PLANNED** — no server-side evaluation or submission endpoint exists yet.

### Requirement: One-way stage unlock state machine
Each stage SHALL resolve to one of LOCKED, UNLOCKED, IN_PROGRESS, SUB_EXAMS_PASSED, or GRADUATED, derived from unlock state and score/graduation records rather than an extra status column. Passing the last sub-exam of Stage N SHALL move it to SUB_EXAMS_PASSED and unlock Stage N+1 in the same transaction as the triggering score record. Transitions SHALL be one-way, a stage SHALL NOT unlock twice, and Stage 5 SHALL unlock nothing. All stage states SHALL be readable in one query.

#### Scenario: Last sub-exam unlocks the next stage
- **WHEN** the final required sub-exam of Stage N is passed
- **THEN** Stage N becomes SUB_EXAMS_PASSED and Stage N+1 is unlocked atomically with the score write

#### Scenario: Invalid transitions are rejected
- **WHEN** an invalid state transition is attempted
- **THEN** it is rejected and logged

#### Scenario: Stage 5 unlocks nothing
- **WHEN** Stage 5's sub-exams are all passed
- **THEN** no further stage is unlocked

#### Scenario: currentStage is separate from status
- **WHEN** a stage's status is displayed
- **THEN** the active-stage marker (currentStage) is shown separately, not as a sixth status

> **Status: PLANNED** — the unlock transition logic does not exist; the current UI derives display states ad-hoc from `currentStage` only.

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

> **Status: PLANNED** — no graduation attempt lifecycle or gating logic exists yet.

### Requirement: Superseding abandoned graduation attempts
Starting a graduation SHALL close any still-IN_PROGRESS attempt for the same user and stage as ABANDONED within the same transaction instead of refusing the start, leaving at most one IN_PROGRESS attempt. An ABANDONED attempt SHALL be closed, SHALL keep its component records, and SHALL permit a fresh start.

#### Scenario: A stale in-progress attempt is superseded
- **WHEN** a graduation is started while an earlier attempt for the same user and stage is still IN_PROGRESS, for example after the browser was closed mid-attempt
- **THEN** the earlier attempt is closed as ABANDONED and the new attempt is the only IN_PROGRESS attempt

#### Scenario: The superseded attempt keeps its history
- **WHEN** an IN_PROGRESS attempt is closed as ABANDONED
- **THEN** its component records remain linked to it and a new graduation start is permitted

### Requirement: Graduation retake
A GRADUATED stage SHALL be retakable at any time for confidence, and a failed retake SHALL NOT revoke GRADUATED status. Every retake SHALL be stored as its own graduation attempt row. The stage map SHALL show best and latest results separately; best ranking considers a passed attempt above a failed one, then more passing components, then higher minimum component accuracy, then most recent finish.

#### Scenario: Failed retake keeps graduation
- **WHEN** a user retakes an already-graduated stage and fails
- **THEN** the stage remains GRADUATED and the failed retake is stored as a new row

> **Status: PLANNED** — no retake path, best/latest ranking, or stage-map result display exists yet.
