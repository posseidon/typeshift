# Story 1.4 — Practice session summary schema
Points: 3 | Tool: executing-plans | ADRs: 002, 003, 009

Table: practice_session: id, user_id, stage_id, exercise_kind (PROSE or MATH), wpm, cpm, correct_characters, total_characters, duration_seconds, submitted_at.

Acceptance criteria
- [ ] Written once per practice session at submission (ADR-002).
- [ ] No keystroke or intermediate rows (ADR-003).
- [ ] Indexed on user_id and submitted_at for the progress charts.
