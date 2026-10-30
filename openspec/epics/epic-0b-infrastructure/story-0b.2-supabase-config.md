# Story 0b.2 — Supabase connection configuration
Points: 2 | Tool: executing-plans | ADRs: 009

Acceptance criteria
- [ ] Production connection string comes from an environment variable.
- [ ] Only plain PostgreSQL features are used (ADR-009).
- [ ] Connection pool size is kept small to suit the free tier.
- [ ] The app fails fast with a clear message if the connection cannot be made.
