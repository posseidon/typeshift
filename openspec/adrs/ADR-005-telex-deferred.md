# ADR-005: Telex IME interception deferred
Status: Locked

**Decision:** Stage 4 Telex Tactician is removed from v1 scope. The Telex agent and TELEX envelope fields are not built.

**Rationale:** Browser IME composition events are platform-inconsistent. Reliable Telex evaluation is not achievable in a browser within v1 effort.

**Consequences:** Stage count drops from 6 to 5 (ADR-013). Vietnamese model quality is no longer a dev/prod concern (ADR-010). Revisit in v2 with a native desktop approach.
