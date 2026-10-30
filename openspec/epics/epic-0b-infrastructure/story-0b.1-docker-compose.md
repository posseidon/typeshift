# Story 0b.1 — Docker Compose PostgreSQL
Points: 3 | Tool: writing-plans then executing-plans | ADRs: 009

Acceptance criteria
- [ ] A Compose file defines one PostgreSQL service with a named volume so data survives restarts.
- [ ] Credentials come from a local env file that is git-ignored.
- [ ] Port is exposed on localhost only.
- [ ] Health check passes before the app connects.
- [ ] A documented command starts and stops the database.
