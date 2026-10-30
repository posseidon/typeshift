# Story 2.3 — Graduation exam gating
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 013, 008

Acceptance criteria
- [ ] Graduation for a stage is offered only when that stage's sub-exams are all passed.
- [ ] Stage 1 to 4 graduation: one component task, pass at 95% accuracy.
- [ ] Stage 5 graduation: three tasks in sequence, all must pass (ADR-008).
- [ ] Failing any component ends the graduation attempt as FAILED. The user may start a new attempt.
- [ ] Graduation attempt rows are created at start and closed at finish (Story 1.7).
- [ ] Graduation is never available for a stage that is still LOCKED or UNLOCKED.
