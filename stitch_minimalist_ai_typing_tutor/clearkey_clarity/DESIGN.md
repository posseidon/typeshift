---
name: ClearKey Clarity
colors:
  surface: '#f7f9fb'
  surface-dim: '#d8dadc'
  surface-bright: '#f7f9fb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f6'
  surface-container: '#eceef0'
  surface-container-high: '#e6e8ea'
  surface-container-highest: '#e0e3e5'
  on-surface: '#191c1e'
  on-surface-variant: '#424754'
  inverse-surface: '#2d3133'
  inverse-on-surface: '#eff1f3'
  outline: '#727785'
  outline-variant: '#c2c6d6'
  surface-tint: '#005ac2'
  primary: '#0058be'
  on-primary: '#ffffff'
  primary-container: '#2170e4'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc6ff'
  secondary: '#505f76'
  on-secondary: '#ffffff'
  secondary-container: '#d0e1fb'
  on-secondary-container: '#54647a'
  tertiary: '#006947'
  on-tertiary: '#ffffff'
  tertiary-container: '#00855b'
  on-tertiary-container: '#f5fff6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#004395'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#f7f9fb'
  on-background: '#191c1e'
  surface-variant: '#e0e3e5'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 52px
  display-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '600'
    lineHeight: 42px
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 26px
    fontWeight: '600'
    lineHeight: 34px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-xl:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '400'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 26px
  label-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  type-target:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '500'
    lineHeight: 48px
    letterSpacing: 0.15em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 1rem
  margin: 2.5rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
---

## Brand & Style

This design system is tailored specifically for adult foundational learners (ages 25–50) who may have little to no prior computer experience. Learning keyboarding as an adult carries unique emotional friction: fear of breaking the machine, embarrassment, and sensory fatigue. The overarching brand tone is calm, supportive, patient, and grounded.

The aesthetic blends **warm minimalism** with **modern clarity**:
- **Generous Whitespace & Uncluttered Canvases:** Eliminates cognitive load and prevents accidental misclicks. Only essential actions and content are visible at any given moment.
- **Supportive Demeanor:** Reassuring, clear instructions replace computer jargon. Interface states emphasize progression rather than penalty.
- **Tactile Accessibility:** High-contrast focal targets, explicit hit areas, and visual stability ensure users always feel in control.

## Colors

The color palette centers on calming, confident blues balanced by warm, low-glare neutrals and restorative feedback tones:

- **Primary (`#3B82F6`):** An encouraging, reliable sky blue used for active focus targets, primary calls to action, and core navigational anchors.
- **Secondary (`#64748B`):** A soft, grounding slate gray used for sub-labels, secondary actions, non-essential borders, and structural framing.
- **Tertiary / Success (`#10B981`):** A gentle, restorative emerald for correct keystrokes, mastered milestones, and encouraging feedback. It avoids harsh neon tones to prevent eye strain.
- **Neutral Surface (`#F8FAFC`):** An off-white, warm slate canvas that softens eye fatigue compared to pure `#FFFFFF`. Paired with deep charcoal text (`#0F172A`) to ensure WCAG AAA contrast compliance without aggressive visual glare.
- **Error / Correction (`#F43F5E` / Warm Terracotta `#E11D48`):** Mild, warm clay tones instead of alarming bright reds, signaling correction opportunities without triggering performance anxiety.

## Typography

Typography prioritizes hyper-legibility and confidence:

- **Headlines (`Plus Jakarta Sans`):** Warm, geometric, and welcoming. Its open counters and friendly curves keep prompts accessible and non-intimidating.
- **Body & Prompts (`Inter`):** Systematic, clean, and unambiguous. Key distinguishing glyphs (like `I`, `l`, and `1`) are distinct to eliminate confusion during typing drills.
- **Typing Target Token (`type-target`):** Specifically designated for typing prompts, offering high letter-spacing (`0.15em`) and large size (`36px`) so adult beginners can comfortably track both the prompt and their finger placement without squinting.
- Minimum interactive body size is set to `16px`, avoiding small helper text that could strain vision.

## Layout & Spacing

The layout is built around a predictable, distraction-free grid:

- **Grid Model:** A balanced 12-column grid on desktop (max width 1140px, centered) with generous margins (`2.5rem`) to create an airy breathing perimeter. Single-column stacked flows are prioritized for mobile and tablet touch devices.
- **Component Breathing Room:** Components use generous internal paddings (`space-md` to `space-xl`) to establish high touch targets (minimum 48px height on all interactive controls).
- **Predictable Vertical Flow:** Linear information architecture avoids split-screen complexity. The learning prompt sits in the focal center, with instructions directly above and progress status below.

## Elevation & Depth

Visual hierarchy uses **tonal surfaces and soft ambient shadows** rather than dramatic elevation steps:

- **Flat Foundation:** The canvas rests at `#F8FAFC`, while cards and interactive panels sit on pure `#FFFFFF` containers to naturally draw focus.
- **Soft Ambient Shadows:** Elevated components use an ultra-diffused, gentle shadow (`0 2px 8px -2px rgba(15, 23, 42, 0.06), 0 8px 16px -4px rgba(15, 23, 42, 0.04)`). This creates a physical, soft feel without sharp drop-offs.
- **Focused State Outlines:** Active keys and input areas receive a distinctive 3px outline in primary blue (`#3B82F6`) with a 2px offset, ensuring unambiguous focus identification for motor and cognitive clarity.

## Shapes

The design system employs **Level 2 Roundedness** (0.5rem / 8px base radius, scaling to 1rem on larger containers):

- **Reassuring Softness:** Mildly curved edges remove visual sharpness and mechanical rigidity, conveying a safe, forgiving environment.
- **Interactive Feedback:** Virtual keyboard keys, inputs, and interactive action tiles share the same radius, ensuring consistent spatial familiarity across the screen.
- **Status Pills:** Badges and subtle indicators use nested pill shapes for immediate distinction from standard clickable buttons.

## Components

### Buttons
- **Primary Button:** Solid `#3B82F6` fill with bold white text, min-height 52px, horizontal padding `1.75rem`. Rounded corners (8px) with subtle ambient lift on hover.
- **Secondary / Calm Button:** Soft background (`#E2E8F0` or `#F1F5F9`) with slate text (`#1E293B`). Designed for secondary navigation ("Take a Break", "Repeat Audio"). Never uses red or negative phrasing.

### Inputs & Type Targets
- **Typing Practice Area:** Generously padded container (`space-xl`), bordered by a calm 2px neutral line. Letters in progress highlight with emerald underline; upcoming letters display high-contrast neutral slate; errors show a warm terracotta background tint with gentle, non-jarring feedback.
- **Form Inputs:** 48px height, 16px font size, clear permanent label above the field (no disappearing placeholders).

### Cards & Virtual Key Guides
- **Instructional Cards:** White surface `#FFFFFF`, 1px soft border `#E2E8F0`, rounded 16px (`rounded-lg`), padded with `space-lg`.
- **On-Screen Keyboard Guides:** Keys modeled as tactile soft tiles with clear letterforms, showing dynamic finger-placement highlights in soothing blue without rapid, anxiety-inducing flashing.

### Progress & Feedback Indicators
- **Gentle Progress Bars:** 12px height with rounded caps. The track uses a neutral slate tint (`#E2E8F0`) while the fill uses emerald (`#10B981`) or sky blue (`#3B82F6`). Progress is expressed through smooth, easing animations.
- **Accompanying Encouragement:** Progress is paired with positive text (e.g., "5 of 10 letters mastered — Great pace!") instead of cold percentages or timers.

### Anxiety-Reducing Empty & AI Loading States
- **AI Latency & Processing States:** Displays calm, gentle breathing pulse indicators (slow 2-second ease) paired with conversational reassurance: *"Listening to your keystrokes..."* or *"Preparing your next custom exercise..."*
- **Empty States:** Accompanied by warm, friendly illustrations or clear, single-step guidance encouraging the user to take their first step.