# Story 1.7 — Graduation attempt schema

Table: graduation_attempt: id, user_id, stage_id, status (IN_PROGRESS, PASSED, FAILED), started_at, finished_at, passed.

Acceptance criteria
- [ ] Retakes accumulate as separate rows. History is never overwritten.
- [ ] Links to score_record rows for each component task (Stage 5 has three).
- [ ] Unlock state lives in learning_profile, not here (separation of concerns).
