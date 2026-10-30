# ADR-001: Authentication mechanism
Status: Locked

**Decision:** Custom JWT issued by Spring Boot on login. No Supabase Auth.
- Token lifetime: 30 days.
- Token stored in an httpOnly cookie.
- Server-side invalidation via a `revoked_token` table (jti, revoked_at, expires_at). Expired rows are purged by a daily job.
- Single user. No OAuth, no roles beyond the owner.

**Rationale:** Single-user personal tool. Full control over token lifecycle with minimal moving parts.

**Consequences:** Token rotation and blocklist cleanup are managed in application code.
