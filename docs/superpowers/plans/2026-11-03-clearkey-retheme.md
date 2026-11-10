# ClearKey Clarity Re-theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-skin all views (layout, stage-map, exercise + docket) to the ClearKey Clarity design while preserving every existing behavior (htmx docket loading, `engine.js` typing flow, keypad capture, i18n).

**Architecture:** Pure CSS re-theme. `static/css/style.css` keeps its selector set (template + engine contract) and changes only values: tokens, typography, radii, shadows, and component styling. `layout.html` gets a font swap and ClearKey header chrome; `stage-map.html` and `exercise.html` get minor class/markup touches only where the bureau motifs (stamps, punch strips) must yield.

**Tech Stack:** Thymeleaf, vanilla CSS, htmx 2, Google Fonts (Plus Jakarta Sans + Inter).

**Spec:** `docs/superpowers/specs/2026-11-03-clearkey-retheme-design.md`

## Global Constraints

- Engine contract preserved verbatim: classes `ch-ok`, `ch-bad`, `ch-todo`, `cursor-block`, `verdict-ok`, `verdict-bad`; IDs `prose-display`, `practice-input`, `practice-pad`, `math-verdict`, `keypad`, `typing-docket`, `typing-container`, `exercise-loading`, `keyboard-banner`, `numlock-banner`, `live-wpm`, `live-acc`, `live-timer`, `gauge-bar`, `gauge-label`, `retry-count`, `gross-count`.
- htmx contract preserved: `#exercise-loading` with `hx-get` / `hx-trigger="load"` / `hx-swap="outerHTML"`; the `htmx:beforeSwap` error script stays functional.
- Thymeleaf attributes (`th:*`), message keys, language picker form, and model attributes untouched.
- No Tailwind, no new dependencies, no Java/script/test changes.
- Palette and typography values copied from `stitch_minimalist_ai_typing_tutor/clearkey_clarity/DESIGN.md`.

## Review Focus

- Used-class selector coverage after rewrite: every class string in the three templates (and `engine.js` outputs) must still resolve to a rule — grep-verified in Task 1 and re-checked per task.
- Hungarian/Vietnamese glyph rendering in the new fonts (Latin Extended): visual check on stage map copy and docket text at final task.
- Typing pane readability at 36px/0.15em for wrapped long prose (`white-space: pre-line` lines from the generator) — check chat-burst and support-ticket variants.
- Sticky readout bar overlap with content at small widths.

---

### Task 1: Tokens, fonts, shared shell (layout)

**Files:**
- Modify: `src/main/resources/static/css/style.css` (rewrite `:root`, base, topnav, fonts, primitives: `.page`, typographic roles, `.tag`, `.btn`, `.notice`, `.gauge`)
- Modify: `src/main/resources/templates/layout.html` (font link swap; header markup to ClearKey brand chrome)

**Interfaces:**
- Produces: CSS custom properties used by later tasks — `--bg #f7f9fb`, `--card #ffffff`, `--ink #191c1e`, `--muted #424754`, `--outline #c2c6d6`, container tints `--surface-low #f2f4f6`, `--surface #eceef0`, `--surface-high #e6e8ea`, `--primary #0058be`, `--primary-ct #2170e4`, `--primary-tint #d8e2ff`, `--success #006947`, `--success-ct #00855b`, `--error #ba1a1a`, `--error-tint #ffdad6`; `--radius 0.5rem`, `--radius-lg 0.75rem`; `--shadow 0 2px 8px -2px rgba(15,23,42,0.06), 0 8px 16px -4px rgba(15,23,42,0.04)`; fonts `--font-ui "Inter"`, `--font-display "Plus Jakarta Sans"`.

- [ ] **Step 1:** Edit `layout.html` font link to `family=Inter:wght@400;500;600&family=Plus+Jakarta+Sans:wght@600;700` (keep `@{/css/style.css}` and all other head lines unchanged). Rebuild the header: brand = rounded square tile (40px, `--primary` bg) containing an inline keyboard SVG + "TypeShift" (Plus Jakarta Sans 600) + small uppercase muted subtitle "Adult learning space"; keep the existing nav `<a th:href="@{/stage-map}">` and language-picker form with their current `th:*` attributes, restyled via `.topnav` class values (no attribute changes).
- [ ] **Step 2:** Rewrite `style.css` `:root` and the base/shared sections (body, `.topnav`, `.brand`, nav links, `.language-picker`, `.page`, `.label-caps`, `.headline-xl`, `.headline-md`, `.body-lg`, `.page-head`, `.tag` [+variants], `.btn` [+variants], `.notice` [+`.close`], `.gauge` [+fill/marker]) to the ClearKey values: Inter/Plus Jakarta Sans, radii `var(--radius)`/`var(--radius-lg)`, soft `var(--shadow)` instead of offset shadows, focus outlines `3px solid var(--primary)` with 2px offset. Delete bureau-only flourishes (`--stamp-shadow` usage, stamp rules NOT referenced by any current template — verify each deletion against a grep of `src/main/resources/templates` before removing).
- [ ] **Step 3:** Verify used-class coverage: grep every `class="..."` token from the three templates + engine.js against the rewritten CSS; every hit must resolve to a rule or a shared primitive.
- [ ] **Step 4:** Run `./mvnw -q test` (or project's standard test command) — expected: PASS (controller tests unaffected).
- [ ] **Step 5:** Commit: `feat: adopt ClearKey tokens, fonts, and shell chrome`.

### Task 2: Stage map view

**Files:**
- Modify: `src/main/resources/static/css/style.css` (`stn-*` component block, `.directive` if referenced by stage-map)
- Modify: `src/main/resources/templates/stage-map.html` (only if a bureau-only wrapper class must be dropped; keep `th:each` loops and `th:href` links intact)

**Interfaces:**
- Consumes: Task 1 tokens.

- [ ] **Step 1:** Restyle stations per the Stitch stage-map mockup: white card (`--card`), `--radius-lg`, `--shadow`, hover shadow lift; `.stn-head` grey band (`--surface-high`) with dark "STAGE NN" pill (`#191c1e` pill, white Inter label, small radius) matching `<span class="tag tag-ink">`; docket links as list rows (`padding 0.5rem 0.75rem`, `--radius`, hover `--surface-low` bg, primary text with 4px-offset underline on hover).
- [ ] **Step 2:** Run `./mvnw -q test` — PASS. Start the app (`./mvnw spring-boot:run` or standard command), fetch `/stage-map`, and visually confirm: 2-column grid intact, all five stations render, links point to `/exercise/{stageId}/{examKey}`.
- [ ] **Step 3:** Commit: `feat: ClearKey stage-map station cards`.

### Task 3: Exercise view (readout bar, docket, keypad)

**Files:**
- Modify: `src/main/resources/static/css/style.css` (exercise block: `.readout-bar*`, `.readout`, `.shift-state`, `.docket*`, `.type-pane`, `.ch-*`, `.cursor-block`, `#practice-input`, `.pad-*`, `.keypad`, `.kbd`, `.controls-row`, `.key-hints`, `#math-verdict`)
- Modify: `src/main/resources/templates/exercise.html` (markup polish only: replace `⌨`/`▣` bare glyphs with small inline SVGs, keep IDs/`th:*`/htmx attributes byte-identical)

**Interfaces:**
- Consumes: Task 1 tokens.

- [ ] **Step 1:** Restyle: readout bar → sticky white strip with soft shadow and calm metric cards; `.gauge-fill` → `var(--primary)`; keep the brass-like 95% marker (use `var(--primary-ct)`); docket → white card `--radius-lg --shadow` with the docket-head ghost buttons as calm secondary buttons; `.type-pane` → Inter 400, `2rem` with `letter-spacing .15em` (the ClearKey `type-target`), lines on `--bg` inner panel; feedback: `.ch-ok` emerald (`--success`, 2px underline), `.ch-bad` warm tint (`--error` on `--error-tint` 40% bg), `.cursor-block` primary; keypad keys → soft tiles with `--radius` and press-down active state; `verdict-ok/bad` → `--success`/`--error` on white cards.
- [ ] **Step 2:** Retry-panel script audit: `exercise.html`'s `htmx:beforeSwap` handler re-creates `.docket.type-card` + `.btn` — confirm those selectors still produce the ClearKey look (no JS edits).
- [ ] **Step 3:** Run `./mvnw -q test` — PASS. Launch app, open one prose docket (`/exercise/1/exam-1`) and one numeric docket (`/exercise/3/exam-1`): typing pane, cursor, error/ok spans (type 2–3 chars), keypad, and retry-on-error path functional.
- [ ] **Step 4:** Commit: `feat: ClearKey exercise docket and metrics`.

### Task 4: Task-type variants, responsive, final check

**Files:**
- Modify: `src/main/resources/static/css/style.css` (`.docket--chat-burst/-email-reply/-document-paragraph/-code-comment` variants, media queries)

**Interfaces:**
- Consumes: Tasks 1–3 tokens.

- [ ] **Step 1:** Re-map the four Stage 5 variants to calm equivalents: chat burst → `--surface-low` tinted block; email reply → serif Georgia retained with `--primary` left rule (replacing brass); document paragraph keeps 65ch measure; code comment → `--surface-low` monospace block.
- [ ] **Step 2:** Update both media queries (960px / 600px) for the new grid/metric wrapping.
- [ ] **Step 3:** Visual pass (launch app): i18n toggle hu/vi/en on stage map and exercise — glyph rendering, label lengths, language picker; Stage 5 dockets (5/exam-1..4) render per variant; at 900px and 400px widths no sticky-bar overlap.
- [ ] **Step 4:** Commit: `feat: ClearKey task variants and responsive pass`.

---

**Verification (whole branch):** `./mvnw -q test` green; app boots; `/`, `/exercise/1/exam-1`, `/exercise/3/exam-1`, `/exercise/5/exam-3` all render in ClearKey style with generation + typing flow working.
