# Story 6.1 — Task pool and random selection
Points: 5 | Tool: executing-plans | ADRs: 008

Acceptance criteria
- [ ] Pool holds exactly four task types: CHAT_BURST, EMAIL_REPLY, DOCUMENT_PARAGRAPH, CODE_COMMENT.
- [ ] Selection is uniform random, excluding the type used on the previous attempt.
- [ ] Selected type is passed to the Prose agent (Story 3.2a).
