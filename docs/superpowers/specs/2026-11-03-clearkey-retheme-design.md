# ClearKey Clarity Re-theme — Design (short)

**Date:** 2026-11-03
**Source templates:** `stitch_minimalist_ai_typing_tutor/` (`clearkey_clarity/DESIGN.md`, `stage_map_5_practice_stations_dockets/code.html`, `typing_exercise_design_3_calm_story_studio/code.html`)

## Intent

Replace the "mid-century employment-bureau" visual language (square corners, hairline rules,
stamp shadows, mono type) with the ClearKey Clarity design — calm, warm-minimalist, blue —
across **all current views**, while keeping every existing behavior: htmx docket loading,
`engine.js` typing flow, keypad, i18n (vi/hu/en), language picker.

Agreed approach: **CSS re-theme + apply the design elements**, existing functionality intact.
No Tailwind, no markup overhaul.

## Views in scope

- `layout.html` — header/nav shell
- `stage-map.html` — 5 station cards with practice dockets
- `exercise.html` — metrics readout bar, notices, docket (+ task-type variants: chat burst,
  email reply, document paragraph, code comment), keypad, key hints

## What changes

1. **Tokens (`style.css` `:root`)** — adopt ClearKey palette from DESIGN.md:
   background `#f7f9fb`, card white, primary `#0058be`, on-surface `#191c1e`,
   muted `#424754`, outline `#c2c6d6`, success/tertiary `#006947`, error `#ba1a1a`
   (+ `#ffdad6` tint), soft container tints `#eceef0 #e6e8ea`.
2. **Typography** — headlines Plus Jakarta Sans, body/labels Inter; keep generous sizes;
   labels stay small-caps-like but lighter (Inter 600 tracking-wider, not mono ink-heavy).
3. **Shapes & elevation** — rounded corners (0.5rem base, 0.75rem cards), soft ambient
   shadows (`0 2px 8px -2px rgba(15,23,42,.06), 0 8px 16px -4px rgba(15,23,42,.04)`),
   no offset stamp shadows. Bureau flourishes (rubber-stamp rotation, punch strip) removed.
4. **Header** — ClearKey header: brand icon tile (keyboard glyph) + "TypeShift" + small
   uppercase subtitle, calm surface bar; language picker & stage-map icon restyled, same
   HTML contract (htmx & Thymeleaf attributes untouched).
5. **Stage map** — white station cards with grey header band, "STAGE 01" tag → dark pill
   styling, docket links as list rows with hover tint + primary underline; grid unchanged
   (2 columns, generated from `${stageIds}`).
6. **Exercise** — readout bar → calm white/sticky metric strip; gauge fill primary blue
   with 95% marker retained; typing target panel: white card, generous padding,
   `type-target` sizing (36px / 0.15em Inter) for the prose pane; feedback states restyled:
   `ch-ok` emerald with underline, `ch-bad` warm error tint, `cursor-block` primary blue.
   Keypad keys → soft rounded tiles (silent press effect, no stamp offset).
7. **Engine contract preserved** — classes `ch-ok`, `ch-bad`, `ch-todo`, `cursor-block`,
   `verdict-ok/bad` and all element IDs keep their names; only their *values* change.
8. **Font loading** — swap Google Fonts link in `layout.html` to
   `Plus Jakarta Sans 600;700` + `Inter 400;500;600`.

## Non-goals

- No new features from the mockups (AI topic cards, confidence gauge, audio clicks,
  keyboard-guide modal) — design furniture only where real behavior exists.
- No Tailwind build/CDN.
- No controller, service, engine, or test changes (visual-only diff).

## Risks

- Style contract: any class the engine or templates set is left in place even if only its
  value changes; no selector deletion of used classes.
- Hungarian/Vietnamese glyphs must render in the new fonts — both Google fonts support
  Latin Extended; verified visually at the end.
