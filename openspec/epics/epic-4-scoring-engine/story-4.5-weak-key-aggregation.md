# Story 4.5 — Weak key aggregation
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 003, 012

Acceptance criteria
- [ ] Each session's miss map is added to weak_key_profile in one transaction.
- [ ] Merge is additive: new count = stored count + session count.
- [ ] A key is inserted if not present.
- [ ] Merge is idempotent per submission: a repeated submission with the same request id is ignored.
- [ ] Top 5 query is served from this table (ADR-012).
