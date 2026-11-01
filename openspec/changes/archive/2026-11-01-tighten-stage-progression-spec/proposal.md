# Proposal

## Why

The `stage-progression` spec is the contract for Epic 2, but reading it against ADR-013, the Flyway schema, and the code already on disk shows it cannot be implemented as written: two requirements contradict locked decisions (Stage 3 graduation threshold; duplicate-submission safety the schema can't express), and three central behaviors are undefined or deferred to still-open questions (who advances `currentStage` — OQ-6, how the five statuses derive from raw rows, what closes an abandoned graduation attempt — OQ-7, which can permanently block a user). Tightening the spec now, before any scoring or unlock logic exists, is the cheapest moment to fix it: every requirement is still a target, so no working behavior breaks.

## What Changes

- **Status derivation becomes explicit**: the five-state model (`LOCKED`, `UNLOCKED`, `IN_PROGRESS`, `SUB_EXAMS_PASSED`, `GRADUATED`) gets a written first-match-wins derivation ladder with defined precedence, a stated `SUB_EXAMS_PASSED` rule (every sub-exam number has a passed record, monotonic under retakes), and clarification that only graded sub-exam score writes move the machine — practice sessions never do.
- **`currentStage` gets an owner (resolves OQ-6)**: it auto-advances to the highest unlocked stage in the same transaction as the unlock, stays separate from status display, and is not moved by graduation.
- **Stage 3 graduation threshold corrected**: Stage 1, 2, 4 graduation passes at 95%; Stage 3 graduation (Cashier Rush) requires exact, 100% correct answers, aligning the spec with locked ADR-013 instead of contradicting it.
- **Abandoned graduation attempts resolved (resolves OQ-7)**: starting a graduation supersedes any leftover `IN_PROGRESS` attempt for that user and stage by closing it as `ABANDONED` in the same transaction — no timers, no resumable content (ADR-015 persists no exercise), no permanent deadlock. `ABANDONED` joins the allowed status values.
- **Duplicate submissions get a real key**: graduation component scores carry a `component_index` (1..3), unique per attempt, so a retried submission returns the stored result instead of writing a second row; this also makes "three distinct task types" and the best-result ranking checkable. Enforce "at most one `IN_PROGRESS` attempt per user and stage" with a partial unique index rather than application code alone.
- **Scope notes recorded, not solved**: the missing owner of the sub-exam submission endpoint, and the existing `DashboardService` status derivation (emits `CURRENT` as a sixth status, ~5 queries instead of one) are recorded as follow-up work this change deliberately does not do.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `stage-progression`: five requirements tightened — explicit status derivation and precedence, `currentStage` advancement rule, Stage 3 graduation threshold carve-out, graduation attempt lifecycle supersede rule with `ABANDONED` status, and component-level dedupe keyed by `component_index`.

## Impact

- **Spec**: `openspec/specs/stage-progression/spec.md` (via the change's delta), then `openspec/OPEN-QUESTIONS.md` marks OQ-6 and OQ-7 resolved.
- **Database** (inert until graduation logic lands; follows this repo's schema-first pattern): a new Flyway migration extending the `graduation_attempt.status` check with `ABANDONED`, adding `score_record.component_index` with a per-attempt unique index, and a partial unique index for one `IN_PROGRESS` attempt per user and stage.
- **Conforming code, later changes only**: `LearningProfile.currentStage` updates move into the unlock transaction; `DashboardService` status derivation must drop `CURRENT` and adopt the derivation ladder (it already violates the existing "currentStage is separate from status" scenario); the graduation service implements supersede-on-start. None of these are implemented by this change.
- **Cross-spec consistency**: aligns with locked ADR-013, ADR-015 (no persisted exercises), and `real-world-tasks` (three distinct components). `ui`, `scoring`, and `progress-dashboard` requirement texts are unaffected.
- **Out of scope (recorded)**: the sub-exam submission endpoint contract (no spec currently owns it — belongs with `scoring`/`ui`), `task_type` on component records for distinctness proof, and all Epic 2 scoring/unlock/graduation logic.
