# Dashboard backed by persisted data

**Intent:** Replace the fabricated figures and statuses on `/` with values read for the signed-in owner from the existing database. Do not add score/session write paths as part of this change.

**Approach:** Add a small dashboard read service that loads the learning profile, stage definitions, practice-session summaries, score records, and weak-key counts using the existing Spring JDBC support. The controller passes this view data to `index.html`; the template renders it with Thymeleaf. Do not seed demo rows or infer a pass from speed/accuracy aggregates.

**Data rules:**
- Current station and total practice minutes come from `learning_profile`; station names/counts come from `stage`.
- A station is current when it matches `current_stage`, unlocked when listed in `learning_profile_unlocked_stages`, locked otherwise; call it graduated only when a passing graduation `score_record` exists. Do not render “Approved” from stage position alone.
- Speed history/counts come from `practice_session`; precision, attempt counts, and recent verdicts come from `score_record`; key friction comes from `weak_key_profile`.
- Empty tables render explicit “No sessions/attempts/miss data yet” states and zero counts. A chart with no rows has no plotted fake points. Metrics without a defined database source are omitted or replaced by a neutral unavailable state, never hardcoded.
- All reads are scoped to the authenticated username/user id. The route remains behind the existing authentication filter.

**Alternatives:** (1) Keep canned dashboard values until Epic 7 — rejected because they look like real records. (2) Implement the scoring/session write flows too — deferred because this request is to make the dashboard truthful, and those flows are separate work. (3) Read existing tables and show empty states — recommended and selected by the user.

**Constraints/non-goals:** Read-only change; no migration, synthetic data, scoring logic, progression writes, or changes to exercise routes. Preserve the existing visual layout where possible, but remove copy that makes unsupported claims (including fabricated dates, speeds, pass counts, heatmap misses, and audit references).

**Verification:** MockMvc tests cover a new account with empty tables and a seeded account with session/score/weak-key data; assert the page renders the database values, excludes old fabricated values, and does not leak another user's rows. Run `mvn verify`.
