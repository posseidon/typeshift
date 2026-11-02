# Exercise prompt quality design

**Intent:** Improve all generated exercise content for learners with B1 Hungarian by making the 18 prompts clear, varied, and specific to the supplied station topics, while retaining the existing 4 reusable Stage 5 task-format resources.

**Approach:** Enrich the existing Markdown resources rather than changing the resource layout. Tailor each exercise template to its listed topic and bounds; keep dynamically selected Stage 5 format rules in the four task resources. Stage 5 exercise resources set the scenario, while the randomly selected task-format resource controls the output form. Add weak-key characters to `AiPromptFactory`'s shared instructions when present. For math exercises, pass only focus characters representable by the math grammar to both the prompt and validation; letter keys cannot appear in a valid ledger expression. Keep the current response envelope and one-call generation behavior unchanged.

**Content map (owner-provided):**

| Station | Sub-exam topics | Graduation topic |
|---|---|---|
| 1 | Home row grounding; top row and QWERTZ swap; bottom row baseline | 3-minute continuous QWERTZ passage |
| 2 | Standard punctuation and hyphenation; AltGr symbols, brackets, and quotes | Formatted technical-support ticket |
| 3 | Numeric home row and keypad orientation; ledger arithmetic and balancing | Cashier Rush math evaluation |
| 4 | Accent grounding (`á é í ó ö ő ú ü ű`); formal wording drill | Formal Hungarian request letter |
| 5 | Informal bureau chat dispatch; inter-departmental memorandum/email; official petition and business dossier; system technical code annotation | Three random tasks in sequence |

**Language:** Use clear B1-level Hungarian: familiar vocabulary, direct sentence structure, one main idea per sentence, and natural transitions. Avoid idioms, ornate prose, and unexplained specialist terms. Keep formal genres polite and credible without making them harder to read.

**Specification constraints:** Follow ADR-013 and the supplied topics for all 18 catalog identities. Preserve Hungarian QWERTZ practice and each task format. Stage 3 output must fit the Java expression parser: one expression using only `+`, `-`, `*`, `/`, decimal commas, and a final ` = ?`; sub-exam difficulty scales with its number. Stage 4 must use the required accented character set. Stage 5 must follow the dynamically selected task-type contract and existing length bounds. Keep each resource concise and the stage-specific instruction under 200 tokens. Stage 5 graduation is specified as three random tasks in sequence, but the current generator returns one selected task per request; prompt only for that one task and frame it as one part of the larger requirement.

**Alternatives:** (1) Enrich the existing 18 exercise resources and four shared task resources, with weak-key guidance in Java — selected because it preserves the dynamic task selection without duplicated instructions. (2) Put complete prompts, including each task format, in every exercise resource — rejected because Stage 5 task formats are selected dynamically and duplicated text could drift. (3) Leave weak keys implicit — rejected because the service validator rejects output that omits them.

**Constraints/non-goals:** Prompt content only; do not alter the exercise count, catalog, response fields, random task selection, current single-task generation, length validation, math evaluator, or graduation orchestration. Keep all prose focus keys; for math, retain only single characters accepted by the evaluator. Use that same filtered list for prompting and validation. Do not include recent content hashes or typing history in the prompt. No production Java changes or dependency changes. A test-only constructor call-site alignment is allowed so the existing Azure tests compile against the current service signature.

**Verification:** Extend `AiPromptFactoryTest` to cover the prompt content for all 18 exercises, all four task formats, and present/empty focus-key handling. Run the backend verification lifecycle; no live Azure call is required.
