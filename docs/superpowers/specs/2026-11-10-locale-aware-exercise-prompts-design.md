# Locale-aware exercise prompts

## Intent

Generate exercise text in the UI-selected locale (English, Hungarian, or Vietnamese), including locale-appropriate keyboard-row and diacritic practice, instead of always requiring Hungarian.

## Goals and boundaries

- Use the session-resolved Spring `Locale` when the asynchronous exercise docket is generated.
- Keep one prompt file per exercise; make language, keyboard characters, and diacritics dynamic where applicable.
- Preserve each exercise's scenario, length, output shape, and validation behavior.
- Keep arithmetic-only outputs as expressions using the existing decimal-comma format.
- Do not translate the UI, canned exercise catalog, or other AI features.

## Approaches

1. **One prompt per exercise with locale placeholders (selected):** provide locale details to the existing prompt factory and substitute them into the shared exercise prompts. This avoids maintaining three copies of every exercise while keeping locale-specific constraints explicit.
2. **A complete prompt set per locale:** easy to inspect in isolation, but triples prompt maintenance and risks the sets drifting apart.

## Design

Pass the controller's resolved `Locale` through exercise generation into the AI request. Map its language to the supported target language and locale prompt details: language name, keyboard-row characters, relevant diacritics, and email subject label. Supported mappings are English (`en`), Hungarian (`hu`), and Vietnamese (`vi`); an unsupported locale falls back to Vietnamese, matching the application's default locale.

Replace hard-coded Hungarian language and layout requirements in all exercise prompt resources with placeholders. Retain locale-independent task requirements and provide a sensible no-diacritic rule for English, Hungarian-specific QWERTZ/accent practice for Hungarian, and Vietnamese QWERTY plus Vietnamese diacritic practice for Vietnamese. Layout names and the Hungarian y/z swap instruction must only appear for Hungarian. Continue requiring math expressions without prose and preserve decimal commas because the expression validator only accepts commas.

## Verification

Add/adjust tests for locale mapping and prompt substitution, and verify the docket endpoint passes the session locale through generation. Run the focused tests and the project's standard test suite if practical.

## Implementation calls

- Use the resolved `Locale` rather than a new request parameter; locale selection already lives in the session.
- Keep existing prompt files as the single source of exercise scenarios; do not create translated prompt copies.
- Use Vietnamese as the fallback because it is the configured default UI locale.
