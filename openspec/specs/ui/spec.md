# ui Specification

## Purpose

Server-render the application with Thymeleaf and vanilla JavaScript: a shared layout shell, login/registration, the stage map, prose and numpad exercise pages, real-time speed counters, a results page, and a Hungarian keyboard reminder. (Epic 5 + the shared app-shell baseline.)

## Requirements

### Requirement: Shared layout shell with content injection point
The application SHALL render views through a shared layout template that defines a content injection point, so individual views provide only their content fragment while the shell owns the document skeleton, loads the engine stylesheet and script placeholders, and includes HTMX from its CDN. The index view SHALL be reachable at the site root.

#### Scenario: View renders inside the shell
- **WHEN** any view is requested
- **THEN** it renders as the shared layout plus that view's content fragment, with engine assets referenced

> **Status: IMPLEMENTED** — `layout.html` composes fragments and loads the engine assets.

### Requirement: Layout and navigation
The shared layout SHALL include navigation to the stage map, progress, profile, and logout. Navigation SHALL be hidden on the login and registration pages, and the layout SHALL work in desktop browsers.

#### Scenario: Authenticated pages show navigation
- **WHEN** an authenticated page renders
- **THEN** the navigation links are present

> **Status: PARTIAL** — the top bar renders Profile and Logout but has no dedicated Progress link (the dashboard is reached via the brand link), and the header renders unconditionally including on login/registration.

### Requirement: Login and registration pages
Login and registration forms SHALL post to the Spring endpoints, render errors inline from the server response, and use a generic login error text regardless of which field was wrong.

#### Scenario: Login error renders inline
- **WHEN** login fails
- **THEN** the generic error message is shown inline

> **Status: IMPLEMENTED** — `login.html` and `signup.html` post to the controllers and render inline errors.

### Requirement: Stage map page
The stage map SHALL show the five stages with their canonical status (LOCKED, UNLOCKED, IN_PROGRESS, SUB_EXAMS_PASSED, GRADUATED), mark `currentStage` separately from status, list each stage's sub-exams with pass/not-yet state, visibly disable locked stages without working links, and show a graduation button only for SUB_EXAMS_PASSED or GRADUATED stages.

#### Scenario: Locked stages are inert
- **WHEN** a stage is locked
- **THEN** it is visibly disabled and its links do not navigate

#### Scenario: Graduation button respects eligibility
- **WHEN** a stage has not passed all sub-exams
- **THEN** no graduation button is offered for it

> **Status: PARTIAL** — stages render from `currentStage` alone; per-sub-exam pass state, the canonical status model, and an eligibility-gated graduation button are not implemented, and locked stages still render practice links.

### Requirement: Prose exercise page
The prose exercise page SHALL request an exercise from the server and display its text, compare characters as the user types to count misses, update speed counters periodically, and send the full typed text to the server on submit for scoring. Closing the browser mid-exercise SHALL lose the exercise with no recovery. A server error SHALL show the full stack trace and stop the exam. (OQ-3's contenteditable input is a pending decision; a textarea is currently used.)

#### Scenario: Typed text is scored by the server
- **WHEN** the user submits a prose exercise
- **THEN** the full typed text is sent to the server, which returns the score

> **Status: PARTIAL** — server fetch, display, and client-side character comparison exist, but there is no submit endpoint or server scoring; input is a textarea rather than contenteditable, and there is no server-error/stack-trace handling.

### Requirement: Numpad exercise page
The numpad exercise page SHALL capture keydown using `KeyboardEvent.code`, accept only the ADR-006 numpad codes (ignoring other keys, with main-keyboard digits counting as misses), insert a comma on NumpadDecimal, show a NumLock reminder with a confirm checkbox that gates the exercise start, hide the live speed display for the graduation Cashier Rush, submit the typed result to the server for exact comparison, and stop with a stack trace on server error.

#### Scenario: Only numpad keys are accepted
- **WHEN** the user presses a non-numpad key during a numeric exercise
- **THEN** it is ignored (or counted as a miss for main-keyboard digits)

#### Scenario: Graduation hides live speed
- **WHEN** the Stage 3 graduation Cashier Rush runs
- **THEN** the live WPM display is hidden

> **Status: PARTIAL** — `KeyboardEvent.code` capture, numpad filtering, and NumpadDecimal exist client-side, but the confirm checkbox does not gate start, graduation does not hide live WPM, and there is no server submission or error handling (comparison is client-side string equality).

### Requirement: Real-time speed counters
The typing engine SHALL compute speed from a rolling 10-second window of correct characters, refresh the display periodically, send the final WPM and CPM once at submission, and hide the counters for graduation attempts. (ADR-007 / ADR-002.)

#### Scenario: Counters refresh while typing
- **WHEN** the user types
- **THEN** the speed readout updates on a rolling window

> **Status: PARTIAL** — the current implementation uses a whole-session average (not a rolling 10-second window), refreshes every second, has no CPM, sends nothing at submission, and never hides for graduation.

### Requirement: Results page
After a sub-exam or graduation, a results page SHALL show accuracy, pass/fail, substitutions, insertions, deletions, and final WPM/CPM, present the next action (retry or continue), and derive all results from the server response rather than recalculating in the browser.

#### Scenario: Results come from the server
- **WHEN** an attempt is scored
- **THEN** the results page renders the server-provided score breakdown and next action

> **Status: PLANNED** — no results template, route, or server-score round-trip exists.

### Requirement: Hungarian keyboard reminder
A one-time reminder at the first session start SHALL prompt the user to set the OS keyboard to Hungarian QWERTZ, be remembered as dismissed in the learning profile or a user setting, and never block the exercise.

#### Scenario: Reminder is shown once and remembered
- **WHEN** the user starts their first session and dismisses the reminder
- **THEN** the dismissal is persisted and the reminder does not appear again

> **Status: PARTIAL** — a non-blocking reminder banner exists and does not block typing, but it is not tied to the first session and its dismissal is not persisted.
