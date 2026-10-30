# ADR-007: Real-time WPM/CPM display
Status: Locked

**Decision**
- Computed in the browser on a 10-second trailing window of correctly typed characters. WPM = (correct characters in window / 5) / (window seconds / 60). CPM = correct characters in window / (window seconds / 60).
- Display refreshes every 2 seconds.
- Visible in practice sessions. Hidden during graduation exams.
- Purely cosmetic. Nothing is sent to the backend except the final summary (ADR-002).
