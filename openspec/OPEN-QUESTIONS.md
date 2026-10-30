# Open questions

- OQ-1: Does passing a stage's graduation test also gate Stage N+1, or does Stage N+1 unlock on sub-exams alone? Current assumption: sub-exams alone unlock; graduation is a confidence and record test. Affects Story 2.2 and 2.3.
- OQ-2: Interface language. Current assumption: English only in v1. Affects Story 0.5.
- OQ-3: Prose input element. Current assumption: a plain contenteditable div for all stages, to keep one input component. Affects Story 5.4.
- OQ-4 (RESOLVED): no Telex/Vietnamese fields are encoded in v1. ADR-005 removes the Telex agent and TELEX envelope fields; ADR-004's envelope is PROSE or MATH only.
- OQ-5: Stage 5 sub-exam vs graduation scoring. Current assumption: each sub-exam is one task; graduation is three tasks in sequence. Affects Story 6.2.
