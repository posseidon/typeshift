# real-world-tasks Specification

## Purpose

Provide randomised workplace-style typing tasks for Stage 5, drawn from four real-world formats and sequenced for graduation. (Epic 6.)

> **Implementation status: mostly PLANNED.** The four task types exist as an enum and as static canned exemplars, but random selection, the graduation sequence, and server-side scoring are not implemented.

## Requirements

### Requirement: Task pool of four types
The task pool SHALL hold exactly four task types: CHAT_BURST, EMAIL_REPLY, DOCUMENT_PARAGRAPH, and CODE_COMMENT, each with a canned example matching its format.

#### Scenario: Four task types are defined
- **WHEN** the Stage 5 task pool is inspected
- **THEN** it contains exactly the four task types

> **Status: IMPLEMENTED** — the `TaskType` enum defines the four types and the canned catalog holds one exemplar each.

### Requirement: Random selection avoiding the previous type
Task selection SHALL be uniform random, excluding the type used on the previous attempt, and the selected type SHALL be passed to the prose agent.

#### Scenario: Consecutive attempts differ
- **WHEN** a Stage 5 sub-exam is requested after a previous attempt
- **THEN** the selected task type differs from the previous attempt's type

> **Status: PLANNED** — Stage 5 is currently served via fixed routes with no random selection.

### Requirement: Graduation three-task sequence
Graduation SHALL draw three distinct task types without repeats, present them one at a time in order, score each separately (one score record per task), end the graduation attempt as FAILED if any task fails, and show a "task n of 3" progress indicator. A sub-exam is one task; graduation uses three distinct types in one sequence.

#### Scenario: Three distinct tasks run in order
- **WHEN** a Stage 5 graduation attempt starts
- **THEN** three distinct task types are presented sequentially with a progress indicator

#### Scenario: A failed task ends the attempt
- **WHEN** any task in the sequence fails
- **THEN** the graduation attempt is marked FAILED

> **Status: PLANNED** — Stage 5 graduation is currently a single canned passage labeled "three-task sequence" with no sequence or scoring logic.

### Requirement: Task format contracts
Each task type SHALL match its documented format and length bounds, with no markup or code fences in the display text: CHAT_BURST (one-to-three-sentence informal message), EMAIL_REPLY (subject line plus a two-to-four-sentence body), DOCUMENT_PARAGRAPH (one structured business paragraph), CODE_COMMENT (a technical comment for a code block described in words).

#### Scenario: Format is matched
- **WHEN** a task of a given type is generated
- **THEN** its display text conforms to that type's format within the Stage 5 length bounds

> **Status: PARTIAL** — canned exemplars conform to each format, but no agent generates or validates against the bounds and length limits.
