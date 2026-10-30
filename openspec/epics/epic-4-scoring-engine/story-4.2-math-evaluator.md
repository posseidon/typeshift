# Story 4.2 — Math result evaluator
Points: 5 | Tool: opsx:plan + opsx:apply | ADRs: 003, 006, 016

Acceptance criteria
- [ ] Parses the user's numpad input, accepting comma as decimal separator.
- [ ] Compares with BigDecimal compareTo, so 4,50 equals 4,5.
- [ ] Invalid or empty input is a miss, not an exception.
- [ ] Stage 3 pass requires every answer correct.
- [ ] Missed numpad keys (main-keyboard digits, see ADR-006) are counted for ADR-003.
