# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Classroom teachers tracking daily attendance for their own classes. The app serves a single active user — the teacher — not a multi-role school system.

## Product Purpose

A focused, offline-first Android app for recording and reviewing student attendance. Purpose-built to replace paper roll calls and spreadsheets with a tactile calendar interface, class grouping, and shareable reports — all without accounts or internet.

## Positioning

Simple, private, bilingual (Arabic/English), and built for the classroom reality: no login, no cloud, no setup overhead. Absence Record combines all four traits in a way no other attendance tracker does.

## Operating Context

A teacher using Absence Record mid-lesson or at end of day on their phone or tablet. Speed and one-handed use matter — marking attendance is a quick toggling action, not a data entry task. The device may have no internet. The teacher may switch between Arabic and English.

## Capabilities and Constraints

- Track student presence/absence per day on a calendar view
- Manage student roster: add, edit, delete with optional photos
- Group students into classes; filter attendance and reports by class
- Generate monthly attendance reports and share as calendar images
- Import/export full data as JSON for backup and migration
- Sound effects for marking attendance (optional feedback)
- Fully offline — no network permissions required
- Single-user — no sync, no multi-teacher, no accounts
- Room SQLite persistence with kotlinx-datetime (no java.time)
- Bilingual strings (English + Arabic) with Material 3 dynamic color
- GPL v3.0 licensed
- Undecided: multi-device sync, per-class attendance reports, student photo capture

## Brand Commitments

- Name: **Absence Record** (سجل الغياب)
- App icon exists in repo
- GPL v3.0 open-source
- No brand guidelines, no visual identity constraints beyond existing Material 3 dynamic color scheme

## Evidence on Hand

- Full source code in `/app`
- App icon: `app/src/main/ic_launcher-playstore.png`
- README with feature list and build instructions
- AGENTS.md with full architecture and coding conventions
- Spec docs in `specs/` (5 prior feature specs)

## Product Principles

1. **Tap, don't type.** Attendance marking must be faster than a paper roll call — every extra tap is a failure.
2. **Offline is the default.** The app works fully without internet. Syncing is optional future scope only.
3. **Bilingual by birth, not afterthought.** Arabic and English are co-equal from day one, not a localisation layer.
4. **One teacher, one device.** The data model and UI optimise for a single classroom teacher — adding multi-user must not complicate the primary user's experience.
5. **Surfaces reflect a calendar, not a spreadsheet.** The visual metaphor is a wall calendar with coloured dots, not a grid of checkboxes.

## Accessibility & Inclusion

All interactive composables must have meaningful content descriptions. Touch targets minimum 48dp. Colour is never the sole indicator of state; icons or text labels accompany status. TalkBack compatibility required.
