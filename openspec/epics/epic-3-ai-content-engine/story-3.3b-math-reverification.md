# Story 3.3b — Math arithmetic re-verification
Points: 5 | Tool: opsx:plan + opsx:apply | ADRs: 004, 016

Acceptance criteria
- [ ] Parses displayText into operands and operators in Java. Unparseable text is rejected.
- [ ] Recomputes the result with BigDecimal, never double.
- [ ] Compares to the agent's expectedAnswer at exact equality. Mismatch rejects the exercise.
- [ ] The Java result, not the agent's, is what the scorer uses.
- [ ] Division that does not terminate is rejected, or rounded to a rule fixed in the plan.
