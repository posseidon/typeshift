# Epic 0b — Gaps and open questions

Parking lot for things to clarify before or while building epic 0b. Return here before planning each story.

## Resolved this session
- ADR list corrected to `004, 009, 010, 012, 017` (was missing 012/017, carried a stray 014).
- SQLite is a throwaway prototype DB from epic 0; dropped when 0b lands. PostgreSQL is the only database. No data migration.
- Flyway owns the schema: `ddl-auto=validate`, V1 empty marker, schema history table created by Flyway itself.

## Defaulted this session (re-check when planning)
- 0b.6: Ollama runs as a native install (`ollama serve`), not a Compose service.
- 0b.7: connectivity check lives in the `health` domain, gated by a profile flag.
- 0b.4: this story owns the interface + two implementations; epic 3 owns orchestration/content.
- 0b.5: same migration set must run clean on both Compose Postgres and Supabase (verification AC added).

## Answered after review

**0b-Q1 — What does "prod" mean for the app process?**
Answer: prod is the prod Spring profile — the same app run with production environment settings (Supabase + Azure OpenAI). It is not a hosted deployment; the app runs on the owner's machine in both profiles. No cloud-deployment story is needed.

**0b-Q2 — "Developer-only route" vs single-user prod.**
Answer: developer is a role that exists only in the dev environment. Prod has a single user (the owner) and no developer route. So the connectivity check route is dev-profile-only; ADR-001 needs no change.

**0b-Q3 — Prod secret storage.**
Answer: prod secrets use the same git-ignored `.env` mechanism on the owner's machine (a `.env.production` if separate files are preferred). No hosted secret store.

**0b-Q4 — Ollama fallback: manual or automatic?**
Answer: fallback model is Mistral 7B (replaces qwen2.5:7b), chosen via the model env var. ADR-010 and 0b.6 updated. Mechanism is manual (env var); no auto-detect.

**0b-Q5 — Supabase free-tier connection specifics.**
Answer: use the minimal free-tier settings — small pool, direct connection (5432). Single-user app, so the pooler is unnecessary. Existing 0b.2 AC ("pool size kept small") covers it; no change.

**0b-Q6 — How is "same migrations on both" verified?**
Answer: manual run by the developer (no CI). Story 0b.5's verification AC stands as a manual step.

**0b-Q7 — Telex/Vietnamese residue in the envelope (cross-ref OQ-4).**
Answer: do not encode Telex/Vietnamese fields at all. Already enforced by ADR-005 (Telex agent and TELEX envelope fields are not built) and ADR-004 (contentType is PROSE or MATH only). Guard added to 0b.4.

**0b-Q8 — Build order: 0b.4 before 0b.6 / 0b.7.**
Answer: keep current order; 0b.4 ships against placeholder env vars for model/endpoint.

## Open questions

None remaining.
