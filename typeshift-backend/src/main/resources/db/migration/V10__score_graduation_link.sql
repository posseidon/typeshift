-- Review fix (epic 1): a graduation score must always link its attempt and a
-- sub-exam score must never link; V9 allowed an unlinked GRADUATION score.
-- Story 2.3 creates the attempt before any component score is written.
ALTER TABLE score_record DROP CONSTRAINT ck_score_record_graduation_link;
ALTER TABLE score_record
    ADD CONSTRAINT ck_score_record_graduation_link
    CHECK ((attempt_kind = 'GRADUATION' AND graduation_attempt_id IS NOT NULL)
        OR (attempt_kind = 'SUB_EXAM' AND graduation_attempt_id IS NULL));
