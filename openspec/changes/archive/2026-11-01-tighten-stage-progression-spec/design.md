# Design

## Context

The `stage-progression` capability is entirely PLANNED: tables exist (`score_record`, `graduation_attempt`, `learning_profile.unlocked_stages`, `stage`), but no scoring, unlock, or graduation logic is implemented. Tightening happens before any of that code exists, so nothing working changes.

Shape of the current state (details in proposal.md):

- Statuses must be derived from three inputs — the `unlocked_stages` set, `score_record` rows, and `graduation_attempt` rows — because the schema deliberately has no status column.
- `graduation_attempt.status` is a CHECK constraint (`IN_PROGRESS | PASSED | FAILED`); `score_record` forces `sub_exam_number` to NULL for graduation rows, so component rows within one attempt are currently indistinguishable.
- ADR-015 persists no exercise content: a browser closed mid-graduation leaves `IN_PROGRESS` forever with no server-side event to close it.
- The dashboard already reads status ad-hoc (five queries, `CURRENT` emitted as a sixth status) — conformance work deferred, listed in the proposal.

## Goals / Non-Goals

**Goals:**

- Make every requirement in the delta spec implementable and testable without interpretation: a written derivation ladder, an owner for `currentStage`, a threshold that agrees with ADR-013, a resolution for abandoned attempts, and a schema-level key for component dedupe.
- Keep the schema changes inert: they must be safe to migrate now, while zero graduation logic exists.

**Non-Goals:**

- Implementing any scoring, unlock, graduation, or endpoint logic (Epic 2 work, later changes).
- Reworking `DashboardService` status derivation or its query count.
- Owning the sub-exam submission endpoint (recorded gap; belongs to `scoring`/`ui`).
- Storing `task_type` on component records to prove the three Stage 5 types are distinct.

## Decisions

### D1: `currentStage` auto-advances — derived, monotonic

`currentStage` is defined as *the highest unlocked stage*, written in the same transaction as the unlock that raises it.

- **Why over user-picked active stage:** no new UI or endpoint, consistent with the one-way unlock machine, and graduation (a confidence record per OQ-1) correctly has no effect. A user can still visit and re-practice earlier unlocked stages; only the marker stays put.
- **Alternative rejected:** user-selectable active stage — nicer for "re-doing Stage 1 this week", but requires a new control, a new endpoint, and spec rules for switching; deferred until someone actually asks.
- **Drift guard:** because the value is derived from `unlocked_stages`, the unlock routine must set both fields in one transaction. The delta spec's scenario makes this testable.

### D2: The derivation ladder is one SQL query, evaluated top-down

```
CASE
  WHEN :stage NOT IN unlocked_set                          THEN 'LOCKED'
  WHEN EXISTS (passed graduation_attempt)                  THEN 'GRADUATED'
  WHEN passed_sub_exam_numbers = all numbers 1..N          THEN 'SUB_EXAMS_PASSED'
  WHEN EXISTS (score_record for stage)                     THEN 'IN_PROGRESS'
  ELSE                                                        'UNLOCKED'
END
```

- One query over `stage` LEFT JOINed against aggregated `score_record` and `graduation_attempt` plus the `unlocked_stages` set satisfies the existing "readable in one query" requirement; first-match-wins CASE mirrors the spec's ordered rules.
- `passed` is monotonic per sub-exam (already requirement 1), so the aggregation uses `EXISTS passed record per number`, not "latest attempt passed" — a failed retake after a pass never demotes a stage.
- Practice sessions (`practice_session`) are deliberately excluded from the query.
- **Alternative rejected:** computing the ladder in Java from several queries — that is what `DashboardService` does today and the spec forbids it ("one query").

### D3: `ABANDONED` status, closed lazily on the next start (supersede rule)

The stale `IN_PROGRESS` row is closed at the moment a new graduation starts, inside the same transaction: close old (if any) → insert new → commit. No scheduler, no timer, no resumable state (ADR-015 makes resume meaningless anyway).

- **Why `ABANDONED` over reusing `FAILED`:** the dashboard spec promises to show *all* retakes to the user; a `FAILED` row the user never played reads as a bug. Cost is one extra value in the status CHECK plus handling alongside `PASSED`/`FAILED`.
  - *Alternative rejected (acceptable fallback):* reuse `FAILED` — zero schema change, functionally correct, but paints phantom failures into user-visible history.
- **Why supersede-on-start over expiry job:** lazy, self-healing, no infra; a timer adds a scheduler dependency to close exactly one row type.
- **Concurrency:** the spec's "at most one `IN_PROGRESS` per user and stage" is currently application-code-only; two tabs clicking Start can both win. A **partial unique index** `(user_id, stage_id) WHERE status = 'IN_PROGRESS'` makes it a database guarantee; the supersede transaction closes the old row before the insert, so the index never fires spuriously.

### D4: `component_index` column + per-attempt unique index for dedupe

Add nullable `score_record.component_index INT` (1..3), populated only for `attempt_kind = 'GRADUATION'` rows, with a partial unique index on `(graduation_attempt_id, component_index)`.

- A retried component submit becomes an upsert-or-read: the unique index rejects the second write, the handler returns the stored row. This is what makes the spec's "no duplicate component records" promise deliverable — today nothing on the row distinguishes task 2 from a retry of task 2 (graduation rows must keep `sub_exam_number` NULL per V7's CHECK).
- Ranking requirement 5 ("more passing components, then higher minimum component accuracy") becomes arithmetic over distinguishable rows.
- **Alternative rejected:** client-supplied request id (the `practice_session` pattern) — puts idempotency mechanics in the browser for every graduation submit; the position is already known server-side and is user-visible ("task n of 3").
- **Out of scope, recorded:** proving the three Stage 5 types are *distinct* would additionally need `task_type` on the component row; that belongs to the `real-world-tasks` change.

## Risks / Trade-offs

- [Dashboard now violates two delta requirements (`CURRENT` as sixth status, multi-query derivation)] → Conforming rework is explicitly out of scope here but must land with Epic 2 implementation; tracked in proposal Impact so archive review doesn't miss it.
- [`ABANDONED` handling added everywhere `status` is consumed] → Only `DashboardService.stages()` reads the status column today (and only via `passed`); grep during implementation confirms the blast radius before the migration ships.
- [Partial unique index could reject a legitimate insert if supersede logic regresses] → The failure mode is a clear constraint violation, not silent data loss; unit-test the supersede transaction.
- [`component_index` NULL on SUB_EXAM rows widens the check-constraint surface] → Mirror V10's pattern: CHECK that `component_index` is NULL exactly when `attempt_kind = 'SUB_EXAM'`.
- [`currentStage` derived value could drift from `unlocked_stages` if written elsewhere] → Single-writer rule: only the unlock routine advances it; assert in code review / test that no other call site sets it.

## Migration Plan

One new Flyway migration (next version after `V10`), forward-only like the rest of `db/migration/`:

1. Extend `graduation_attempt.status` CHECK with `'ABANDONED'` (drop + re-add constraint, V9 style).
2. Add `score_record.component_index` + CHECK (NULL for `SUB_EXAM`, 1..3 for `GRADUATION`).
3. Partial unique index: `score_record (graduation_attempt_id, component_index)` where not null.
4. Partial unique index: `graduation_attempt (user_id, stage_id)` where `status = 'IN_PROGRESS'`.

All four are inert on an empty table — no data backfill, no application reads the new columns yet. Rollback = corrective migration (repo convention; no down-migrations).

## Open Questions

- Where the sub-exam submission endpoint lives (no spec owns its contract today). Recorded in the proposal as out of scope; answering it belongs to `scoring`/`ui` and does not change this delta.
- Whether component rows should carry `task_type` to prove Stage 5 distinctness. Deferred to the `real-world-tasks` implementation change; does not affect the dedupe mechanism chosen here.
