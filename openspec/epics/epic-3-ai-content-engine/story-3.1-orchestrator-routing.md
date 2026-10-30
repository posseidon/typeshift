# Story 3.1 — Orchestrator routing
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 004, 008, 012

Acceptance criteria
- [ ] Maps stage and exam type to the correct agent (Prose or Math).
- [ ] Reads top 5 weak keys and last 5 content hashes for the user, and nothing else (ADR-012).
- [ ] For Stage 5, selects a task type at random, avoiding the type used in the previous attempt (ADR-008).
- [ ] Calls the AIContentService interface only; no provider logic.
- [ ] Stateless between requests.
- [ ] Any failure after validation retries is raised as an AI error (ADR-017).
