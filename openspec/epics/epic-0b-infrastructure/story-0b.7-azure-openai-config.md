# Story 0b.7 — Azure OpenAI configuration
Points: 3 | Tool: executing-plans | ADRs: 010

Acceptance criteria
- [ ] Endpoint, API key, and deployment name are environment variables.
- [ ] Setup notes list the quota prerequisite for a GPT-4o class deployment.
- [ ] A connectivity check runs on demand from a dev-profile-only route (prod has a single user and no developer route), not from the public UI.
- [ ] The connectivity check lives in the health domain.
