# ADR-012: Bounded agent context
Status: Locked

**Decision:** Each agent call receives only:
- stage metadata and exam type
- top 5 weak keys by cumulative miss count (from weak_key_profile)
- last 5 content hashes served to the user (deduplication)

No session history, no previous exercise text, no score history.

**Budget:** Fixed system prompt per agent. Dynamic portion under 200 tokens. Total under 1000 tokens per call.
