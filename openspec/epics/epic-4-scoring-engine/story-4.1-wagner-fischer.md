# Story 4.1 — Wagner-Fischer text evaluator
Points: 11 | Tool: opsx:plan + opsx:apply | ADRs: 016

Acceptance criteria
- [ ] Computes edit distance between target and typed text with an alignment trace.
- [ ] Output counts: correct characters, substitutions, insertions, deletions.
- [ ] accuracy = correct / target length, rounded to 4 decimal places for storage, compared unrounded.
- [ ] Case and diacritics are exact. "a" does not match "á".
- [ ] Empty typed input gives accuracy 0.
- [ ] Output includes the list of target characters that were missed, for ADR-003.
- [ ] Input length cap of 5000 characters to bound memory.
- [ ] Deterministic: same inputs always give the same output.
