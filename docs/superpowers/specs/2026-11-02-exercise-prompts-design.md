# Exercise prompts in classpath resources

**Intent:** Make the prompt instructions editable as Markdown resources (`.md`) under `typeshift-backend/src/main/resources/prompts/exercises/`, with one resource for each of the 18 catalog exercises.

**Approach:** Name each exercise resource by stage and exam key, and select it using the existing stage, exam type, and sub-exam number in `AiRequest`. Keep the shared language and JSON-output contract in `AiPromptFactory`. Stage 3 substitutes its sub-exam number. Because Stage 5 selects a task type dynamically, store its four existing task-format instructions in four reusable resources in the same directory and load the matching one; this avoids duplicating them across five Stage 5 exercise resources.

**Alternatives:** (1) Keep all prompts in Java — rejected because prompts should be resource-managed. (2) Duplicate the shared response contract and Stage 5 task instructions in every exercise file — rejected because shared content would drift. (3) Keep one exercise resource per catalog entry plus four Stage 5 task-format resources, with shared response rules in Java — recommended and selected.

**Constraints/non-goals:** Preserve current prompt wording, task-type selection, stage validation, and output contract. Do not change the request model or add dependencies.

**Verification:** Add failing-first coverage for all 18 exercise resource selections, all Stage 5 task-format resources, and existing invalid-stage/missing-task behavior; run the backend test suite.
