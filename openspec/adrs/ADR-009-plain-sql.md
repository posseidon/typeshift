# ADR-009: Plain SQL only, no Supabase extensions
Status: Locked

**Decision:** No Row Level Security, no Realtime subscriptions, no pgvector in v1. All schema changes are Flyway migrations written in standard PostgreSQL SQL.

**Rationale:** Single user, so RLS adds nothing. Plain SQL means the same migrations run on Docker Compose PostgreSQL and Supabase.

**Future path:** RLS and Realtime can be added in v2 if the app becomes multi-user.
