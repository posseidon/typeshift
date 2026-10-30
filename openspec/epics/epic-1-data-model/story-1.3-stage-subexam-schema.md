# Story 1.3 — Stage reference schema
Points: 2 | Tool: executing-plans | ADRs: 009, 013

Table: stage: id (1 to 5), name, sub_exam_count, graduation_name.

Acceptance criteria
- [ ] Seeded by a Flyway migration with the five rows in constants/stage-map.md.
- [ ] Seed rows are immutable in application code.
- [ ] sub_exam_count values are 3, 2, 2, 2, 4.
