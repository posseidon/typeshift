# data-model Specification

## Purpose

Define the core relational schema the application persists, authored as plain PostgreSQL SQL via Flyway. Exercises are never stored (ADR-015) — only scores and aggregate metrics. (Epic 1.)

## Requirements

### Requirement: User and revocation schema
The schema SHALL include an `app_user` table (username unique, password hash, display name, timestamps) and a `revoked_token` table with a uniqueness constraint on the token id and an index on expiry to support the daily purge.

#### Scenario: Username uniqueness is database-enforced
- **WHEN** two accounts attempt to share a username
- **THEN** the database constraint rejects the duplicate independently of application checks

#### Scenario: Revocation purge is indexed
- **WHEN** the purge job queries revocation rows by expiry
- **THEN** an index supports the expiry lookup

### Requirement: Learning profile schema
The schema SHALL include a `learning_profile` table with a `current_stage` constrained to 1–5, unlocked stages stored as a related table (not a comma string) supporting a one-query "is stage N unlocked" check, and one profile per user.

#### Scenario: Current stage is range-constrained
- **WHEN** a write attempts to set `current_stage` outside 1–5
- **THEN** the database rejects it

#### Scenario: Unlock lookup is a single query
- **WHEN** the application checks whether a stage is unlocked
- **THEN** it is answerable from the unlocked-stages table in one query

### Requirement: Stage reference schema
The schema SHALL include a `stage` table seeded by migration with the five stages from the authoritative stage map, sub-exam counts 3/2/2/2/4, and graduation names. Seed rows SHALL be immutable in application code.

#### Scenario: Stages are seeded
- **WHEN** migrations run against a fresh database
- **THEN** five stage rows exist matching the stage map

### Requirement: Practice session summary schema
The schema SHALL include a `practice_session` table written once per practice session at submission, holding speed and character aggregates with no keystroke or intermediate rows, indexed on user and submission time for the progress charts.

#### Scenario: One row per submitted session
- **WHEN** a practice session is submitted
- **THEN** a single summary row is stored, with no per-keystroke rows

### Requirement: Score record rows
The schema SHALL include a `score_record` table with one row per submitted sub-exam attempt and one row per graduation component task, storing scores and accuracy only — never exercise text.

#### Scenario: One row per attempt
- **WHEN** a sub-exam or graduation component task is scored
- **THEN** exactly one score row is stored for it

#### Scenario: Exercise text is not persisted
- **WHEN** an attempt is scored
- **THEN** no exercise text column stores the prompt or typed content

### Requirement: Score record attempt linkage
A graduation score row SHALL link to its graduation attempt and keep the sub-exam number null; a sub-exam row SHALL keep the graduation link null. The `passed` flag SHALL be stored alongside accuracy. These constraints SHALL be enforced by the database.

#### Scenario: Attempt kind drives the link
- **WHEN** a score row is written
- **THEN** its attempt kind determines whether it carries a sub-exam number or a graduation-attempt link, per the database check constraints

### Requirement: Weak key profile schema
The schema SHALL include a `weak_key_profile` table keyed by (user, key character) with a cumulative miss count, and an index supporting a "top 5 by miss count for a user" query.

#### Scenario: Top weak keys are queryable
- **WHEN** the application requests a user's most-missed keys
- **THEN** an index supports the ordered top-N lookup

### Requirement: Graduation attempt schema
The schema SHALL include a `graduation_attempt` table that is append-only (retakes accumulate as separate rows, history is never overwritten), links to its component `score_record` rows, and keeps unlock state in `learning_profile` rather than here. `status` and `passed` SHALL remain consistent (IN_PROGRESS ⇒ passed=false, finished_at null; PASSED ⇒ passed=true, finished_at set; FAILED ⇒ passed=false, finished_at set).

#### Scenario: Retakes accumulate
- **WHEN** a stage graduation is retaken
- **THEN** each attempt is stored as its own row

#### Scenario: Component scores link to their attempt
- **WHEN** a graduation attempt's component tasks are scored
- **THEN** each component `score_record` references its `graduation_attempt`

> **Status: PARTIAL** — only the `finished_at` nullability per status is enforced by a database check; the `passed`-flag-per-status consistency is not yet database- or code-enforced.
