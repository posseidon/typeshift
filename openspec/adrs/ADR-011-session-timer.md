# ADR-011: Session timer and duration
Status: Locked (revised)

**Decision:** The client-submitted durationSeconds is trusted and stored as-is.

**Rationale:** Single-user learning tool. The user has no motive to manipulate their own metrics.

**Consequences:** No server-side timestamps, no minimum-duration check. The session record stays simple.
