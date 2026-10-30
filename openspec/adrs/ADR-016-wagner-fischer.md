# ADR-016: Prose accuracy algorithm
Status: Locked

**Decision:** Accuracy is computed by Wagner-Fischer edit distance between target and typed text, retaining per-position alignment so each error is classified as substitution, insertion, or deletion.

- accuracy = correctly aligned characters / target character count
- Pass threshold: accuracy >= 0.95
- Insertions count as errors. Trailing untyped target characters count as deletions.
- Case and diacritics are compared exactly. The user must type á, not a.
- Output per attempt: correct, substitutions, insertions, deletions, accuracy, and a list of missed characters for ADR-003.

**Numeric results (Stage 3)** use exact BigDecimal equality, not edit distance (Story 4.2).
