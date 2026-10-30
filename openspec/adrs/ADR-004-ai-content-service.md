# ADR-004: AIContentService interface contract
Status: Locked

**Purpose:** Given a generation request, return one validated exercise. The service knows nothing about users, sessions, or scoring.

**Request fields**
- stageId (1 to 5)
- examType: SUB_EXAM or GRADUATION
- focusKeys: up to 5 characters from ADR-012
- recentContentHashes: last 5 SHA-256 hashes served to this user
- languageTarget: HUNGARIAN, ENGLISH, or MIXED
- taskType (Stage 5 only): CHAT_BURST, EMAIL_REPLY, DOCUMENT_PARAGRAPH, CODE_COMMENT

**Response fields (envelope shared by all agents)**
- contentType: PROSE or MATH (TELEX deferred, ADR-005)
- displayText: text the user types
- expectedAnswer: PROSE = displayText; MATH = numeric result as a decimal string
- contentHash: SHA-256 of displayText
- estimatedDurationSeconds

**Validation (runs before the exercise is served)**
- PROSE: displayText non-empty, within stage length bounds, contains each focus key at least once where possible.
- MATH: expectedAnswer is recomputed in Java with BigDecimal. Mismatch means the exercise is rejected.
- ALL: contentHash not in recentContentHashes. A duplicate is rejected and regenerated, up to 3 attempts.
- Any failure after 3 attempts is an AI error (ADR-017).

**Provider abstraction:** Interface with two implementations: AzureOpenAIContentService (prod) and OllamaContentService (dev). A Spring profile selects one. Both return the same envelope.

**Note:** Exercises are not persisted (ADR-015).
