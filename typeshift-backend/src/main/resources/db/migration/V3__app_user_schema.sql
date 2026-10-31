-- Story 1.1: users becomes app_user; revoked_token gains user_id and purge index.
ALTER TABLE users RENAME TO app_user;
ALTER TABLE app_user RENAME CONSTRAINT uk_users_username TO uk_app_user_username;

ALTER TABLE revoked_token ADD COLUMN user_id BIGINT;
ALTER TABLE revoked_token
    ADD CONSTRAINT fk_revoked_token_user FOREIGN KEY (user_id) REFERENCES app_user (id);
CREATE INDEX idx_revoked_token_expires_at ON revoked_token (expires_at);
