# Changelog

All notable changes to this project are documented in this file.

## [1.6.0] - 2026-10-18

### Added
- A dedicated **Student Detail screen** reached by tapping a student: view and edit their name, change their class, delete them, and browse a month-filtered attendance calendar you can share as an image. The standalone Report screen is retired and the main pager is now exactly two pages, Students and Calendar.
- Student Detail is redesigned around a header, an Attendance/Schedule pager, and counted segmented buttons. Its **Lessons** tab lets you assign lessons from the hours a student can actually take and shows "Your slot" — their own ten minutes — inside each block.
- A **Busy** tab for per-weekday busy appointments: a Material 3 time picker at exact-minute granularity, a 0.5h–6h duration slider in 30-minute steps, and a live "Ends at" preview. A block crossing midnight is rejected before you can save it.
- **Undo for student, lesson and busy deletes.** Removing a student keeps their attendance, lessons and appointments so the whole record can be restored; removed lessons and appointments can be re-assigned or re-inserted. All three flows surface through Snackbar undo actions.
- **Weekly lesson scheduling.** A Schedule page sets the hours you are available each weekday; each hour row shows occupancy as "x / y" and expands to list the assigned students. Assignment and busy management moved to Student Detail, where they belong.
- Lesson blocks are sized by **length in minutes (10–120, steps of 10)** instead of a headcount. Capacity is derived as `length / 10`, so it can no longer drift out of sync with the block it describes.
- The **running order** inside a block is recomputed from the block's students, their commitments and its length every time it is read, so the order you see no longer depends on the order the database happened to return rows in.
- **Backup and restore now carry your schedule.** The export always writes the full availability plan plus each selected student's lessons and busy appointments, and import merges them additively — overlapping hours are skipped, imported busy wins over existing lessons, and existing hours grow to fit imported lessons.
- **Calendar redesign:** a tactile grid of circular day cells with distinct states per day, and an attendance dialog extracted into its own component with an attendance meter, a legend and a responsive layout.
- The calendar now shows **which dates already have attendance**, with undoable feedback, and refuses to mark future dates.
- **Students list redesign:** search, a selection-scope banner for bulk actions, unified class + search + selection filtering with a derived total count, an undo delete, and a confirmation dialog before an export deletes anyone.
- A **clear class filter** action in the attendance dialog, and a dedicated state for when you have no classes yet.
- Room schema **v4**, adding `available_lesson_hours`, `lesson_assignments` and `busy_appointments` with cascading deletes, plus the v3 → v4 migration.

### Changed
- Empty class-filter selection now shows unassigned students only. An empty filter and an "all classes" filter are no longer the same thing.
- Backup format **v3**, signalled twice: the version is bumped *and* the field is renamed to `durationMinutes`, so either signal alone identifies the format. v2 files still import and are converted at the merge boundary; anything older or newer is refused with nothing written.
- Import is bounded twice — documented record caps, and a 32 MB file-size guard enforced *before* decoding. A cap consulted after decoding cannot prevent an out-of-memory crash, because the memory is already spent by the time it is read.
- Anything skipped, capped or malformed during an import is counted and reported in the summary. A partial import is still an import.
- A student's commitment is now validated against **their own ten minutes** rather than the whole block, so a commitment later inside a block no longer evicts or rejects them.
- Anything that could drop students now works in two phases: it proposes a plan without writing, names who would be dropped, and commits only after you confirm. A reported removal is actually carried out.
- Shrinking a block past its occupancy proposes first and confirms second, rather than silently overfilling.
- A busy-overlap conflict now takes priority over a full block in the Add Lesson dialog, for status text, row colours and trailing icon alike. A full-only hour still reads as full.
- The Students FAB stays hidden once it hides, returning only on an upward scroll or when the list is back at the top.
- Error and confirmation messages continue to use Material Snackbars; all new strings ship in English and Arabic.

### Fixed
- **Overlapping blocks were accepted.** The overlap check used the *proposed* block's length where the *existing* block's length was needed, so an existing 10:00–12:00 block plus a new 11:00–11:10 block passed. Both pre-existing tests used equal lengths, the one shape in which the error is invisible.
- **Moving a block's start time bypassed confirmation**, writing the new start and then evicting whoever collided — a student gone on a toast, with no prompt and no way to decline.
- **A confirmed shrink did nothing.** It named the students it would drop and left them in place, so a "confirmed" shrink silently kept a block over capacity.
- **Import broke the export/import round trip.** A student's commitments were tested against the block's whole extent before their lesson was planned, so a 09:30 commitment cost them a valid 08:00 slot. Since a backup is exported from lessons that were themselves valid, this silently discarded exactly the lessons the feature exists to accommodate.
- `SoundPlayer` is context-aware and checks the ringer mode, instead of playing over an active ring.
- The app now has a single inset owner and animator-safe navigation transitions, removing the flicker and double-applied insets seen on screen changes.

### Under the hood
- **The v3 → v4 migration rebuilds its table** rather than using `ALTER TABLE … RENAME COLUMN`, which needs SQLite 3.25 (API 30) while this app supports API 26 — that statement fails outright on a supported device. The rebuild sets `PRAGMA legacy_alter_table` first, because on newer SQLite a rename rewrites the child foreign keys to point at the discarded copy, whose `DROP` would then cascade-delete every lesson assignment. Covered by a migration test that fails if assignments are lost, a foreign key dangles, the temporary table survives, or the weekday index is not recreated.
- **Lint now fails the build.** `abortOnError = false` is gone, so lint errors break `./gradlew check` and `assembleRelease` rather than being reported and ignored — with no CI in this repository, a gate that always passes is not a gate. The 12 pre-existing errors are baselined so the build stays green today while any newly introduced issue still breaks it.
- **Instrumented tests stopped mocking ViewModels.** `mockk-android` cannot mock final ViewModel classes on device; one such mock passed on API 35 and failed on API 26 because the stub never matched. The Calendar and Schedule screens now expose stateless content composables driven by hand-rolled `MutableStateFlow` harnesses, matching the existing Schedule pattern.
- Documented the inward-only dependency rule (`presentation → domain ← data`), the ViewModel delegate pattern used to keep ViewModels under the 300-line cap, and the reproducible local signing setup. `signing.properties.example` is now tracked so the build is configurable without ever committing a keystore.
- Constitution amended to v1.2.0.
- Build upgrades: Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.21, KSP 2.3.6, Compose BOM 2026.09.00, Room 2.8.5, JUnit 6.1.3, core-ktx 1.19.1.

[1.6.0]: https://github.com/Nenoeldeeb/Absence-Record/releases/tag/v1.6.0
[1.5.0]: https://github.com/Nenoeldeeb/Absence-Record/releases/tag/v1.5.0