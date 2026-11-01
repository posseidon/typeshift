# scoring Specification

## Purpose

Deterministically evaluate typed input and aggregate miss data. No AI is involved. (Epic 4.)

> **Implementation status: PLANNED.** The target tables (`practice_session`, `score_record`, `weak_key_profile`) exist from the data-model capability, but no evaluator, threshold constant, submission endpoint, or aggregation logic is implemented yet.

## Requirements

### Requirement: Wagner-Fischer text evaluator
The evaluator SHALL compute edit distance between target and typed text with an alignment trace, outputting correct characters, substitutions, insertions, deletions, and the missed target characters. Accuracy SHALL be correct characters over target length, compared unrounded and stored rounded to 4 decimals. Case and diacritics SHALL match exactly, and evaluation SHALL be deterministic.

#### Scenario: Accuracy reflects edit distance
- **WHEN** typed text is compared to the target
- **THEN** accuracy and the substitution/insertion/deletion counts are derived from the alignment

#### Scenario: Diacritics are exact
- **WHEN** the target contains "á" and the user types "a"
- **THEN** the character counts as a substitution, not a match

#### Scenario: Empty input scores zero
- **WHEN** a submission has empty typed input
- **THEN** accuracy is 0

> **Status: PLANNED** — no runtime evaluator exists.

### Requirement: Math result evaluator
The evaluator SHALL parse numpad input accepting a comma as the decimal separator and compare using BigDecimal `compareTo`. Invalid or empty input SHALL be a miss rather than an exception, every Stage 3 answer must be correct to pass, and missed numpad keys SHALL count toward the miss profile.

#### Scenario: Equivalent decimals compare equal
- **WHEN** the user enters "4,50" against an expected "4,5"
- **THEN** the answers compare equal

#### Scenario: Invalid input is a miss
- **WHEN** the user enters unparseable or empty numeric input
- **THEN** it counts as a miss, not an error

> **Status: PLANNED** — no runtime math evaluator exists.

### Requirement: Accuracy threshold enforcement
A single 0.95 threshold constant SHALL be referenced everywhere, with pass derived as accuracy >= threshold, and unit tests covering the boundary.

#### Scenario: Boundary accuracy is handled
- **WHEN** accuracy is exactly 0.95
- **THEN** the attempt passes; at 0.9499 it fails; at 0.9501 it passes

> **Status: PLANNED** — no threshold constant or pass derivation exists (only a SQL comment mentions 0.95).

### Requirement: Session summary endpoint
An endpoint SHALL accept wpm, cpm, duration, character count, and the miss map; validate non-negative ranges and single-character miss keys; write one practice session row; trust the client duration (ADR-011); and trigger the weak-key merge.

#### Scenario: A submitted session is persisted
- **WHEN** a valid session summary is posted
- **THEN** one practice session row is written and the weak-key merge runs

> **Status: PLANNED** — no session submission endpoint exists.

### Requirement: Weak-key aggregation
Each session's miss map SHALL be merged into the weak-key profile in one transaction, additively (stored + session), inserting absent keys, idempotent per submission (a repeated submission with the same request id is ignored). The top-5 query SHALL be served from this table.

#### Scenario: Miss counts accumulate
- **WHEN** a session's miss map is merged
- **THEN** each key's stored count increases by the session count

#### Scenario: Duplicate submission is ignored
- **WHEN** the same submission is received twice with the same request id
- **THEN** the second is ignored and counts are not double-applied

> **Status: PLANNED** — no merge logic exists; the top-5 read query is served from an empty table.
