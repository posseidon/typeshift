# Open questions

- OQ-1 (RESOLVED): Passing every sub-exam in Stage N unlocks Stage N+1. Graduation is a separate confidence and record test and does not gate the next stage. Stage 5 unlocks nothing after it.
- OQ-2: Interface language. Current assumption: English only in v1. Affects Story 0.5.
- OQ-3: Prose input element. Current assumption: a plain contenteditable div for all stages, to keep one input component. Affects Story 5.4.
- OQ-4 (RESOLVED): no Telex/Vietnamese fields are encoded in v1. ADR-005 removes the Telex agent and TELEX envelope fields; ADR-004's envelope is PROSE or MATH only.
- OQ-5 (RESOLVED): Each Stage 5 sub-exam is one task. Graduation draws three distinct task types from the four available types, in sequence within one attempt; each task is scored separately and all three must pass.
- OQ-6: When Stage N unlocks Stage N+1, does `currentStage` advance automatically, or does it remain the user's active-stage choice? Affects Story 2.2 and the stage map.
- OQ-7: What happens to an abandoned `IN_PROGRESS` graduation attempt: resume it, or expire/close it? At most one active graduation attempt per user and stage is allowed. Affects Stories 2.3 and 2.4.
