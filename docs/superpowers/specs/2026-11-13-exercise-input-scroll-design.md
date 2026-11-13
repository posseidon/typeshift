# Exercise input scroll design

## Intent

Keep the typing exercise's visible reference cursor in view during long passages without the invisible keyboard-capture textarea pulling the page back to its original position.

## Goals and non-goals

- Keep existing page-follow behavior and docket layout.
- Prevent hidden-input focus from scrolling the document.
- Avoid a new smooth-scroll operation on every keystroke.
- Do not change typing, scoring, or submission behavior.
- Do not add a dependency or create a separately scrollable passage panel.

## Approaches

- **Selected — page follows visible cursor:** position the hidden textarea relative to the viewport, focus it with `preventScroll`, and use non-animated cursor scrolling. This preserves current layout and removes competing scroll anchors.
- **Alternative — internally scrolling passage:** constrain the visible passage to a scrollable panel and scroll within it. This keeps the page fixed but changes the exercise layout more substantially.

## Constraints and implementation calls

- Apply behavior consistently to both checked-in `engine.js` copies.
- Keep changes limited to hidden-input positioning/focus and visible-cursor scroll behavior.
- Verify the regression with a focused test and run the Maven test suite.
