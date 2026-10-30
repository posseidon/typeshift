# ADR-010: Dev model and prod provider
Status: Locked

**Decision**
- Dev: Ollama with qwen2.5:14b. Fallback qwen2.5:7b on machines with 8 GB RAM.
- Prod: Azure OpenAI (GPT-4o class deployment).
- Model name is an environment variable, never hardcoded.

**Accepted limitation:** Dev content quality for Hungarian is lower than prod. Dev is for functional testing only.

**Staging requirement:** Stage 4 (Hungarian accents) and Stage 5 content quality must be checked against Azure OpenAI before those epics are accepted.
