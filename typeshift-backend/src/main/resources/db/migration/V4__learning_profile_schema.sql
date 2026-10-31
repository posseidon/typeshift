-- Story 1.2: learning_profiles becomes learning_profile; keyboard layout and
-- updated_at columns plus one-profile/stage-range guarantees.
ALTER TABLE learning_profiles RENAME TO learning_profile;
ALTER TABLE learning_profile
    RENAME CONSTRAINT uk_learning_profiles_user TO uk_learning_profile_user;

ALTER TABLE learning_profile
    ADD COLUMN keyboard_layout VARCHAR(16) NOT NULL DEFAULT 'HU_QWERTZ';
ALTER TABLE learning_profile
    ADD COLUMN updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now();
ALTER TABLE learning_profile
    ADD CONSTRAINT ck_learning_profile_current_stage CHECK (current_stage BETWEEN 1 AND 5);

ALTER TABLE learning_profile_unlocked_stages ALTER COLUMN stage SET NOT NULL;
ALTER TABLE learning_profile_unlocked_stages
    ADD CONSTRAINT ck_unlocked_stage_range CHECK (stage BETWEEN 1 AND 5);
CREATE UNIQUE INDEX uk_learning_profile_unlocked_stage
    ON learning_profile_unlocked_stages (learning_profile_id, stage);
