# Contracts — UI & Navigation (Schedule)

**Feature Branch**: `007-lesson-scheduling` | **Date**: 2026-08-08

## Navigation contract

- `MainScreen` pager (`app/MainActivity.kt`): `pageCount = { 3 }`.
  - Page 0: `StudentsScreen`
  - Page 1: `CalendarScreen` (attendance report)
  - Page 2: `ScheduleScreen` **← new, "next to the attendance report" (user requirement)**
- `AppNavigation.kt`: `rememberPagerState(initialPage = 1, pageCount = { 3 })`.
  `AppDestination` back-stack unchanged (`MainPager` | `StudentDetail(studentId)`).
- Swiping right from Calendar reaches Schedule; pager swipe + accessibility
  "page" semantics handled by `HorizontalPager`'s `PagerState` (existing behavior).
- Tapping an assigned student's name in an expanded hour on the Schedule page
  pushes `AppDestination.StudentDetail`; on return the Schedule page keeps its
  selected weekday and expanded-hour state (held in `ScheduleViewModel` state).
- `ScheduleScreen` is a stateless root composable receiving `scheduleUiState` +
  `onEvent` + `onStudentClick` (mirrors `CalendarScreen`).

## Schedule page structure (availability-only, 2026-08-09 spec)

```
ScheduleScreen
├── WeekdaySelector               // 7 chips, Saturday-first (matches ComposeCalendar)
└── AppointmentsTab               // LazyColumn of expandable HourRow for selected weekday
```

- **Weekday selector**: Saturday-first row of 7 `FilterChip`s; selected chip =
  filled `FilterChip`; each chip has `contentDescription` from existing
  `DayOfWeek.toUiText()` resources (Arabic + English). Reused by the Student
  Detail schedule section.
- **No tabs on the Schedule page**: the `SecondaryTabRow` (and the Appointments /
  Busy Appointments tabs) moved to the Student Detail screen (see below). The
  `ScheduleTab` enum was removed.

## Appointments tab

- Empty state: `EmptyStateMessage` ("No lesson hours set for this day yet.") +
  primary "Add hour" `Button`.
- Each `HourRow`: time range `9:00 – 10:00`, occupancy text `"2 / 5"` (never color
  alone), `isFull` → "Full" label, icons:
  - Expand/collapse (chevron) — `contentDescription` "Expand hour 9:00" /
    "Collapse hour 9:00"
  - Edit (opens `HourDialog` prefilled) — "Edit hour 9:00"
  - Delete (opens `ScheduleConfirmDialog`) — "Delete hour 9:00"
- **Expanded state** (`expandedHourIds: Set<Int>` in `ScheduleScreenState`,
  toggled by `ToggleHourExpanded`): lists the assigned student names READ-ONLY
  (no assign action offered — assignment is per-student from the profile);
  tapping a name calls `onStudentClick(studentId)` → `AppDestination.StudentDetail`.
- Occupancy derived from `HourWithOccupancy`; all row actions ≥ 48dp.

## Student Detail — weekly schedule section (interactive)

Replaces the old read-only schedule card and the Schedule page's Appointments /
Busy tabs. A Saturday-first `WeekdaySelector` (reused from schedule/components)
plus a Material 3 `SecondaryTabRow` with **"Lessons"** | **"Busy"** tabs, scoped
to the open student and the selected weekday.

- **Lessons tab** (`StudentScheduleLessonsTab`): lists the student's lessons for
  the selected weekday (time range + unassign icon). "Add lesson" opens
  `AddLessonDialog` scoped to that weekday and is unavailable when the student
  already has a lesson that day (FR-019). Unassign resolves the hour id from
  `observeHoursForWeekdayUseCase` (match `startMinutes` + `assignedStudentIds`).
- **Busy tab** (`StudentScheduleBusyTab`): lists the student's busy appointments
  for the selected weekday (time range `9:00 – 9:45`, edit/delete icons) with
  "Add busy appointment" button and empty state.
- Changes reflect immediately in the Schedule page occupancy (flow auto-refresh).

## Dialogs (Material 3 `AlertDialog`/`BasicAlertDialog`, `UiText`-backed)

1. **HourDialog** (schedule/) — start-time slot dropdown (48 × :00/:30,
   `TimeFormatter` labels), `maxStudents` number field (≥ 1), save/cancel.
   Validation errors surface inline as `supportingText` + `isError`.
2. **ScheduleConfirmDialog** (schedule/) — destructive deletes
   ("Delete this lesson hour?").
3. **AddLessonDialog** (studentdetail/dialogs/) — lists the eligible hours for the
   selected weekday with occupancy; ineligible hours (full FR-007, busy-overlap
   FR-009) are disabled with a reason label; when the weekday has no hours it
   shows a "no hours for this day" hint pointing to the Schedule page (FR-018).
4. **BusyAppointmentDialog** (studentdetail/dialogs/) — start slot dropdown +
   duration dropdown (15/30/45/60/90/120/150/180/240), with live "Ends at HH:mm"
   preview that shows midnight-crossing as an inline error. **Preview-and-confirm
   (FR-011)**: when the new/edited range overlaps one of the student's existing
   lessons, the dialog lists the conflicting lesson(s) and the confirm button
   becomes a teacher confirmation before the repository save is called.

## Accessibility contract

- Every interactive element has a meaningful `contentDescription` (see labels
  above); touch targets ≥ 48dp; occupancy/capacity conveyed by text, not color;
  tab selection + weekday selection exposed to TalkBack via Material semantics;
  all dialogs are focus-trapped `Dialog`s with correct dismiss behavior.
- Test each screen with TalkBack before shipping.

## Strings

All labels above are new entries in `strings.xml` (en) + `values-ar/strings.xml`
(ar) and reached via `UiText` (`.StringResource`/`.PluralResource`). No hardcoded
strings. Time text is rendered by `TimeFormatter` (system locale + 12/24h
preference) — Arabic AM/PM ("9:00 ص") comes from `android.text.format.DateFormat`.
