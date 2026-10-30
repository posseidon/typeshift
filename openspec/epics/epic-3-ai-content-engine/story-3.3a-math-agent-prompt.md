# Story 3.3a — Math agent system prompt
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 004, 006, 013

Acceptance criteria
- [ ] Generates ledger-style arithmetic using only operators the numpad supports: + - * / and decimal comma.
- [ ] Each exercise is a displayText expression plus expectedAnswer.
- [ ] Output is JSON matching ADR-004 with contentType MATH.
- [ ] Decimal values use comma, matching Hungarian convention.
- [ ] Difficulty scales with stage sub-exam number.
