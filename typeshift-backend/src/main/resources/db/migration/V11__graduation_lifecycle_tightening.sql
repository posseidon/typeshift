-- tighten-stage-progression-spec: graduation lifecycle hardening (OQ-6/OQ-7).
-- ABANDONED lets a new graduation start supersede a stale IN_PROGRESS attempt;
-- component_index keys each component of an attempt (1..3) so a duplicate
-- submission retry cannot write a second record for the same position.
ALTER TABLE graduation_attempt DROP CONSTRAINT ck_graduation_attempt_status;
ALTER TABLE graduation_attempt
    ADD CONSTRAINT ck_graduation_attempt_status
    CHECK (status IN ('IN_PROGRESS', 'PASSED', 'FAILED', 'ABANDONED'));

ALTER TABLE score_record ADD COLUMN component_index INTEGER NULL;
ALTER TABLE score_record
    ADD CONSTRAINT ck_score_record_component_index
    CHECK (
        (attempt_kind = 'SUB_EXAM' AND component_index IS NULL)
        OR (attempt_kind = 'GRADUATION' AND component_index BETWEEN 1 AND 3)
    );

-- One record per component position within an attempt (retry idempotency).
-- Predicate matches ck_score_record_graduation_link: only graduation rows
-- carry both keys, so sub-exam rows stay outside the index.
CREATE UNIQUE INDEX uk_score_record_attempt_component
    ON score_record (graduation_attempt_id, component_index)
    WHERE graduation_attempt_id IS NOT NULL;

-- At most one IN_PROGRESS graduation attempt per user and stage, enforced in
-- the database rather than application code alone.
CREATE UNIQUE INDEX uk_graduation_attempt_in_progress
    ON graduation_attempt (user_id, stage_id)
    WHERE status = 'IN_PROGRESS';
