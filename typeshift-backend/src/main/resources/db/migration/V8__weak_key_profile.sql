-- Story 1.6: cumulative per-character miss counts (ADR-003 merge -> Story 4.5).
-- key_char uses VARCHAR(4): decomposed Unicode forms (e + combining mark) exceed
-- one code point though ADR-003 keys are conceptually single characters.
CREATE TABLE weak_key_profile (
    user_id    BIGINT                      NOT NULL,
    key_char   VARCHAR(4)                  NOT NULL,
    miss_count INTEGER                     NOT NULL CHECK (miss_count >= 0),
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, key_char),
    CONSTRAINT fk_weak_key_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE INDEX idx_weak_key_user_miss
    ON weak_key_profile (user_id, miss_count DESC);
