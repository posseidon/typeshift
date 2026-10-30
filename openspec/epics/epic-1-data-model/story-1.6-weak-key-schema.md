# Story 1.6 — Weak key profile schema
Points: 2 | Tool: executing-plans | ADRs: 009, 012

Table: weak_key_profile: user_id, key_char, miss_count, updated_at. Primary key (user_id, key_char).

Acceptance criteria
- [ ] miss_count is cumulative. Each session merge adds to it (Story 4.5).
- [ ] Index supports "top 5 by miss_count for a user" in one query (ADR-012).
