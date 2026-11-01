# progress-dashboard Specification

## Purpose

Show the owner their speed, accuracy, stage completion, and miss history on a dashboard. (Epic 7.)

> **Implementation status: PARTIAL, and data-dependent.** The read-side queries and much of the display exist, but the dashboard reads tables (`practice_session`, `score_record`, `weak_key_profile`, `graduation_attempt`) that nothing currently writes — the scoring/submission path is not built, so these views stay empty until it lands.

## Requirements

### Requirement: WPM history chart
The dashboard SHALL show a line chart of WPM and CPM per practice session over time from the practice-session table, filterable by stage, using a chart library that works with vanilla JavaScript and no build step.

#### Scenario: Speed history is charted
- **WHEN** the dashboard loads with practice sessions on record
- **THEN** a line chart plots WPM and CPM over time

> **Status: PARTIAL** — WPM history is queried and rendered as a plain list (not a line chart, no CPM, no stage filter, no chart library).

### Requirement: Miss key heatmap
The dashboard SHALL render the weak-key profile as a keyboard-layout heatmap for the QWERTZ/Hungarian layout, showing zero-miss keys as neutral, revealing the exact count on hover or focus, grouping numpad keys separately, and conveying intensity through more than colour alone.

#### Scenario: Weak keys appear as a heatmap
- **WHEN** the dashboard loads with recorded misses
- **THEN** a keyboard heatmap highlights the most-missed keys accessibly

> **Status: PARTIAL** — weak keys are queried (top 5) and shown as a flat list; no heatmap, neutral keys, hover counts, numpad block, or non-colour encoding exists.

### Requirement: Stage completion overview
Per stage the dashboard SHALL show sub-exams passed out of total and the graduation best result, reading only from the score-record and graduation-attempt tables.

#### Scenario: Completion is summarised per stage
- **WHEN** the dashboard loads
- **THEN** each stage shows its sub-exams passed/total and graduation result

> **Status: PARTIAL** — sub-exams passed/total is shown; graduation is only a graduated boolean with no best result.

### Requirement: Sub-exam history table
The dashboard SHALL show a table of sub-exam score records, newest first, with columns for stage, sub-exam, accuracy, passed, and date.

#### Scenario: Recent sub-exams are listed
- **WHEN** the dashboard loads with scored sub-exams
- **THEN** a newest-first table lists them with accuracy and pass state

> **Status: PARTIAL** — a newest-first list exists but is limited to the 5 most recent rows and mixes graduation rows in; it is not a full sub-exam history table.

### Requirement: Graduation attempt history
The dashboard SHALL show a table of graduation-attempt rows per stage, showing all retakes, and display the best and latest result per stage.

#### Scenario: Graduation history with best and latest
- **WHEN** the dashboard loads with graduation attempts
- **THEN** a per-stage table lists all retakes with best and latest results

> **Status: PLANNED** — graduation attempts are only used to derive a boolean graduated flag; no rows, retakes, or best/latest results are queried or rendered.
