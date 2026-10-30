# Exercise payload contract
Mirrors ADR-004. Text description only.

Request: stageId, examType, focusKeys (max 5), recentContentHashes (max 5), languageTarget, taskType (Stage 5 only).
Response: contentType (PROSE or MATH), displayText, expectedAnswer, contentHash, estimatedDurationSeconds.
Validation: PROSE length and focus-key checks. MATH recomputed with BigDecimal. Duplicate hashes rejected. Max 3 attempts, then AI error (ADR-017).
