---
name: Absence Record
description: A cheerful, classroom-wall-calendar attendance tracker for teachers
colors:
  daylight-blue: "#0062FF"
  on-daylight-blue: "#001432"
  blue-chalk: "#66A1FF"
  deep-blue-chalk: "#003C96"
  amethyst-chalk: "#6E14EB"
  on-amethyst-chalk: "#F8EFFF"
  spring-chalk: "#69FF00"
  spring-chalk-deep: "#3B9900"
  roll-call-red: "#FF0A00"
  paper: "#FFFFFF"
  warm-paper: "#F2F2F2"
  ink: "#1A1A1A"
  graphite: "#666666"
  hairline: "#CCCCCC"
  night-paper: "#1A1A1A"
  on-night-paper: "#E6E6E6"
typography:
  display:
    fontFamily: "Roboto, sans-serif"
    fontSize: "57sp"
    fontWeight: 400
    lineHeight: "64sp"
    letterSpacing: "-0.25sp"
  headline:
    fontFamily: "Roboto, sans-serif"
    fontSize: "28sp"
    fontWeight: 400
    lineHeight: "36sp"
  title:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
    letterSpacing: "0.15sp"
  body:
    fontFamily: "Roboto, sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0.5sp"
  label:
    fontFamily: "Roboto, sans-serif"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "0.1sp"
rounded:
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  pill: "50%"
spacing:
  xs: "4dp"
  sm: "8dp"
  md: "16dp"
  lg: "24dp"
components:
  day-cell:
    backgroundColor: "transparent"
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    size: "48dp"
  day-cell-marked:
    backgroundColor: "{colors.amethyst-chalk}"
    textColor: "{colors.on-amethyst-chalk}"
    rounded: "{rounded.pill}"
    size: "48dp"
  day-cell-today:
    backgroundColor: "{colors.daylight-blue}"
    textColor: "{colors.on-daylight-blue}"
    rounded: "{rounded.pill}"
    size: "48dp"
  button-primary:
    backgroundColor: "{colors.daylight-blue}"
    textColor: "{colors.on-daylight-blue}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    height: "40dp"
    padding: "0 24dp"
  button-outlined:
    textColor: "{colors.daylight-blue}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    height: "40dp"
    padding: "0 24dp"
  list-item-selected:
    backgroundColor: "{colors.blue-chalk}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
  attendance-present-row:
    backgroundColor: "rgba(59, 153, 0, 0.5)"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
  empty-state:
    textColor: "{colors.graphite}"
---

# Design System: Absence Record

## Overview

**Creative North Star: "The Classroom Wall Calendar"**

Absence Record is the wall calendar that lives at the front of a real classroom — the one a teacher glances at mid-lesson to see the month in a second, then reaches over and touches one circle. The whole interface is built around that glance-and-touch rhythm: a big friendly month grid of perfect circles, three colors doing exactly three jobs, and a name list that reads like a well-kept roll-call register. There is nothing dashboard-y or spreadsheet-cold about it; the metaphor is physical, warm, and reassuringly familiar.

The system is governed end-to-end by Material Design 3: standard components, standard type scale, standard navigation and interaction. Brand expresses through Material theming — the palette, the circular calendar geometry, and the consistent green/purple/blue status grammar — never through off-spec controls. Density is comfortable and scannable, built for one-handed use in a 40-minute class period: touch targets are never smaller than 48dp, every row is a tap target, and a single glance at a circle tells you whether a day is marked.

The palette reads cheerful and friendly — bright paper whites and warm grays against three saturated chalk accents — while the structure stays quietly precise. Confirmations use snackbars, interruptions use Material dialogs, and the calendar is always the hero, never a widget buried in a page.

**Key Characteristics:**

- One hero element: the circular day cell (the classroom wall calendar)
- Three chalk accents, three fixed meanings: today (blue), marked day (purple), present (green)
- Material 3 defaults for shapes, elevation, and motion — the calendar geometry is the only signature
- Bilingual by construction: names and dates render in content text direction, the grid stays LTR
- Confident, friendly fill colors paired with dark-ink on-color text in light mode

## Colors

A warm paper-and-chalk world: bright neutrals for surfaces, three saturated chalk accents, and one roll-call red reserved for destructive and error states. Colors are light-scheme canonical; the dark scheme uses the deep variants of the same chalk tones. **On Android 12+, the user's wallpaper (Material You dynamic color) replaces this palette at runtime** — this documented palette is the identity that must survive as the static fallback on older devices and in any preview, test, or brand asset.

### Primary

- **Daylight Blue** (#0062FF): The "today" color and the primary action color. Fills the current day's circle, the primary buttons, and the active sort control. Everything the teacher *can do now* is blue.
- **On Daylight Blue** (#001432): A deep, almost-black navy — primary text and icons sit on Daylight Blue with this ink in light mode, a deliberately dark-contrast choice on a bright accent.
- **Blue Chalk** (#66A1FF): Primary container (light). The selected student's row in the roster and multi-selection highlight.
- **Deep Blue Chalk** (#003C96): Primary container (dark mode).

### Secondary

- **Amethyst Chalk** (#6E14EB): The "marked" color. Fills any circle on the calendar with attendance recorded for that date. One meaning, never swapped with blue.
- **On Amethyst Chalk** (#F8EFFF): Near-white lavender for the date number on a marked circle.

### Tertiary

- **Spring Chalk** (#69FF00): The "present" accent. Light-mode tertiary role and the launcher icon's signature color (#62FF00).
- **Spring Chalk Deep** (#3B9900): Dark-mode tertiary, and at 50% opacity the wash behind an "present" attendance row.

### Neutral

- **Paper** (#FFFFFF): Background in light mode.
- **Warm Paper** (#F2F2F2): The resting surface in light mode.
- **Ink** (#1A1A1A): Text on paper. Body, list text, calendar numerals.
- **Graphite** (#666666): Outline and muted/empty-state text.
- **Hairline** (#CCCCCC): Divider lines between list rows.
- **Night Paper** (#1A1A1A) / **On Night Paper** (#E6E6E6): Surface and text in dark mode.

### Named Rules

**The Three-Chalk Rule.** Daylight Blue, Amethyst Chalk, and Spring Chalk each claim exactly one meaning — today, marked, present — and never trade roles. If a new state needs a fourth meaning, add a role; do not repurpose an existing chalk.
**The Paper Body Rule.** Red (#FF0A00) is reserved for destructive actions, errors, and the absent mark. It never decorates; it always signals.
**The Wallpaper Caveat Rule.** Because Material You can recolor the app from a phone wallpaper, no UI decision may depend on a specific hue surviving — only on role contrast and the status grammar (icon + color, never color alone).

## Typography

**Body Font:** Roboto (system, via `FontFamily.Default`)

No custom display face. The type system is the Material 3 scale with one deliberate tweak: `bodyLarge` carries slightly generous letter spacing (0.5sp) for scannability of bilingual name lists.

**Character:** Quiet, legible, and entirely neutral — the type steps back and lets the chalk colors and circles carry the personality. Names use content text direction so Arabic and English mix correctly in one list.

### Hierarchy

- **Display** (Regular, 57sp, 64sp line): Reserved for the month title and any large calendar context; rarely used on small screens.
- **Headline** (Regular, 28sp, 36sp line): Large modal contexts.
- **Title** (Medium, 16sp, 24sp line, 0.15sp): Screen titles, the month header, sort announcements ("Students sorted by (…)"). `titleMedium` is the working heading voice.
- **Body** (Regular, 16sp, 24sp line, 0.5sp): Student names, dialog content, calendar numerals. `bodyLarge` is the workhorse.
- **Label** (Medium, 14sp, 20sp line, 0.1sp): Button text and dropdown entries.

### Named Rules

**The sp Rule.** All type is sized in sp and follows system font scaling. Never fixed px. Test name lists and calendar numerals at large font settings.

## Layout

A swipe-driven three-screen pager (Students, Calendar, Report) with no bottom navigation bar — horizontal swiping is the entire navigation metaphor, matching the wall-calendar "flip the page" feel. The Calendar is the default first view.

- **Rhythm:** 16dp horizontal screen padding; 8dp vertical rhythm between stacked controls; 12dp between stacked form fields; 2dp inset around each calendar cell.
- **Touch floor:** every interactive target is at least 48dp, with the calendar grid guaranteeing it via `sizeIn(minWidth = 48.dp, minHeight = 48.dp)` and a 1:1 aspect ratio per cell.
- **Calendar sizing:** capped at 400dp wide in portrait so cells never balloon; in landscape it scales proportionally to height and centers itself.
- **RTL handling:** the app is fully Arabic-capable, but the calendar month grid and day-of-week header are *always* rendered LTR (dates and weeks have one natural direction); names and prose follow content direction.

## Elevation & Depth

Material 3 defaults, no custom stance. Depth is expressed through tonal layering — the `surfaceContainer` family in the theme, with light-mode surfaces stepping up from Paper through Warm Paper — and through the Material default dialog elevation. Lists do not cast shadows; rows are separated by Hairline dividers, which keeps the wall-calendar flatness intact. The only "elevated" objects in the system are the dialogs and dropdowns that Material raises by default.

## Shapes

Material 3 default corner vocabulary (small 8dp, medium 12dp, large 16dp) with one signature geometry: **the circle**. Calendar day cells clip to a full circle (`CircleShape`), and buttons use the M3 full-pill treatment. Rounded rects are for containers and dialogs; circles are for days; pills are for buttons and filters.

## Components

### Day Cell (Signature)

The hero component — a perfect circle, 1:1 aspect ratio, 48dp minimum. Three states, three fills, no borders:

- **Default:** transparent fill, Ink numeral.
- **Today:** Daylight Blue fill, On Daylight Blue numeral.
- **Marked:** Amethyst Chalk fill, On Amethyst Chalk numeral.
  Selected state is announced semantically ("Day 15, Present, Today") so TalkBack matches the visual grammar. Empty grid slots render nothing.

### Buttons

- **Shape:** full pill (50% radius), Material 3 default height (40dp) and 24dp horizontal padding.
- **Filled (Primary):** Daylight Blue fill, On Daylight Blue label. The confident action: "Add", "Save", the sort toggle in report controls, import.
- **Outlined:** transparent fill, Daylight Blue label and stroke. The secondary action: the class filter trigger.
- **Text:** no fill, Daylight Blue label. Dialog dismissal ("Cancel", "Close") and quiet actions.

### List Items

- **Roster row:** a student's name at `bodyLarge` in content text direction, Hairline divider below. Selected rows fill with Blue Chalk and a check icon leads; multi-select uses long-press to enter.
- **Attendance row:** one row per student in the day dialog. Present rows wash with Spring Chalk Deep at 50% opacity and lead with a check icon in Daylight Blue; absent rows sit transparent with a close icon in Graphite. Color is never the sole signal — icons and text labels carry the state.

### Filter (Class Checkbox Dropdown)

An OutlinedButton that reads its state ("1 class selected", "No filter"), expanding a Material dropdown of checkbox rows. Used identically on Students, Calendar, and Report — one shared component, one shared filter state across screens.

### Inputs / Fields

OutlinedTextField everywhere, including read-only exposed dropdowns for month and class selection. Labels float per M3; a trailing clear icon appears on the month filter when a value is set.

### Dialogs

Material AlertDialog: centered title (the date and attendance count in the day dialog), scrollable body, and a footer of a TextButton dismiss plus an optional filled Button confirm. Never a bare toast for decisions.

### Empty States

A single centered line of Graphite body text ("No students added yet.") — quiet, friendly, and unadorned, in keeping with the rest of the wall.

## Do's and Don'ts

### Do:

- **Do** keep the three chalk meanings fixed: blue = today/primary action, purple = marked day, green = present.
- **Do** pair color with an icon or label for every status — TalkBack and color-blind users rely on it.
- **Do** keep calendar cells circular and ≥48dp, with dates forced LTR even in Arabic UI.
- **Do** use the shared class-filter component so all three screens feel like one app.
- **Do** reach for snackbars for confirmations and transient feedback.
- **Do** use Material 3 roles (`MaterialTheme.colorScheme.*`) rather than raw hex in Compose, so dynamic wallpaper theming keeps working.

### Don't:

- **Don't** introduce arbitrary drop shadows — depth comes from tonal surface layering and Material defaults.
- **Don't** repurpose Red for anything but destructive, error, or absent states.
- **Don't** add a bottom navigation bar — navigation is the swipe pager.
- **Don't** set type in fixed px; always sp.
- **Don't** put a new status color in the grid without giving it an icon companion.
- **Don't** rely on a specific hue surviving Material You recolor — design by role contrast.
