-- Story 1.3: static stage reference data (ADR-013). Seeded by migration only;
-- no application path mutates it, and NO ACTION FKs protect the rows.
CREATE TABLE stage (
    id BIGINT PRIMARY KEY CHECK (id BETWEEN 1 AND 5),
    name            VARCHAR(64)  NOT NULL,
    sub_exam_count  INTEGER      NOT NULL CHECK (sub_exam_count > 0),
    graduation_name VARCHAR(128) NOT NULL
);

INSERT INTO stage (id, name, sub_exam_count, graduation_name) VALUES
    (1, 'QWERTZ Core',       3, '3-minute QWERTZ passage'),
    (2, 'Symbol Sorcerer',   2, 'Technical support ticket'),
    (3, 'Numpad and Math',   2, 'Cashier Rush'),
    (4, 'Hungarian Accents', 2, 'Formal Hungarian request letter'),
    (5, 'Real-World Tasks',  4, 'Three random tasks in sequence');
