# Story 3.4 — Content deduplication
Points: 5 | Tool: executing-plans | ADRs: 004, 012

Acceptance criteria
- [ ] Each user's last 5 served hashes are kept in a small table or session-scoped list.
- [ ] Hashes are SHA-256 of displayText.
- [ ] Hash list is rotated on each served exercise.
- [ ] Exercises are not stored, so only hashes persist (ADR-015).
