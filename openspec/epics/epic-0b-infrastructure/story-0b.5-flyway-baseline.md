# Story 0b.5 — Flyway baseline migration
Points: 3 | Tool: executing-plans | ADRs: 009

Acceptance criteria
- [ ] Flyway runs on startup in both profiles.
- [ ] Migrations use only standard PostgreSQL SQL (ADR-009).
- [ ] Migration V1 creates an empty baseline with a schema history table.
- [ ] A failed migration stops startup with a clear error.
