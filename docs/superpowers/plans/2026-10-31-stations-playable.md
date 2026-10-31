# Stations Playable (Option A) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** All five TypeShift stages are playable in the real app from one parameterized exercise route, with 18 canned exercises shaped like the ADR-004 payload contract.

**Architecture:** A `CannedCatalog` in the `exercise` domain serves fixed exercise records through an extended `ProseExerciseController` (`GET /exercise/{stageId}/{examKey}`, old `/exercise` redirects). The existing `exercise.html` Thymeleaf docket is parametrized; `engine.js` reads its target (and numeric mode data) from page data attributes. Stage 3 renders a numpad per ADR-006; Stage 5 branches display styling on the payload's task type.

**Tech Stack:** Spring Boot (MockMvc tests), Thymeleaf layout shell with fragments, vanilla JS engine, no new dependencies.

**Spec:** `docs/superpowers/specs/2026-10-31-stations-playable-design.md`

## Global Constraints

- Stage map is ADR-013 verbatim: Stage 1 = QWERTZ Core (3 sub-exams: home row `asdf jklé`; top row `qwertzuiopőú`; bottom row `yxcvbnm,-.`), Stage 2 = Symbol Sorcerer (2), Stage 3 = Numpad & Math (2), Stage 4 = Hungarian Accents (2), Stage 5 = Real-World Tasks (4). Graduations: 1 "3-minute continuous QWERTZ passage", 2 "Formatted technical support ticket", 3 "Cashier Rush math evaluation", 4 "Formal Hungarian request letter", 5 "Three random tasks in sequence".
- Sub-exam counts 3/2/2/2/4 + 5 graduation = 18 catalog items; one representative text each.
- Payload shape follows ADR-004 envelope (`contentType`, `displayText`, `expectedAnswer`, `contentHash`, `estimatedDurationSeconds`); reuse the existing enums `hu.elte.ntb.typeshift.ai.model.ContentType/ExamType/TaskType` — do not invent parallel enums.
- ADR-006 numeric rules: input listens to `KeyboardEvent.code`; accepted keys are Numpad digits, `NumpadAdd/Subtract/Multiply/Divide/Decimal/Enter`; `NumpadDecimal` maps to comma (Hungarian decimal convention) regardless of OS locale; main-keyboard digit codes are rejected and counted as misses; the numeric capture element is a contenteditable div, not `input type=number`.
- Test commands run from repo root: `mvn -q test` (all checks incl. PMD/SpotBugs/Checkstyle must stay green; existing controllers carry full Javadoc on public members — match that style).
- Routes stay inside the existing security chain (anything not in the permitAll list requires the auth cookie — do not touch `SecurityConfig` or `JwtAuthenticationFilter`).
- No DB writes, no score verdicts, no unlock logic. Live WPM/accuracy readouts stay as pure UI.
- All exercise content is Hungarian-flavored real text (exercises the native Hungarian QWERTZ keys), including Stage 5 chat/email/code-comment.

## Review Focus

- Unknown `{stageId}` or `{examKey}` → 404 with the app's existing error conventions; no stack page. *(Pinned by Task 2 Step 6 test.)*
- Main-keyboard digits in numeric mode must count as misses and never enter the typed value; only Numpad codes produce characters. *(JS has no test infra — pinned by Task 4 manual checklist and the code-review gate on `engine.js`.)*
- `NumpadDecimal` produces `,` on every OS locale; every Stage 3 `expectedAnswer` is stored as a comma decimal. *(Pinned by Task 1 Step 2 catalog test asserting comma format.)*
- "New copy" reset and the Escape-abort path leave zero residual typed state. *(Pinned by Task 3 reset step + manual check.)*
- Stage 1 graduation passage must be long enough to feel like ~3 minutes: ≥ 1000 chars. *(Pinned by Task 1 catalog test.)*

---

### Task 1: Canned exercise catalog

**Files:**
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/model/CannedExercise.java`
- Create: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/service/CannedCatalog.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/service/CannedCatalogTest.java`

**Interfaces:**
- Consumes: `ai.model.ContentType`, `ai.model.ExamType`, `ai.model.TaskType` enums as-is.
- Produces:

```java
public record CannedExercise(
    int stageId, ExamType examType, String examKey, String label,
    ContentType contentType, TaskType taskType, // taskType null except Stage 5
    String displayText, String expectedAnswer, int estimatedDurationSeconds) {
  public String contentHash();          // "canned-" + stageId + "-" + examKey
  public boolean isMath();              // contentType == MATH
}
```

```java
@Service
public class CannedCatalog {
  // 18 items, keys "exam-1".."exam-N" then "grad" per stage
  public Optional<CannedExercise> find(int stageId, String examKey);
  public List<CannedExercise> byStage(int stageId);       // catalog order, empty if unknown stage
  static final Map<Integer, String> STAGE_NAMES;          // 1 "QWERTZ Core", 2 "Symbol Sorcerer", 3 "Numpad & Math", 4 "Hungarian Accents", 5 "Real-World Tasks"
}
```

- [ ] **Step 1: Write the failing content test**

Plain JUnit test (`CannedCatalogTest`, no Spring context). Assertions:

- per-stage counts from `byStage`: stages 1..5 → 3, 2, 2, 2, 4 sub-exams; exactly one `GRADUATION` item (`grad`) per stage, i.e. 18 total via find probes 1..5 × keys.
- `find(9, "grad")` and `find(1, "nope")` empty (also covers part of the 404 contract).
- every `examKey` globally unique; every PROSE item has `expectedAnswer.equals(displayText)` (ADR-004).
- every MATH item has one `expectedAnswer` containing `,` and no `.` (comma decimals, ADR-006), and recompute check: parse equation from `displayText` (strip `= ?`, map `×`→`*`, `÷`→`/`, `−`/`–`→`-`?, only `+ - × ÷` appear; plain `=` minus sign) with `BigDecimal`, decimals via `new BigDecimal(part.replace(',', '.'))` — scale to the operand with the larger scale result string must equal `expectedAnswer` after replacing `.`→`,`. Implementer may instead evaluate from the record's operands; the recompute must be BigDecimal, not hardcoded strings.
- Stage-specific prose checks (Hungarian QWERTZ coverage):
  - stage 1 `exam-1` contains each of `a`,`s`,`d`,`f`,`j`,`k`,`l`,`é`; `exam-2` contains `q`,`w`,`e`,`r`,`t`,`z`,`ő`,`ú`; `exam-3` contains `y`,`x`,`c`,`v`,`b`,`n`,`m`,`-`; all 120–220 chars.
  - stage 1 `grad` ≥ 1000 chars.
  - stage 2 `exam-1` contains `.` `,` `;` `:` and a hyphen; `exam-2` contains `@`, `&`, `(`, `)`, `„`, `”`, and a dash `–`; 150–260 chars.
  - stage 4 both exams and `grad` each contain ≥ 6 distinct characters of `á é í ó ö ő ú ü ű`; 200–340 (`grad` 300–450) chars.
  - stage 5 `exam-4` (code comment) starts with `//` and contains no backticks; all four 200–320 chars, `grad` 300–420 chars.
  - estimatedDurationSeconds: 45 for sub-exams, 180 for `grad`.
  - stage 5 taskType mapping exactly: `exam-1`=CHAT_BURST, `exam-2`=EMAIL_REPLY, `exam-3`=DOCUMENT_PARAGRAPH, `exam-4`=CODE_COMMENT; all other stages' `taskType` null.
- Content style pin for the implementer: consistent with the existing Stage 4 docket registry copy (formal Hungarian civil-bureau tone). Stage 4 `exam-1` `displayText` MUST reuse the current hardcoded engine text exactly: `'A hivatalos iratokat kérjük a főbejárat melletti iktatóba eljuttatni még a mai nap folyamán. Az ügyintézés határideje tizenöt munkanap, kivéve ha sürgősségi kérelmet nyújtott be az illetékes osztályvezetőnél...'`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl typeshift-backend -Dtest=CannedCatalogTest test`
Expected: FAIL (classes don't exist).

- [ ] **Step 3: Implement `CannedExercise` and `CannedCatalog` with all 18 items**

Write the 18 Hungarian texts per the Step 1 constraints. Keep the catalog a static `List<CannedExercise>` in plain code (mirrors `stage` seeding being immutable-by-code in Story 1.3).

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl typeshift-backend -Dtest=CannedCatalogTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise
git commit -m "feat: canned exercise catalog for all five stations (18 items)"
```

---

### Task 2: Parameterized exercise route

**Files:**
- Modify: `typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseController.java`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/controller/ProseExerciseControllerTest.java` (new)

**Interfaces:**
- Consumes: `CannedCatalog.find/stage names`, `CannedExercise` (Task 1).
- Produces: model attributes the template (Task 3) consumes — all `String` unless noted:
  - `title` — "TypeShift — " + exercise label
  - `contentFragment` — `"exercise"`
  - `shiftState` — `"Shift in progress // Stage " + stageId + ": " + STAGE_NAMES[stageId] + (examType == GRADUATION ? " [Graduation]" : " [HU]")`
  - `docketLabel` — label from the record; `docketFraming` — exam-derived framing string (e.g. stage 4: "Hungarian prose transcribing [civil service record]", stage 5 exam-2: "email reply composition"), implementer-captured one-liner per exam
  - `dataTarget` — `displayText`; `dataExpected` — `expectedAnswer` when MATH, `null` otherwise
  - `dataMode` — `"numeric"` when MATH else `"prose"`
  - `taskClass` — lowercase TaskType name with underscores → dashes (`"chat-burst"`, `"email-reply"`, `"document-paragraph"`, `"code-comment"`) when taskType non-null, `null` otherwise

Routes (extend the existing controller):

```java
@GetMapping("/exercise")                         // redirect to current Stage 4 assignment
public String redirectToAssignment()             // "redirect:/exercise/4/exam-1"

@GetMapping("/exercise/{stageId}/{examKey}")
public java.util.Optional<CannedExercise> lookup(int stageId, String examKey)
// unknown pair → org.springframework.web.server.ResponseStatusException(NOT_FOUND)
// known pair → renders "layout" with model attrs above
```

- [ ] **Step 1: Write the failing controller test**

Copy the class-level setup of `ProfileControllerTest` verbatim (`@SpringBootTest @AutoConfigureMockMvc @TestPropertySource` same properties; seed owner + issue cookie; `session()` helper). Test methods, named exactly:

- `proseDocketServesStageFourText()` — `GET /exercise/4/exam-1` + cookie → 200, body contains `A hivatalos iratokat` and `Stage 4`.
- `numericDocketServesKeypad()` — `GET /exercise/3/exam-1` + cookie → 200, contains `data-mode="numeric"` and `id="keypad"`.
- `graduationDocketServes()` — `GET /exercise/4/grad` + cookie → 200.
- `unknownStageGivesNotFound()` / `unknownExamKeyGivesNotFound()` — `GET /exercise/9/exam-1` / `GET /exercise/4/nope` + cookie → 404.
- `oldRouteRedirectsToCurrentAssignment()` — `GET /exercise` (with cookie) → `status().is3xxRedirection()`; location header contains `/exercise/4/exam-1`.
- `unauthenticatedRequestIsRebuffed()` — `GET /exercise/4/exam-1` without cookie → same behavior as `ProfileControllerTest`'s unauthorized handling (verify `JwtAuthenticationFilter.reject` first and pin whatever it does — do not change the filter).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl typeshift-backend -Dtest=ProseExerciseControllerTest test`
Expected: FAIL (route/model attributes don't exist).

- [ ] **Step 3: Implement the two routes per the Interfaces block**

Keep full Javadoc style of the existing controller. Old `exercise()` method becomes the redirect.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -pl typeshift-backend -Dtest=ProseExerciseControllerTest test`
Expected: PASS.

- [ ] **Step 5: Full suite stays green**

Run: `mvn -q test`
Expected: PASS including static analysis.

- [ ] **Step 6: Commit**

```bash
git add typeshift-backend/src/main/java/hu/elte/ntb/typeshift/exercise/controller typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/controller
git commit -m "feat: parameterized /exercise/{stageId}/{examKey} route with catalog lookup"
```

---

### Task 3: Data-driven docket (prose stations playable end-to-end)

**Files:**
- Modify: `typeshift-frontend/src/main/resources/templates/exercise.html`
- Modify: `typeshift-frontend/src/main/resources/static/js/engine.js:23-26` (target text source)
- Test: covered by the Task 2 controller test add-back (below)

**Interfaces:**
- Consumes: model attrs from Task 2 ("produces").
- Produces: docket markup contract `engine.js` reads — pinned exactly:
  - `<div id="typing-docket" data-target="${dataTarget}" data-mode="${dataMode}" [data-expected="${dataExpected}"] th:classappend="${taskClass} ? ' docket--' + {taskClass}">` wraps the docket card (classappend only when taskClass set).
  - prose input `id="practice-input"` stays; `display` id `prose-display`; readout ids unchanged (`live-wpm`, `live-acc`, `live-timer`, `gauge-bar`, `gauge-label`, `retry-count`, `gross-count`); shift-state span gets `th:text="${shiftState}"`; docket head labels get `th:text` for `docketLabel` / `docketFraming`.
- Add the "New copy" reset affordance in the docket head: `<button type="button" onclick="window.location.reload()">New copy</button>` (task-independent, zero JS state).

- [ ] **Step 1: Write the failing assertion**

Extend `ProseExerciseControllerTest` (Task 2 file):

- `GET /exercise/4/exam-1`: body matches `id="typing-docket"` with `data-target` whose value contains `A hivatalos iratokat` (regex or `containsString` on the whole tag is acceptable), contains `data-mode="prose"`, contains `New copy`.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl typeshift-backend -Dtest=ProseExerciseControllerTest test`
Expected: FAIL (template not yet parameterized).

- [ ] **Step 3: Parameterize the template and the engine**

- In `exercise.html` repurpose the existing static docket markup into the attribute contract above (thin-Thymeleaf `th:attr`, labels `th:text`, remove the old hardcoded labels/text where replaced).
- In `engine.js` replace lines 23–26 `var targetText = ...` with `var docket = document.getElementById('typing-docket'); var targetText = docket && docket.dataset.target;` — early-return stays first (pages without the docket no-op). Everything else unchanged in this task.
- Keep the numeric keypad markup OUT of this task (Stage 3 still renders prose-style; numeric UI arrives in Task 4 — its Task 2 test assertion `id="keypad"` may stay failing until then; note this when running the suite).

- [ ] **Step 4: Run the prose assertions to verify they pass**

Run: `mvn -q -pl typeshift-backend -Dtest='ProseExerciseControllerTest#proseDocketServesStageFourText' test`
Expected: PASS. (Task 2's `numericDocketServesKeypad` may stay failing until Task 4 adds the keypad markup; that intermediate red is expected — check the suite again in Task 4 Step 4.)

- [ ] **Step 5: Manual UX check (browser, the point of this whole iteration)**

Start the app (`mvn -q -pl typeshift-backend spring-boot:run`, register owner, log in). Play stages 1, 2, 4: typing highlights right/wrong chars, live WPM/acc/gauge move, Escape-abort dialog works, "New copy" reloads clean with zero residual state.

- [ ] **Step 6: Commit**

```bash
git add typeshift-frontend/src/main/resources/templates/exercise.html typeshift-frontend/src/main/resources/static/js/engine.js typeshift-backend/src/test/java/hu/elte/ntb/typeshift/exercise/controller
git commit -m "feat: data-driven exercise docket, engine reads target from page"
```

---

### Task 4: Stage 3 numeric mode (numpad capture, ADR-006)

**Files:**
- Modify: `typeshift-frontend/src/main/resources/static/js/engine.js`
- Modify: `typeshift-frontend/src/main/resources/templates/exercise.html` (numeric variant + keypad + NumLock banner)
- Modify: `typeshift-frontend/src/main/resources/static/css/style.css` (keypad styles)

**Interfaces:**
- Consumes: `data-mode="numeric"`, `data-expected` Task 3 contract; catalog comma-decimal answers (Task 1).
- Produces: pinned exam elements consumable by tests/manual checks —
  - static warning banner (numeric only) with text containing `NumLock` (ADR-006: warn at session start; browser cannot read state — always shown)
  - keypad `<div id="keypad">` of buttons `data-code="Numpad0..9 | NumpadDecimal | NumpadAdd | NumpadSubtract | NumpadMultiply | NumpadDivide | NumpadEnter"`, labels `0–9 , + − × ÷ ⏎`
  - capture element `<div id="practice-pad" contenteditable="false">` (not an input, not contenteditable-true — direct divs and manual character rendering, per ADR-006 the key codes must stay available)
  - verdict line `<div id="math-verdict">` (hidden until Enter)
  - engine wiring: document-level `keydown` handler for accepted codes; keypad buttons dispatch `document.dispatchEvent(new CustomEvent('typepad', {detail: {code}}))`; typed value renders into `#practice-pad`

**Behavior pins:**
- Key map (engine constant): digits → their digit char; `NumpadDecimal` → `,`; `NumpadAdd` → `+`; `NumpadSubtract` → `−`; `NumpadMultiply` → `×`; `NumpadDivide` → `÷`.
- `NumpadEnter` / physical `Enter` evaluates: trimmed typed value equal (string equality) to `dataExpected` → verdict green "Ledger reconciled — answer accepted", else red "Reconciliation failed — recorded answer does not match the ledger" and show the expected value.
- Main-keyboard `Digit0..Digit9` codes: rejected, never enter the value, increment the miss counter (reuse `#retry-count` readout, label it "Missed / Hiányszám" context is already err count — numeric mode repoints the same element to misses).
- All other main-keyboard printable codes: silently ignored (per ADR-006 scope).
- Typed value max length = length of `dataExpected` + 2 (guard against runaway).

- [ ] **Step 1: Add the numeric variant markup + styles**

Numeric-only markup gated by `th:if="${dataMode == 'numeric'}"`: NumLock banner, keypad grid, `#practice-pad`, `#math-verdict`; prose input hidden via `th:unless`. CSS: `.keypad` grid (5 columns, mono digits, pressed feedback), `.pad-value` styling, verdict success/failure colors reusing existing `--green`/error palette variables.

- [ ] **Step 2: Implement the numeric engine branch**

In `engine.js`, branch on `docket.dataset.mode`: numeric mode installs the keydown/typepad handlers per the pins above; prose mode keeps Task 3 flow untouched. Prose path refactored minimally if needed (shared timer/reload logic only).

- [ ] **Step 3: Manual checklist (browser — the pin-covering check; no JS test infra in repo)**

- `/exercise/3/exam-1`: tapping keypad buttons types `,` correctly; typed value renders; Enter with wrong value shows verdict red with expected shown; Enter with exactly the catalog answer shows green.
- Physical keyboard: main-row digits increment misses and add nothing; numpad digits type; `NumpadDecimal` types `,` even when OS locale would produce `.`; operators `+ − × ÷` appear per key map.
- `/exercise/3/exam-2` and `/exercise/3/grad` same; prose stations (1/2/4/5) still behave exactly as in Task 3.
- NumLock banner visible on numeric pages only, dismissible like the layout banner.

- [ ] **Step 4: Full suite stays green**

Run: `mvn -q test`
Expected: PASS (now includes Task 2's `id="keypad"` assertion).

- [ ] **Step 5: Commit**

```bash
git add typeshift-frontend/src/main/resources/static/js/engine.js typeshift-frontend/src/main/resources/templates/exercise.html typeshift-frontend/src/main/resources/static/css/style.css
git commit -m "feat: stage 3 numpad exercise mode per ADR-006"
```

---

### Task 5: Stage 5 real-world task display variants

**Files:**
- Modify: `typeshift-frontend/src/main/resources/static/css/style.css`
- Modify: `typeshift-frontend/src/main/resources/templates/exercise.html` (variant-only rendering hooks)
- Test: optional style-only; UI verified manually

**Interfaces:**
- Consumes: `docket--chat-burst` / `docket--email-reply` / `docket--document-paragraph` / `docket--code-comment` container classes from Task 3's classappend contract.

- [ ] **Step 1: CSS for the four variants + Stage 5 framing copy check**

- `--chat-burst`: each line of `displayText` (lines start with `- <name>:` per content authoring) renders with chat rhythm — `#prose-display { white-space: pre-line; }` hanging indent, speaker-ish tint on the block; no bubbles/-DOM change (plain styling, char engine untouched).
- `--email-reply`: serif-ish block, left rule, room for the header line (content authored as `Tárgy: ...` first line).
- `--document-paragraph`: plain wide-measure paragraph look.
- `--code-comment`: monospace, no wrap behavior change, tinted background (Story 6.6 rule: displayText contains no code fences — the `//` prefix look is authored content, not markup).
- Manual check: `/exercise/5/exam-1..4` each visibly distinct from the page; `/exercise/5/grad` renders like the document variant.

- [ ] **Step 2: Full suite stays green**

Run: `mvn -q test`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add typeshift-frontend/src/main/resources/static/css/style.css typeshift-frontend/src/main/resources/templates/exercise.html
git commit -m "feat: stage 5 task-type display variants"
```

---

### Task 6: Stage-map docket links

**Files:**
- Modify: `typeshift-frontend/src/main/resources/templates/stage-map.html`
- Test: `typeshift-backend/src/test/java/hu/elte/ntb/typeshift/stagemap/controller/StageMapControllerTest.java` (create; if a stage-map test already exists, extend it instead)

**Interfaces:**
- Consumes: the fixed examKey scheme (`exam-1..exam-N`, `grad`) and labels from Task 1's catalog.

- [ ] **Step 1: Write the failing test**

Same `@SpringBootTest` pattern; `GET /stage-map` + cookie → 200 and body contains, for every station card, its own links: `/exercise/1/exam-1`, `/exercise/1/grad`, `/exercise/2/exam-1`, `/exercise/2/exam-2`, `/exercise/3/grad`, `/exercise/4/exam-2`, `/exercise/5/exam-4`, `/exercise/5/grad` (representative sample covering all five cards, incl. the `exams` counts in between as asserted keys: full set 1..5 × their keys).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -pl typeshift-backend -Dtest=StageMapControllerTest test`
Expected: FAIL.

- [ ] **Step 3: Add the link lists to the cards**

Under each station card's milestones block, a small "Practice dockets" list: one link per sub-exam + graduation, label = the ADR-013 focus text of that exam ("Home row grounding", "Top row & QWERTZ swap", "Bottom row baseline", "3-minute continuous QWERTZ passage"; Stage 2/3/4/5 correspondingly per the Global Constraints graduation names and milestone texts already in the cards; Stage 5 exams labeled Chat burst / Email reply / Document paragraph / Code comment and grad "Three random tasks in sequence"). Certification-record content otherwise untouched.

- [ ] **Step 4: Run test to verify it passes; full suite green**

Run: `mvn -q test`
Expected: PASS.

- [ ] **Step 5: Manual UX pass over the whole flow**

From `/stage-map`, click through every station × every dockets entry; all 18 serve and play. Log rough UX notes (user said this iteration is about feeling the flow).

- [ ] **Step 6: Commit**

```bash
git add typeshift-frontend/src/main/resources/templates/stage-map.html typeshift-backend/src/test/java/hu/elte/ntb/typeshift/stagemap
git commit -m "feat: stage map cards link to their practice dockets"
```
