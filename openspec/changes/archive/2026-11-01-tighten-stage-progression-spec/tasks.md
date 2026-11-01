# Tasks

## 1. Schema migration

- [x] 1.1 Create `typeshift-backend/src/main/resources/db/migration/V11__graduation_lifecycle_tightening.sql` with: the `graduation_attempt.status` CHECK extended by `ABANDONED` (drop + re-add, V9 style); a nullable `score_record.component_index` column with a CHECK (`NULL` for `SUB_EXAM` rows, `BETWEEN 1 AND 3` for `GRADUATION` rows, mirroring V10's link CHECK); a partial unique index on `score_record (graduation_attempt_id, component_index)` where not null; and a partial unique index on `graduation_attempt (user_id, stage_id)` where `status = 'IN_PROGRESS'` — verify by adding a `SchemaMigrationTest` case asserting each of the four statements (`mvn test` from `typeshift-backend` passes, following the V9/V10 test style).
- [x] 1.2 Verify the migration applies and validates cleanly on a real database: `docker compose up -d`, start the backend with the dev profile so Flyway runs, and confirm startup reports `V11` applied with `flyway validate` clean — observable behavior: no migration error, schema up to date.

## 2. Open questions

- [x] 2.1 Mark OQ-6 and OQ-7 as `(RESOLVED)` in `openspec/OPEN-QUESTIONS.md` with the adopted decisions (OQ-6: `currentStage` auto-advances to the highest unlocked stage in the unlock transaction; OQ-7: a new graduation start supersedes a stale `IN_PROGRESS` attempt by closing it as `ABANDONED`) — verify both lines carry the RESOLVED marker and cite this change.

## 3. Integration checks

- [x] 3.1 Run `openspec validate "tighten-stage-progression-spec" --type change` from the repo root and confirm it passes with the one `stage-progression` delta.
- [x] 3.2 Run the full backend suite (`mvn test` from `typeshift-backend`) and confirm the new migration test and all existing tests pass together.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied, so the delta lands in `openspec/specs/stage-progression/spec.md`.
- Verify the archived result (`openspec show "stage-progression" --type spec` shows the tightened requirements).
