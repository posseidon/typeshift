# Story 1.2 — Learning profile schema
Points: 3 | Tool: executing-plans | ADRs: 009, 013

Table: learning_profile: user_id (primary key, foreign key to app_user), current_stage, unlocked_stages, program_start_date (nullable), total_practice_minutes, keyboard_layout (default HU_QWERTZ), updated_at.

Acceptance criteria
- [ ] current_stage constrained to 1 to 5.
- [ ] unlocked_stages stored as a table or array, not a comma string. Decision left to the story plan; must support "is stage N unlocked" in one query.
- [ ] One profile per user.
