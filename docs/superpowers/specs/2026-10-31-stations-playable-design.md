# Design — Implement each station as a playable page (Option A)

Date: 2026-10-31
Status: Draft for approval

## Intent and why

The business value of this iteration is a **working station, per station**. The
owner wants to sit down in the real app and *play* every stage in the browser —
QWERTZ Core, Symbol Sorcerer, Numpad and Math, Hungarian Accents, Real-World
Tasks — to get experience and refine the user experience and interface before
any complex metrics, scoring, or stage progression are added.

"Implement each station. I do not want mockups." The pages are real Thymeleaf
pages inside the running Spring Boot app, reachable like the existing
`/exercise` and `/stage-map` routes.

## Goals

1. All five stations are playable in the browser:
   - Stage 1 QWERTZ Core — 3 sub-exams (home row `asdf jklé`, top row
     `qwertzuiopőú`, bottom row `yxcvbnm,-.`) + graduation "3-minute continuous
     QWERTZ passage".
   - Stage 2 Symbol Sorcerer — 2 sub-exams (punctuation & hyphenation; AltGr
     symbols, brackets, quotation marks & dashes) + graduation "Formatted
     technical support ticket".
   - Stage 3 Numpad and Math — 2 sub-exams (keypad orientation & decimal
     tallying; ledger arithmetic) + graduation "Cashier Rush math evaluation".
   - Stage 4 Hungarian Accents — 2 sub-exams (accents set á é í ó ö ő ú ü ű;
     formal wording) + graduation "Formal Hungarian request letter". The
     existing `/exercise` screen is Stage 4 in this scheme and stays correct.
   - Stage 5 Real-World Tasks — 4 sub-exams in the locked task types: chat
     burst, email reply, document paragraph, code comment + graduation
     "three tasks in sequence" (see Non-goals for the sequence part).
2. One representative **canned** exercise per sub-exam, plus one per
   graduation. Counts must match ADR-013: 3/2/2/2/4 → 13 sub-exams + 5
   graduation = 18 catalog items.
3. One generic exercise route: `GET /exercise/{stageId}/{examKey}` serving the
   existing layout shell (`contentFragment` pattern). Content is canned but
   shaped like the ADR-004 payload contract (`contentType`, `displayText`,
   `expectedAnswer`, `contentHash`, `estimatedDurationSeconds`) so the real AI
   content service can later take over without changing these pages.
4. The existing `engine.js` typing engine is reused and parametrized: the
   target text (and for Stage 3, the expected numeric answer) comes from the
   page, not a JS constant. Live WPM / accuracy / progress gauge stay.
5. Stage 3 gets the genuinely different interaction: an on-screen numeric
   keypad (ADR-006) that fills the hidden field, exact-match answer check
   (integer or decimal as typed, string equality against `expectedAnswer`).
   ADR-006 specifics the engine must honor: input listens to
   `KeyboardEvent.code` (NumpadX, not Digit — main-keyboard digits are counted
   as misses, display-only in this iteration); accepted keys are Numpad
   digits, `NumpadAdd/Subtract/Multiply/Divide/Decimal/Enter`;
   `NumpadDecimal` maps to comma (Hungarian decimal convention) regardless of
   OS locale, so `expectedAnswer` is stored as a comma-decimal string; the
   input element is a contenteditable div, not `input type=number`.
6. Stage 5 sub-exams get display variants of the prose docket from the
   payload metadata: chat burst (line-grouped message bubbles), email reply
   (header block + paragraph), document paragraph, code comment (monospace
   block). ADR-004 carries `taskType` only on the request side, so the
   catalog record carries the task type (CHAT_BURST, EMAIL_REPLY,
   DOCUMENT_PARAGRAPH, CODE_COMMENT) next to the envelope and the template
   branches on that field, never on the route.
7. Stage-map station cards link to their own docket lists (sub-exams +
   graduation) without touching the static certification-record content.
8. Retry/abandon UX: keep the existing Escape-abort behavior; add an explicit
   reset affordance on the docket ("New copy") that reloads the exercise clean.

## Non-goals (explicitly deferred)

- No AI content generation; canned texts only (AI arrives in Epic 3 work).
- No score records, no accuracy ≥ 95% pass/fail verdicts, no DB writes for
  attempts, no graduation gating, no unlock state machine (Epic 2), no weak-key
  heatmap (Epic 7). The live readouts stay as-is — they are UI, not scoring.
- Graduation "three random tasks in sequence" (Stage 5) is NOT one combined
  flow; it is a single canned passage for now, selectable like the others.
- No changes to auth, profile, signup, or the stage-map's fictional records.

## Anti-goals (failure even if it works)

- Building a demo shell or static mockup pages — anything that would be
  thrown away when scoring arrives.
- Hardcoding stage data only in JavaScript so a future server-driven exercise
  can't replace it.

## Approaches considered

- **A — One exercise route, parametrized (chosen).** Generic page + one engine,
  fed canned payloads. One page, one UX to refine; every fix benefits all
  stations; contract-shaped content lets the AI service drop in behind it.
- **B — Dedicated page per station.** Five controllers/templates mirroring
  mockups. Traded for per-station framing fidelity that the shared layout
  already largely provides; most of the code duplicates A.
- **C — Client-side stage switcher on the existing page.** Cheapest but a demo
  shell in disguise — rejected by the owner explicitly ("no mockups").

## Constraints

- AGENTS.md: DDD package structure. All new backend classes live in the
  existing `hu.elte.ntb.typeshift.exercise` domain package (it already owns
  the exercise screen). No new top-level domains unless the content model
  clearly outgrows it.
- ADR-013 is the authoritative stage map; stage/sub-exam counts and names come
  from it, not invented freely.
- ADR-018: no key remapping; content must exercise native Hungarian QWERTZ
  keys (ő ú ö ü á é í ó ű). Interface language stays English; exercise content
  is Hungarian-flavored (my call: stages 1–4 Hungarian text; Stage 5 also
  Hungarian, matching the product's fiction).
- ADR-016 pass rule accuracy ≥ 95% — shown only as the gauge threshold label
  already on the readout bar; no verdict logic.
- Quality gates PMD / SpotBugs / Checkstyle must stay green; follow the Javadoc
  style of existing controllers.
- Static analysis and tests: each new route gets a MockMvc-style test in the
  packages' own test conventions; content catalog gets an assert-style check
  that it covers stage counts 3/2/2/2/4 plus 5 graduation items.

## Design decisions (decided)

- **Content home:** an `ExerciseCatalog` in the `exercise` domain package
  holding the 18 canned exercises (13 sub-exams + 5 graduation) as records;
  deterministic, testable, no DB. `contentHash: "canned-" + stageId + "-" +
  examKey` placeholder is fine for dedup testing later. The current hardcoded
  Stage 4 Hungarian prose moves into the catalog as one of its sub-exams.
- **Route:** `GET /exercise/{stageId}/{examKey}`; unknown keys → 404 via
  existing error conventions. The old `/exercise` stays working as a redirect
  to its Stage 4 current assignment so existing links and tests don't break.
  Canonical exam-key slugs derive from the ADR-013 sub-exam/graduation names.
- **Stage 3 answer handling:** `expectedAnswer` is rendered into a data
  attribute; on Enter (or keypad `ENTER` button) the engine compares the typed
  number (trim both, equal-string compare) and shows a pass/fail line on the
  docket. Exact match per stage-map rule.
- **Stage-map wiring:** each station card gets a small docket-link list under
  its milestones linking to its exercises. Certification-record content stays
  untouched.
- **Engine:** reads `data-target`, `data-expected` (optional) and
  `data-mode` (`prose` | `numeric`) from the exercise docket; keyboard-only
  behavior unchanged; Stage 3 keypad buttons dispatch input into the hidden
  field.

## What's left to the builder

- Writing the 16 canned texts (Hungarian-flavored, exercising the right key
  sets per focus; graduation Stage 1 long enough to read as ~3 minutes at
  practice speed; Stage 3 a sequence of ledger equations with numeric results
  computed exactly, e.g. via BigDecimal at authoring time, stored as
  comma-decimal strings per ADR-006).
- Stage 5 framing copy and CSS for the four display variants.
- Minor engine edge cases (e.g., ignoring Tab/focus jumps) at builder's
  discretion.
- Whether `/exercise/{stageId}/{examKey}` requires login — match whatever the
  existing `/exercise` route and security config do today, don't tighten it.

## Open questions for the owner

None outstanding; content depth (one text per sub-exam + graduation) and
Hungarian-flavored content were proposed and stood.
