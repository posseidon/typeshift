# Typing App — OpenSpec Specification Set

Single-user Java/Spring Boot typing-learning app. Five stages, Thymeleaf UI, PostgreSQL (Docker Compose in dev, Supabase in prod), Ollama in dev and Azure OpenAI in prod.

## Folder layout
- adrs/ — 18 locked Architecture Decision Records. Every story references these by number.
- constants/ — stage map, payload contract, tool and complexity guide.
- epics/ — one folder per epic, with epic.md and one file per story.
- OPEN-QUESTIONS.md — items that still need a decision before their stories are built.

## Workflow per epic
1. Read the epic.md and the ADRs it references.
2. Build stories in listed order using the tool stated on each story.
3. Stories above Fibonacci 11 are already split.
4. Commit the epic folder before starting the next epic.
