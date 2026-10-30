# ADR-015: Exercise lifecycle (revised)
Status: Locked (revised)

**Decision:** Exercises are not persisted. An exercise is generated per request, served in the response, and lives only in the browser until submission. Only the score record and session summary are written to the database.

**Consequences:** No content cache table. If the browser closes mid-exercise, the exercise is lost and the user starts again.
