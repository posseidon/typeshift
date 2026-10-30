# Story 0b.3 — Spring Boot profiles
Points: 3 | Tool: executing-plans | ADRs: 009, 010

Acceptance criteria
- [ ] A dev profile points at Docker Compose PostgreSQL and Ollama.
- [ ] A prod profile points at Supabase and Azure OpenAI.
- [ ] Every secret and endpoint is an environment variable.
- [ ] The active profile is shown on the startup log.
- [ ] Dev and prod profiles never share a database.
