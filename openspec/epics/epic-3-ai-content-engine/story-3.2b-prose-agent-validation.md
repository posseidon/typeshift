# Story 3.2b — Prose response validation
Points: 5 | Tool: opsx:plan + opsx:apply | ADRs: 004, 017

Acceptance criteria
- [ ] Rejects output that is not valid JSON or is missing envelope fields.
- [ ] Rejects empty displayText and text outside stage length bounds.
- [ ] Checks that each focus key appears at least once when present in the request.
- [ ] Computes contentHash and rejects a hash found in recentContentHashes.
- [ ] Up to 3 attempts. After that, raises an AI error (ADR-017).
