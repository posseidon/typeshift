# Open questions

- OQ-1 (RESOLVED): Passing every sub-exam in Stage N unlocks Stage N+1. Graduation is a separate confidence and record test and does not gate the next stage. Stage 5 unlocks nothing after it.
- OQ-2: Interface language. Current assumption: English only in v1. Affects Story 0.5.
- OQ-3: Prose input element. Current assumption: a plain contenteditable div for all stages, to keep one input component. Affects Story 5.4.
- OQ-4 (RESOLVED): no Telex/Vietnamese fields are encoded in v1. ADR-005 removes the Telex agent and TELEX envelope fields; ADR-004's envelope is PROSE or MATH only.
- OQ-5 (RESOLVED): Each Stage 5 sub-exam is one task. Graduation draws three distinct task types from the four available types, in sequence within one attempt; each task is scored separately and all three must pass.
- OQ-6 (RESOLVED): `currentStage` auto-advances — it always equals the highest unlocked stage and moves in the same transaction as the unlock that raises it (graduation does not move it). Decided by change `tighten-stage-progression-spec`. Affects Story 2.2 and the stage map.
- OQ-7 (RESOLVED): abandoned `IN_PROGRESS` graduation attempts are superseded — starting a new graduation closes any still-IN_PROGRESS attempt for that user and stage as `ABANDONED` in the same transaction, instead of resuming or expiring it on a timer. Decided by change `tighten-stage-progression-spec`. Affects Stories 2.3 and 2.4.
