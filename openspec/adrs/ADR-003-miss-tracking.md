# ADR-003: Keystroke miss tracking
Status: Locked

**Decision:** The browser tracks per-character miss counts during a session and sends an aggregated frequency map at session end, for example {"á": 4, "ő": 7, "z": 2}.

**Rationale:** No per-keystroke storage. Avoids database growth on Supabase free tier.

**Consequences:** No keystroke-by-keystroke replay. The miss heatmap is cumulative across sessions. The backend merges each map into weak_key_profile (Story 4.5).
