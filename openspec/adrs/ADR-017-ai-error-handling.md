# ADR-017: AI error handling (revised)
Status: Locked (revised)

**Decision:** If the AI agent throws an error or fails validation after 3 attempts, the full stack trace is shown in the browser. The exam stops. The user reports the trace to the developer. After a fix is deployed, the user restarts the exam.

**Rationale:** Single-user tool with the developer in direct contact. Retry and fallback logic adds complexity without benefit.

**Consequences:** No fallback exercises, no retry button. No error is hidden from the user.
