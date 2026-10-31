# Epic 1 — Gaps and open questions

Parking lot for things to clarify before or while building epic 1. Return here before planning each story.

## Resolved this session

- V2 (`V2__core_schema.sql`) already holds `users`, `learning_profiles`, and `revoked_token` in a shape written to satisfy the epic-0 entities. Epic 1 brings the schema to the story-1.x state via new migrations V3–V9 (never by rewriting V1/V2), so both a fresh database and an already-migrated dev database work.
- Existing entity table names are realigned with the stories: `users` → `app_user`, `learning_profiles` → `learning_profile` (two `@Table` edits + `ALTER TABLE ... RENAME`). Later epics refer to these table names verbatim (e.g. story 5.8's "remembered in learning_profile", ADR-012's `weak_key_profile`), so the names are pinned now while no data exists.
- Flyway is never executed by the test suite (full-context tests run SQLite `create-drop`; Flyway is disabled). Migration correctness is pinned by a new `SchemaMigrationTest` (structure assertions per migration) plus a manual Flyway run against the Compose Postgres in the final task — same verification style as 0b.5/0b-Q6.

## Defaulted this session (re-check when planning)

- New tables use the story names verbatim, singular: `stage`, `practice_session`, `score_record`, `weak_key_profile`, `graduation_attempt` (matches the existing singular `revoked_token`).
- `learning_profile` keeps its surrogate `id` primary key with a `UNIQUE(user_id)` FK instead of making `user_id` the primary key. One profile per user is enforced either way; `user_id`-as-PK would need `@MapsId` changes for no functional gain.
- `unlocked_stages` stays a reference table (`learning_profile_unlocked_stages`, existing decision); its `stage` column becomes `NOT NULL`, range-checked 1–5, unique per profile — EXISTS queries support "is stage N unlocked" in one query.
- `revoked_token` keeps its surrogate `id` with `UNIQUE(jti)` instead of `jti` as primary key (functionally equivalent). The story's `user_id` column is added NULLABLE and deliberately **not mapped** in the entity: `logout` parses only jti/expiry and does not know the user; populating it would ripple through `LoginService`/`RevocationService` for an admin nicety nobody reads yet.
- Numeric precision: `wpm`/`cpm` `NUMERIC(6,2)`; `accuracy` `NUMERIC(5,4)` (ADR-016 defines accuracy as a 0..1 fraction); counts and durations `INTEGER`.
- Primary-key/FK type is `BIGINT` everywhere for consistency; `stage.id` is a static `BIGINT` 1–5 so FKs from later tables type-match.
- `graduation_attempt` ↔ `score_record` link (story 1.7 AC): nullable `score_record.graduation_attempt_id` FK added in V9, with a CHECK that only `GRADUATION` rows can link. A junction table was rejected as unnecessary.
- `weak_key_profile.key_char` is `VARCHAR(4)`, not `VARCHAR(1)`, to survive decomposed Unicode forms of á/ő etc. (ADR-003 map keys are "single characters"; composed vs decomposed is not normalized anywhere).
- `stage` seed uses the authoritative `constants/stage-map.md` wording. Note: app-side `CannedCatalog.STAGE_NAMES` says "Numpad & Math" while stage-map/ADR-013 say "Numpad and Math" — the DB seed follows the constants file; the catalog constant is cosmetic and out of scope here.
- No `ON DELETE CASCADE` anywhere between domain tables (no user-deletion path exists in v1); defaults keep `NO ACTION`, which also enforces seed-row immutability from the database side.
- Enum-like columns hold CHECK constraints (`exercise_kind IN ('PROSE','MATH')`, `attempt_kind IN ('SUB_EXAM','GRADUATION')`, `status IN ('IN_PROGRESS','PASSED','FAILED')`, `sub_exam_number` only for `SUB_EXAM` rows) — PostgreSQL-native, no SQL types.

## Answered after review

(pending human review of the defaults above)

## Open questions

None blocking. Please ratify (or veto) the three defaults most likely to matter later:

- renames `users → app_user` / `learning_profiles → learning_profile` (touches 2 entities),
- `revoked_token.user_id` stored but never populated,
- `key_char VARCHAR(4)` Unicode hedge.
