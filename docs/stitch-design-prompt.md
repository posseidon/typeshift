# TypeShift — Stitch design prompt

One-line brief: Design the UI for **TypeShift**, a desktop web app that trains
people to touch-type on the Hungarian QWERTZ keyboard. The whole app is themed
as a vintage employment bureau / punch-clock office: learning to type is framed
like holding a job — you clock in, work through stages like assignments, and
your results are filed like employment records. Clean, official, ink-on-paper,
with a wink. NOT a corporate SaaS dashboard, NOT a neon gamified trainer.

## Design system

**Mood:** mid-century employment office meets modern utility. Calm, precise,
 generously spaced, strictly aligned like a paper form. Flat surfaces, hairline
 borders, no gradients, no glassmorphism, no drop shadows deeper than 2px.

**Palette (exact hex):**
- Background (paper): `#F4F0E5`
- Card surface: `#FCFAF3`
- Ink (primary text): `#1F2A24`
- Muted ink (secondary text): `#5D6862`
- Hairline border: `#D9D4C5`
- Bureau green (primary buttons, links, active states): `#2E5E4E`
- Approved green (pass states): `#2F7D4F`
- Stamp red (errors, fail states): `#A4262C`
- Brass accent (highlights, badges, current stage): `#B98A2F`

**Typography (Google Fonts only):**
- UI text: IBM Plex Sans (400/500/600/700)
- Numbers, typed exercise text, WPM/accuracy counters: IBM Plex Mono (400/500/700)
- Section labels: IBM Plex Sans 600, uppercase, letter-spaced, small — like
  labels on a filing form.
- Fonts must support Hungarian diacritics (á é í ó ö ő ú ü ű) — never substitute
  a display font that drops ő/ű.

**Components:**
- **Top bar (persistent shell):** wordmark "TypeShift" in bureau green, links
  "Profile" and "Logout" right-aligned, thin bottom rule. Plain, no icons.
- **Cards = dockets:** paper cards on the paper background, 1px hairline border,
  4px corner radius, generous inner padding. Section labels stamped in the top
  corner of each card.
- **Buttons:** rectangular, 4px radius. Primary = bureau green fill, white,
  uppercase, letter-spaced label ("CLOCK IN"). Secondary = 1px ink outline,
  transparent. Destructive/decline = stamp red outline.
- **Stamps:** pass/fail shown as rotated (−4°) rubber-stamp badges with double
  border: "APPROVED" in approved green, "REJECTED — RETRY" in stamp red.
- **Live counters (WPM, accuracy, timer):** IBM Plex Mono, large, like a
  timeclock display, on a slightly darker paper chip.
- **Progress:** thin bureau-green bar with a brass marker for the 95% pass
  threshold.
- **Tables:** ledger style — hairline row rules, mono digits right-aligned,
  column headers uppercase letter-spaced.
- **Icons:** Material Symbols Outlined, thin, used sparingly (lock, check,
  keyboard). Most meaning comes from stamps, labels, and type.

**Platform:** desktop web first (1280px+), must remain usable at tablet width.
Light theme only. WCAG AA contrast. Real text everywhere, no lorem ipsum.

## Screens

1. **Login — "Start your shift."** Centered punch-clock card on paper: title
   "Start your shift", fields Username / Password, primary button "CLOCK IN",
   below-card link "No employment record yet? Open one." A thin brass strip
   across the card top like a punch-card slot. Error state: stamp-red inline
   message "Invalid username or password."

2. **Signup — "New Employment Record."** A taller application-form card titled
   "New Employment Record": fields Username, Password, Display name (optional),
   primary button "FILE RECORD", link back "Already employed? Clock in."
   Layout mirrors login for continuity.

3. **Dashboard — "Shift Review."** The home screen after login. H1 "Shift
   Review" + greeting line. Grid of dockets: (a) KPI row — current WPM,
   accuracy %, total practice minutes, current stage (mono counters, brass
   stage badge); (b) WPM history — simple line chart over past sessions on
   ledger grid lines; (c) stage completion — 5 small stations with progress;
   (d) miss-key heatmap — a QWERTZ key grid where redder keys = more misses;
   (e) recent sub-exam history — ledger table: date, stage, sub-exam, WPM,
   accuracy, APPROVED/REJECTED stamp.

4. **Stage Map.** The training program as five numbered stations down the page,
   like a career ladder: 1 QWERTZ Core (home/top/bottom row), 2 Symbol
   Sorcerer (punctuation, symbols, brackets), 3 Numpad & Math (10-key, ledger
   equations), 4 Hungarian Accents (á é í ó ö ő ú ü ű), 5 Real-World Tasks
   (chat, email, document, code comment). Each station card shows its 2–4
   sub-exams as checklist rows and its final "Graduation" entry — 1:
   "3-minute continuous QWERTZ passage", 2: "Formatted technical support
   ticket", 3: "Cashier Rush math evaluation", 4: "Formal Hungarian request
   letter", 5: "Three random tasks in sequence". States: locked
   (grayed, small lock, "LOCKED" label), unlocked (bureau green outline,
   "START SHIFT" button), passed ("APPROVED" stamp). Rule footnote: "Pass
   threshold: 95% accuracy. Stage 3 numeric results must be 100%."

5. **Prose Exercise.** Typing work screen. Top: sticky readout bar with mono
   timeclock counters — live WPM, accuracy, elapsed time, and a progress bar
   with the brass 95% marker. Center: the passage to type in IBM Plex Mono,
   large (18–20px), line height loose; typed-so-far in ink, correct characters
   solid, wrong characters in stamp red with red underline, cursor as a
   blinking block. Include Hungarian sample text with diacritics. A small
   "ABANDON SHIFT" ghost button bottom-left. First session only: a
   dismissible, non-blocking banner above the passage — "Reminder: set your
   OS keyboard to Hungarian QWERTZ."

6. **Numpad Exercise.** Numeric variant: ledger-style equation lines to copy
   ("142.50 + 38.25 = …") in mono, same sticky readout bar. Right side: an
   on-screen numeric keypad (QWERTZ numpad layout) where the next expected key
   is brass-highlighted and pressed keys flash bureau green; wrong presses
   flash stamp red. Footnote: "Numeric results must be 100% correct."

7. **Real-World Task Exercise (Stage 5).** Same sticky readout bar as the
   prose exercise, but the text to type sits in a container matching its
   real-world format — design four variants of this screen: (a) Chat burst —
   a short informal 1–3 sentence message in a chat-bubble column; (b) Email
   reply — subject line plus a 2–4 sentence body on a memo-style email card;
   (c) Document paragraph — one structured paragraph of business prose on a
   document sheet; (d) Code comment — a technical comment inside a small
   code block with line numbers. Every variant keeps the IBM Plex Mono type
   area with the same correct-ink / wrong-red character feedback.

8. **Results.** After a sub-exam or graduation. Big centered verdict first:
   "APPROVED" or "REJECTED — RETRY" stamp, large. Below, a stats docket — WPM,
   accuracy, elapsed time, characters typed — mono ledger columns. Then a
   compact error list ("missed keys: ő ×3, y ×1"). Buttons: primary "NEXT
   TASK" (or "BACK TO STAGE MAP"), secondary "RETAKE". Subtle confetti-free,
   official tone either way.

9. **Profile — "Employment record."** A single docket styled like a personnel
   file card: dl-style rows Username, Member since, Current stage, Keyboard
   layout "HU_QWERTZ", Interface language "English". Below, two small forms:
   "Display name" (input + SAVE) and "Change password" (current / new, min 8
   characters, CHANGE PASSWORD). Success/error feedback as small inline
   stamp-green / stamp-red messages.

## Rules

- Keep every screen in the same paper world: same background, same card
  language, same top bar.
- Use the exact app copy quoted above (it is the product's voice).
- Never use dark mode, photos, or illustrations; texture only via type,
  hairlines, and stamps.
- Uppercase letter-spaced labels only for buttons and section headers —
  never whole paragraphs.
