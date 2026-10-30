# Story 3.5 — Weak key injection
Points: 5 | Tool: executing-plans | ADRs: 012, 003

Acceptance criteria
- [ ] Top 5 keys by miss_count are read from weak_key_profile at request time.
- [ ] Keys are passed as a short list, not as raw counts or history.
- [ ] If the user has no misses yet, the list is empty and the request still succeeds.
- [ ] Excluded from the prompt: any key with a miss count below 2.
