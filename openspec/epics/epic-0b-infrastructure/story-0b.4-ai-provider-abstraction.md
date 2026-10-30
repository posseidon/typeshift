# Story 0b.4 — AIContentService provider abstraction
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 004, 010, 012, 017

Boundary: this story owns the interface and its two implementations; epic 3 owns orchestration and content logic.

Acceptance criteria
- [ ] One interface defines the request and response envelope in ADR-004.
- [ ] The envelope contains no Telex or Vietnamese fields (ADR-005).
- [ ] Two implementations exist: Azure OpenAI and Ollama.
- [ ] A profile setting selects the implementation. Only one is active at runtime.
- [ ] Both implementations accept the same request and return the same envelope.
- [ ] Model name and endpoint are read from environment variables.
- [ ] Provider errors are raised as a single exception type that carries the full stack trace (ADR-017).
- [ ] No provider-specific types leak outside the implementation.
