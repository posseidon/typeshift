# ADR-002: WPM/CPM computation location
Status: Locked

**Decision:** WPM and CPM are computed entirely in the browser. The backend receives only the final summary at session end: wpm, cpm, durationSeconds, characterCount.

**Rationale:** Avoids high-frequency backend calls and keeps Supabase bandwidth low.

**Consequences:** Speed metrics come from the client clock. They are advisory. Accuracy, the exam gate, is computed server-side from submitted text (ADR-016).
