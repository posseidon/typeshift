# ADR-006: Numpad-specific input capture (Stage 3)
Status: Locked

**Decision**
- Input listens to KeyboardEvent.code, not KeyboardEvent.key. NumpadX codes are distinguished from Digit codes.
- Only NumpadX digits, NumpadAdd, NumpadSubtract, NumpadMultiply, NumpadDivide, NumpadDecimal, and NumpadEnter are accepted in Stage 3 numpad exercises. Main-keyboard digits are rejected and counted as misses.
- NumLock check: the page warns at session start if NumLock appears off, because Numpad digits then send arrow keys.
- NumpadDecimal maps to comma, matching Hungarian decimal convention, regardless of OS locale.
- Input element is a div with contenteditable, not input type=number, so key codes stay available.

**Consequences:** Browser NumLock state cannot be read directly. The warning is a reminder, and the user confirms it.
