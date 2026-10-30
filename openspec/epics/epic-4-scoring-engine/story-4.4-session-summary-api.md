# Story 4.4 — Session summary endpoint
Points: 5 | Tool: executing-plans | ADRs: 002, 011

Acceptance criteria
- [ ] Accepts wpm, cpm, durationSeconds, characterCount, and the miss map from ADR-003.
- [ ] Validates ranges: non-negative numbers, miss map keys are single characters.
- [ ] Writes one practice_session row.
- [ ] Trusts client duration as decided in ADR-011.
- [ ] Triggers the weak key merge (Story 4.5).
