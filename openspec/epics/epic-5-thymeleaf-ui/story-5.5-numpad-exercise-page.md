# Story 5.5 — Numpad exercise page
Points: 8 | Tool: opsx:plan + opsx:apply | ADRs: 006, 007, 014, 016, 017

Acceptance criteria
- [ ] Keydown handler uses KeyboardEvent.code (ADR-006).
- [ ] Accepts only the Numpad codes listed in ADR-006. Other keys are ignored and main-keyboard digits count as misses.
- [ ] NumpadDecimal inserts a comma.
- [ ] NumLock reminder shown at session start, with a confirm checkbox before the exercise starts.
- [ ] Graduation Cashier Rush hides the live WPM display (ADR-007).
- [ ] Submit sends typed result to the server for exact comparison (Story 4.2).
- [ ] Server errors show the stack trace and stop (ADR-017).
