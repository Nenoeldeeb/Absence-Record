# Changelog

All notable changes to this project are documented in this file.

## [1.5.0] - 2026-08-04

### Added
- Multi-select class filter on the Calendar, Report, and Students screens. Select any combination of classes; your selection stays in sync across all screens via a shared `ClassFilterRepository`.
- Class filter moved into the attendance dialog on the Calendar screen, right where you mark attendance, with live count updates.
- Deleting a class now unassigns its students first (they remain in your records) and automatically cleans up the filter selection.
- Consistent Snackbar notifications with clear, localized error messages across Students, Report, and Calendar screens.
- Accessibility: 48dp calendar touch targets, descriptive TalkBack labels for dates, attendance status, student selections, and class checkboxes; disabled actions now announce why they are disabled.
- Arabic and English translations for all new UI strings and error messages.

### Changed
- Empty class-filter selection now shows unassigned students only (previously the default was "All Classes"); the "All Classes"/"Unassigned" filter options were replaced by per-class checkboxes.
- Error and confirmation messages now use Material Snackbars instead of Toasts or inline dialog text.
- Report calendar preview is no longer interactive (no ripple on day taps).

### Fixed
- Import preview date-count formatting (`%1$s` -> `%1$d`).
- "Select All / Deselect All" typo (double space removed).
- Arabic duplicate-class-name error string.

### Under the hood
- Unified error handling across the data, domain, and presentation layers using typed errors and a shared UI mapper.
- Architecture cleanups: StudentsViewModel split into delegates and handlers; 10 oversized files split to meet the 300-line limit.
- Build upgrades: Gradle 9.5.0, AGP 9.3.0, Kotlin 2.4.10, JVM 25, compileSdk/targetSdk 37, Compose BOM 2026.06.00, and updated Room, kotlinx-datetime, kotlinx-serialization, and coroutines libraries.

[1.5.0]: https://github.com/Nenoeldeeb/Absencerecord/releases/tag/v1.5.0
