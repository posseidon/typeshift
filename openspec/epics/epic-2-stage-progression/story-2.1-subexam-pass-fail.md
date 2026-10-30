# Story 2.1 — Sub-exam pass or fail
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 013, 016

Acceptance criteria
- [ ] A sub-exam is passed when accuracy is 0.95 or higher.
- [ ] Stage 3 numeric sub-exams are passed only when every answer is exact.
- [ ] A sub-exam can be retaken immediately. No cooldown in v1.
- [ ] Each attempt writes one score_record (Story 1.5).
- [ ] A sub-exam already passed can be repeated for practice without changing its record status.
- [ ] Pass result is derived server-side from the submitted text, never from client claims.
