# Story 0b.6 — Ollama local setup
Points: 2 | Tool: executing-plans | ADRs: 010

Acceptance criteria
- [ ] Setup notes state the default model qwen2.5:14b and the 8 GB fallback Mistral 7B.
- [ ] Ollama runs as a native install (ollama serve), not a Compose service.
- [ ] Model name is an environment variable.
- [ ] A startup check reports whether the configured model is available and warns if not.
