# Stateless Stage Map Design

## Intent

Make the stage map the application's entry point and let anyone choose an exercise directly, without accounts or a database. AI-generated typing exercises remain the product's central feature.

## Goals

- `/` and `/stage-map` show the stage map without sign-in.
- All five stages and every exercise in `CannedCatalog` are directly selectable; no account progression or locked stages.
- Keep Gemini generation and its existing content validation.
- Keep live typing feedback in the browser, but save no scores, exercise history, or learner progress.
- Remove the remaining authentication, persistence, and user-management wiring and dependencies.

## Non-goals

- Replacing AI generation with static exercises.
- Persisting results in a database, browser storage, or another service.
- Changing exercise content, AI provider, or language behavior.

## Constraints and assumptions

- Keep the current Spring Boot, Thymeleaf, and frontend layout conventions.
- `CannedCatalog` continues to define which stage/exercise choices are available and supplies generation parameters.
- Gemini configuration and `GEMINI_API_KEY` remain required for generating exercises.
- The user's existing deletions and edits are intentional and must be preserved; this change repairs the remaining references and tests rather than restoring deleted user-management code.

## Approaches considered

1. **Stateless AI generation (selected):** retain Gemini and remove account-based personalization, generated-content history, result submission, and auth. This preserves the main feature without persistence.
2. **Static canned exercise text:** remove AI generation as well as persistence. This is simpler at runtime but removes the product's main feature, so it is rejected.

## Design

- Add a small stage-map controller that serves both `/` and `/stage-map`, using the existing catalog to populate selectable exercises. Simplify the stage-map view to show all stages and their exercise links without mock certification/progress data.
- Make exercise lookup and docket generation public. Generate each selected exercise with Gemini without a username, database history, or personalized weak-key data; preserve current response validation and randomized Stage 5 task selection.
- Remove server-side result submission and exercise-ticket signing. Keep live speed, accuracy, timer, and exercise completion feedback in the browser; completing or leaving an exercise returns to the stage map. Nothing is persisted.
- Remove obsolete dashboard output and profile/logout navigation, along with remaining JPA, JDBC, Flyway, database-driver, and Spring Security dependencies/configuration.
- Update tests to cover the public stage-map and exercise flow, AI generation without persistence, and catalog validation; remove tests whose only purpose is deleted account, auth, or database behavior.

## Risks

- Generating an exercise on each docket request continues to consume Gemini quota and requires provider availability.
- Without authentication, any visitor can use the configured AI provider through the app; deployment should account for expected public traffic and provider costs.
- Scores and progress are intentionally not retained.
