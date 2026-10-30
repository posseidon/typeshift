# Story 5.4 — Prose exercise page
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 002, 003, 007, 014, 015, 016

Acceptance criteria
- [ ] Requests an exercise from the server and displays displayText.
- [ ] Input is a contenteditable div (OQ-3).
- [ ] Per-character comparison runs as the user types to count misses for ADR-003.
- [ ] WPM and CPM update every 2 seconds (ADR-007) during practice sessions.
- [ ] Submit sends the full typed text to the server, which returns the score (ADR-016).
- [ ] Browser closed mid-exercise means the exercise is lost (ADR-015). No recovery.
- [ ] A server error shows the full stack trace and stops the exam (ADR-017).
