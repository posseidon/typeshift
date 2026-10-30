# Story 1.5 — Score record schema

Table: score_record: id, user_id, stage_id, attempt_kind (SUB_EXAM or GRADUATION), sub_exam_number (nullable), accuracy, substitutions, insertions, deletions, passed, submitted_at.

Acceptance criteria
- [ ] One row per submitted sub-exam or graduation attempt.
- [ ] passed is derived from accuracy >= 0.95 at write time and stored.
- [ ] Graduation rows keep sub_exam_number null.
- [ ] Only scores are stored; exercise text is not (ADR-015).
