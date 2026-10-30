# Ollama (dev AI provider)

Install Ollama natively and run `ollama serve` — not a Compose service.

- Default model: `qwen2.5:14b` (`ollama pull qwen2.5:14b`).
- 8 GB RAM fallback: **Mistral 7B** — set `OLLAMA_MODEL=mistral`.
- Model name is the `OLLAMA_MODEL` environment variable, never hardcoded (ADR-010).
- Base URL is `OLLAMA_BASE_URL` (default `http://localhost:11434`).

The dev profile checks model availability at startup and warns if it is missing.
