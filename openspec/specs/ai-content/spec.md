## Purpose

Generate stage-appropriate typing content through one Gemini-backed service.

### Requirement: Gemini generation
The application SHALL use `GeminiContentService` backed by Spring AI's Google GenAI starter and configured `ChatModel` in every runtime profile. The model SHALL be `gemini-3.5-flash-lite`, and the Gemini Developer API key SHALL be supplied through `GEMINI_API_KEY`. Application code SHALL NOT construct provider REST requests or use runtime provider selection.

The service SHALL return the AI response envelope and wrap transport, serialization, and response errors in `AiProviderException`, preserving the cause.

### Requirement: Stage-aware exercise generation
Each exercise request SHALL select a prompt according to the stage-native content format:

| Stage | Generated format |
|---|---|
| 1 | Hungarian QWERTZ practice prose |
| 2 | Symbol-rich technical-support ticket |
| 3 | Ledger-style arithmetic |
| 4 | Formal Hungarian request letter with accented characters |
| 5 | Chat burst, email reply, document paragraph, or code comment |

Stage 5 SHALL select a task type other than the previously served task type when one is available. The request SHALL include the stage, exam type and sub-exam number, up to five weak keys, the last five content hashes, the target language, and the task type when applicable. The stage-specific prompt instruction SHALL remain under 200 tokens and request only one JSON response envelope without surrounding prose.

The model response SHALL contain `contentType`, `displayText`, `expectedAnswer`, and `estimatedDurationSeconds`. The application SHALL compute the SHA-256 `contentHash` from `displayText`; the model SHALL NOT generate the hash. For prose, `expectedAnswer` SHALL equal `displayText`. The stage's expected content type SHALL be enforced.

### Requirement: Generated-content validation
Generated content SHALL be rejected when the response is malformed, required fields are missing, `displayText` is empty or longer than 4,000 characters, a requested focus key is absent, or its SHA-256 content hash duplicates one of the user's last five served hashes. An exercise request SHALL make exactly one model call; invalid output SHALL raise an AI error without automatic regeneration.

### Requirement: Math generation and verification
Stage 3 SHALL generate ledger-style arithmetic using only numpad-supported operators (`+`, `-`, `*`, `/`) and decimal commas. Difficulty SHALL scale with the sub-exam number. The expression SHALL be parsed and recomputed in Java using `BigDecimal`; mismatched answers, unparseable expressions, division by zero, and non-terminating division SHALL be rejected. The Java-computed result SHALL be used for scoring.

### Requirement: Per-user content deduplication
The last five served content hashes per user SHALL be retained as SHA-256 hashes of `displayText` and rotated whenever a generated exercise is served. Exercise text and answers SHALL NOT be persisted; only the hashes and task type needed for selection SHALL be stored.

### Requirement: Weak-key personalization
At request time, the top five keys by miss count SHALL be read from the weak-key profile, excluding keys with fewer than two misses. Only the keys—not counts or typing history—SHALL be sent to the model. An empty focus-key list SHALL still produce a valid exercise.
