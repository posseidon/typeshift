# ai-content Specification

## Purpose

Generate validated, personalised typing exercises: route each request to the right agent, produce prose or math content through an AI provider, validate and deduplicate it, and inject the user's weak keys. (Epic 0b.4 provider abstraction + Epic 3 orchestration.)

## Requirements

### Requirement: AI provider abstraction with two implementations
A single interface SHALL define the ADR-004 request/response envelope (no Telex or Vietnamese fields), implemented by both an Azure OpenAI and an Ollama provider, with a profile selecting exactly one active implementation. Model name and endpoint SHALL come from environment variables, provider errors SHALL surface as a single exception type carrying the full cause, and no provider-specific types SHALL leak outside the implementations.

#### Scenario: Profile selects the provider
- **WHEN** a profile is active
- **THEN** exactly one provider serves requests through the shared interface

#### Scenario: Provider failure is wrapped
- **WHEN** a provider call fails
- **THEN** it is raised as the single provider exception type with its cause attached

> **Status: IMPLEMENTED** — `AIContentService` with `OllamaContentService` (dev) and `AzureOpenAIContentService` (prod) exist.

### Requirement: Orchestrator routes requests to the correct agent
An orchestrator SHALL map stage and exam type to the correct agent (Prose or Math), read only the user's top 5 weak keys and last 5 content hashes, and for Stage 5 select a task type at random while avoiding the previous attempt's type. It SHALL call the AI provider interface only (no provider logic), stay stateless between requests, and raise an AI error after validation retries are exhausted.

#### Scenario: Request is routed by stage and exam type
- **WHEN** an exercise is requested for a given stage and exam type
- **THEN** the matching agent handles it and calls only the provider interface

#### Scenario: Stage 5 avoids the previous task type
- **WHEN** a Stage 5 exercise is requested
- **THEN** a task type is chosen at random, excluding the type served on the previous attempt

> **Status: PLANNED** — no orchestrator or agent routing exists; exercises are currently served from a static canned catalog.

### Requirement: Prose agent produces stage-appropriate text
A fixed prose agent system prompt SHALL cover Stages 1, 2, 4, and 5: Stage 1 natural prose using the focus keys, Stage 2 punctuation/symbols/brackets, Stage 4 Hungarian outer-right accents, and Stage 5 formatted per the selected task type. Output SHALL be JSON matching the ADR-004 envelope with no surrounding prose, and the dynamic prompt portion SHALL stay under 200 tokens.

#### Scenario: Stage-appropriate prose is generated
- **WHEN** the prose agent is invoked for a stage
- **THEN** it returns envelope JSON whose display text matches that stage's format

> **Status: PLANNED** — no prose agent prompt exists.

### Requirement: Prose response validation and retry
Generated prose SHALL be rejected if it is not valid JSON or is missing envelope fields, if displayText is empty or outside stage length bounds, if a requested focus key does not appear, or if its content hash duplicates a recent hash. Validation SHALL retry up to 3 attempts, then raise an AI error.

#### Scenario: Invalid generation is retried then rejected
- **WHEN** the provider returns invalid or duplicate content across retries
- **THEN** after 3 attempts an AI error is raised

> **Status: PLANNED** — no validation, retry, or hashing logic exists.

### Requirement: Math agent produces re-verifiable arithmetic
A math agent SHALL generate ledger-style arithmetic using only numpad-supported operators (+ - * / and decimal comma), each exercise being a displayText expression plus an expected answer, in ADR-004 JSON with contentType MATH, scaling difficulty with sub-exam number. The generated expression SHALL be re-parsed in Java and recomputed with BigDecimal; a mismatch with the agent's answer, or unparseable/non-terminating input, SHALL reject the exercise, and the Java-computed result is what the scorer uses.

#### Scenario: Generated math is re-verified
- **WHEN** the math agent returns an expression and answer
- **THEN** Java recomputes it with BigDecimal and rejects it on any mismatch

> **Status: PLANNED** — no math agent or runtime re-verification exists (only a test-time check of canned data).

### Requirement: Content deduplication by hash
The last 5 served content hashes per user SHALL be kept (SHA-256 of displayText), rotated on each served exercise. Exercises themselves SHALL NOT be persisted — only hashes.

#### Scenario: Repeated exercise is avoided
- **WHEN** a newly generated exercise's hash matches a recent hash
- **THEN** it is rejected and regenerated

> **Status: PLANNED** — no hash store or dedup logic exists; canned content hashes are placeholders.

### Requirement: Weak-key injection
At request time the top 5 keys by miss count SHALL be read from the weak-key profile and passed as a short list (not raw counts or history), excluding keys with a miss count below 2. If the user has no qualifying misses the list is empty and the request still succeeds.

#### Scenario: Weak keys personalise the exercise
- **WHEN** a user has qualifying missed keys
- **THEN** they are included as focus keys in the generation request

#### Scenario: No misses still succeeds
- **WHEN** a user has no keys with a miss count of 2 or more
- **THEN** the request proceeds with an empty focus-key list

> **Status: PLANNED** — weak-key injection into generation is not wired (the read query exists only for the dashboard).
