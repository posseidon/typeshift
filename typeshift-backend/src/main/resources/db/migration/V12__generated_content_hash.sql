CREATE TABLE generated_content_hash (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    content_hash CHAR(64) NOT NULL,
    task_type VARCHAR(32),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX generated_content_hash_user_created_idx
    ON generated_content_hash (user_id, created_at DESC, id DESC);
