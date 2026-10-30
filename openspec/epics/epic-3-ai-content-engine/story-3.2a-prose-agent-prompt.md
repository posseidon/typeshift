# Story 3.2a — Prose agent system prompt
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 004, 012, 013

Acceptance criteria
- [ ] One fixed system prompt covers Stages 1, 2, 4, and 5 task formats.
- [ ] Stage 1 output: natural prose using the focus keys deliberately.
- [ ] Stage 2 output: text with punctuation, symbols, and brackets from the Symbol Sorcerer set.
- [ ] Stage 4 output: Hungarian text with the outer-right accents (á é í ó ö ő ú ü ű) present.
- [ ] Stage 5 output: format per taskType (chat, email with subject, document paragraph, code comment).
- [ ] Output is JSON matching the envelope in ADR-004, with no surrounding prose.
- [ ] Dynamic part of the prompt stays under 200 tokens (ADR-012).
