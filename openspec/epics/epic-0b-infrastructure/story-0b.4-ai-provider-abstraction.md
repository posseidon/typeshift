# Story 0b.4 — AIContentService provider abstraction
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 004, 010, 012, 017

Acceptance criteria
- [ ] One interface defines the request and response envelope in ADR-004.
- [ ] Two implementations exist: Azure OpenAI and Ollama.
- [ ] A profile setting selects the implementation. Only one is active at runtime.
- [ ] Both implementations accept the same request and return the same envelope.
- [ ] Model name and endpoint are read from environment variables.
- [ ] Provider errors are raised as a single exception type that carries the full stack trace (ADR-017).
- [ ] No provider-specific types leak outside the implementation.
