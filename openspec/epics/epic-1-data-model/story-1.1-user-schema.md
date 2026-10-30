# Story 1.1 — User and revoked token schema
Points: 3 | Tool: executing-plans | ADRs: 001, 009

Tables
- app_user: id, username (unique), password_hash, display_name, created_at, updated_at.
- revoked_token: jti (primary key), user_id, revoked_at, expires_at.

Acceptance criteria
- [ ] Username uniqueness enforced by the database, not only in code.
- [ ] revoked_token has an index on expires_at for the daily purge.
