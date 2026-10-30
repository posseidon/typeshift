# Story 2.2 — Stage unlock state machine
Points: 11 | Tool: opsx:plan + opsx:apply | ADRs: 013

States per stage: LOCKED, UNLOCKED, IN_PROGRESS, SUB_EXAMS_PASSED, GRADUATED.
Transitions
- Stage 1 starts UNLOCKED. Other stages start LOCKED.
- Passing the last sub-exam of Stage N moves Stage N to SUB_EXAMS_PASSED and unlocks Stage N+1 (OQ-1 assumption).
- Passing the graduation test moves Stage N to GRADUATED. Graduation does not change unlock state.
- Transitions are one-way. Retakes never move a stage back.

Acceptance criteria
- [ ] Invalid transitions are rejected and logged.
- [ ] Unlock is written in the same transaction as the triggering score_record.
- [ ] A stage cannot unlock twice.
- [ ] Stage 5 completion does not unlock anything.
- [ ] Current state for all stages is readable in one query for the stage map page.
