# Story 0b.5 — Flyway baseline migration
Points: 3 | Tool: executing-plans | ADRs: 009

Acceptance criteria
- [ ] Flyway runs on startup in both profiles.
- [ ] Migrations use only standard PostgreSQL SQL (ADR-009).
- [ ] Hibernate ddl-auto is set to validate so Flyway owns the schema; the SQLite prototype DB from epic 0 is removed.
- [ ] Flyway creates its own schema history table; V1 is an empty marker (real schema arrives in epic 1).
- [ ] A failed migration stops startup with a clear error.
- [ ] The same migration set runs clean on both Docker Compose PostgreSQL and Supabase.
