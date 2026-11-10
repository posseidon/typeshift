# Stitch Page Integration Design

## Intent

Apply the existing Stitch stage-map and typing-exercise page layouts to the app so users see the intended page content, not only its styles, while retaining the working TypeShift flows.

## Goals

- Use the Stitch HTML as the starting point for both page layouts.
- Keep the shared Thymeleaf layout, route URLs, translations, dynamic catalog data, and responsive behavior.
- Keep stage/exercise actions connected to the existing routes and preserve asynchronous exercise generation, typing metrics, numeric keypad, and input capture.

## Non-goals

- Recreate Stitch-only demo interactions or static sample data that the app does not support.
- Change exercise generation, scoring, routes, or add new product behavior.

## Approaches

1. Adapt the current Thymeleaf markup to the Stitch layouts and preserve current behavior.
2. Start from the Stitch HTML and selectively bind it to existing data and behavior (selected). This best preserves the requested page content and visual structure while avoiding disconnected demo actions.

## Constraints and implementation calls

- Keep `layout.html` as the shared shell and the `contentFragment` contract unchanged.
- On the stage map, render the Stitch station/card structure from `stageIds`, `stageNames`, and `exercisesByStage`; exercise links must use the existing stage/exam routes.
- On the exercise page, use the Stitch exercise layout as the visual/content base but retain the existing readouts, keyboard notices, asynchronous docket fragment, prose/numeric input modes, keypad, and control hints. Do not introduce Stitch-only topic-selection behavior unsupported by the current controller.
- Reuse `style.css` and `engine.js`; do not add dependencies or change backend logic.
- Preserve unrelated working-tree changes.

## Verification

Run the focused stage-map and exercise controller/template tests, then the repository's standard test/build command. Confirm rendered pages still expose the existing links, docket target/input IDs, and numeric keypad hooks required by `engine.js`.
