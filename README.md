# Absence Record

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="Application Icon" width="100" height="100">
</p>

Absence Record is a comprehensive Android application designed to streamline student attendance tracking. Built with modern Android development practices, it offers a robust solution for educators and administrators to manage student records, track daily attendance, and generate insightful reports.

## Features

- **Student Management**: Efficiently manage student profiles. Facilitates adding new students, editing existing details with support for photos and personal information, and viewing a comprehensive list of all students. Tapping a student opens a Student Detail screen where you can rename them, change their class, delete them, and view their attendance history. The Students top bar includes a sort control (by name or by attendance) and a multi-select class filter.
- **Calendar & Attendance Tracking**: A visual calendar interface designed for easy attendance recording. Allows quick toggling of student presence or absence for specific dates directly from a calendar view. The attendance dialog includes a shared multi-select class filter (also used on the Students screen) with checkboxes for filtering students by one or more classes, with dynamic count updates and attendance clamping.
- **Lesson Scheduling**: A dedicated Schedule page (pager page 2) holds the weekly availability plan — per-weekday lesson blocks with a start time (entered via a standard Material3 time picker at exact-minute granularity) and a **lesson length** of 10–120 minutes in 10-minute steps, chosen with a single continuous slider (the derived student count is shown beside it and follows the drag). Capacity is never stored: it is `length ÷ 10`, so a 60-minute block holds six students. Each block is expandable to show its students **in running order**, each with the exact ten minutes they occupy (tap a name to open that student's profile).

  Students are **sequenced by who has to leave first**: each one's next relevant commitment on that day drives their position, ties break alphabetically then by id, and anyone with no commitment runs last. That order is *derived on every read* from the block's students, their commitments and its length — never stored, never cached — so reopening the app shows the same order. Adding a student re-plans the whole block rather than appending, and is refused outright if any resulting slot would collide with anyone's own commitment; shortening a block that would drop students asks first and names who.

  Per-student lesson assignment and busy appointments (start via a Material3 time picker, 0.5h–6h duration slider in 30-minute steps, with a live end-time preview and midnight-crossing validation) are managed from the Student Detail screen via **Lessons** and **Busy** tabs; busy appointments that would collide with a lesson are previewed in the dialog and applied only after the teacher confirms. Scheduling data is persisted in Room (`available_lesson_hours` stores the length in minutes, plus `lesson_assignments` and `busy_appointments`).
- **Comprehensive Reporting**: Per-student attendance reports are generated from the Student Detail screen — filter by month and share a calendar image of the month's attendance. The old standalone Reports page has been retired; pager navigation is now exactly three pages (Students ↔ Calendar ↔ Schedule).
- **Data Import/Export**: Robust data management capabilities allowing users to export database records for backup purposes and import them to restore data or migrate between devices. Support for JSON format ensures compatibility. The current backup format is **version 3**, which writes each block's `durationMinutes`; version 2 files (carrying a student count) still import and are converted on the way in, while older or newer versions are refused with a message and nothing written. Import is bounded on two fronts — documented record caps (students, blocks, assignments, attendance dates per student), and a 32 MB file-size check applied **before** the file is decoded, so an implausibly large file is refused rather than loaded. Entries that are malformed, ambiguous or past a cap are skipped and counted, never guessed at, and the summary says how many.

## Tech Stack

This project leverages the latest tools and libraries in the Android ecosystem to ensure performance, maintainability, and scalability.

### Languages & Frameworks

- **[Kotlin](https://kotlinlang.org/)**: The primary programming language used, ensuring type safety and concise code.
- **[Jetpack Compose](https://developer.android.com/jetpack/compose)**: Android's modern toolkit for building native UI, used for all user interface components.
- **[Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)**: Used for asynchronous programming and reactive state management.
- **[KSP (Kotlin Symbol Processing)](https://github.com/google/ksp)**: A powerful API for building lightweight compiler plugins, used by Room.

### Libraries

- **[Room Database](https://developer.android.com/training/data-storage/room)**: An abstraction layer over SQLite to allow fluent database access and robust local data storage.
- **[Kotlinx Serialization](https://kotlinlang.org/docs/serialization.html)**: Provides accurate and efficient JSON serialization/deserialization for import/export features.
- **[Kotlinx Datetime](https://github.com/Kotlin/kotlinx-datetime)**: A multiplatform library for working with date and time.
- **[Accompanist](https://github.com/google/accompanist)**: A collection of extension libraries for Jetpack Compose.

### Architecture & Design Principles

- **Clean Architecture**: The codebase is strictly separated into Presentation, Domain, and Data layers to ensure testability, maintainability, and independence of frameworks. Dependencies point **inward only** — `presentation/` → `domain/` ← `data/`. `domain/` never imports from `data/` or `presentation/`, and `presentation/` reaches persistence only through use cases.
- **MVI (Model-View-Intent)**: Used for state management in the Presentation layer by wrapping them in a single UiState data class and for handling side effects by wrapping them in a sealed interface.
- **Repository Pattern**: Abstracts the data layer, providing a clean API for the domain layer to access data irrespective of the source (Database, API, etc.).
- **ViewModel Delegates**: Behaviour that would push a ViewModel past the 300-line cap is extracted into a delegate class under `presentation/screens/<feature>/delegates/`. The ViewModel retains state ownership while a delegate holds the injected action handler — e.g. `ScheduleViewModel` → `HourManagementDelegate`, `StudentsViewModel` → 4 delegates.
- **Composition-Based Navigation**: Navigation is implemented without a navigation library. `AppDestination` (sealed interface) + `AppNavigation` (composable) drive a `rememberSaveable` back-stack rendered with `AnimatedContent`, with `BackHandler` for pop support. Tapping a student pushes a `StudentDetail` destination onto the stack.
- **Room Migrations**: Schema changes require an explicit `Migration` with a version bump — destructive migration is never used, since it would destroy user data. Exported schemas are version-controlled under `app/schemas/` and every schema change ships with a migration test.
- **Synchronized Class Filtering**: A shared `ClassFilterRepository` (domain/repositories) backed by `ClassFilterRepositoryImpl` (data/repositories) holds an in-memory `selectedClassIds: StateFlow<Set<Int>>` (emptySet by default) that keeps the multi-select `ClassCheckboxFilter` in sync across the Calendar attendance dialog and Students screens. An empty selection shows unassigned students only; checked classes show enrolled students in those classes; there is no "Select All" entry.
- **SOLID Principles**: Adherence to key object-oriented design principles (SRP, OCP, LSP, ISP, DIP) for robust and scalable code.
- **Accessibility**: All user-facing composables must be accessible — content descriptions, 48dp touch targets, sufficient color contrast, and TalkBack compatibility.
- **UDF (Unidirectional Data Flow)**: Ensures consistent state management within the Compose UI, where state flows down and events flow up.

## Building

To get this project up and running on your local machine:

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Nenoeldeeb/Absence-Record.git
   cd Absence-Record
   ```

2. **Create your keystores** (one per build type). The paths below match the
   template in step 3 and are git-ignored:
   ```bash
   mkdir -p keystores
   keytool -genkeypair -v -keystore keystores/debug.jks -keyalg RSA \
     -keysize 2048 -validity 10000 -alias debug \
     -storepassword CHANGE_ME -keypassword CHANGE_ME \
     -dname "CN=Absence Record, O=Absence Record, C=US"
   ```
   Repeat with `keystores/release.jks` and an alias of your choice for release
   builds, and keep a backup — a lost release keystore means you can never
   update the app on Play.

3. **Create your own copy of `signing.properties` in the repository root** by
   copying the tracked template, then fill in your values:
   ```bash
   cp signing.properties.example signing.properties
   # Windows: copy signing.properties.example signing.properties
   ```
   The template contains every key the build reads:
   ```properties
   release.store.file=../keystores/release.jks
   release.key.alias=CHANGE_ME
   release.key.password=CHANGE_ME
   release.store.password=CHANGE_ME
   debug.store.file=../keystores/debug.jks
   debug.key.alias=CHANGE_ME
   debug.key.password=CHANGE_ME
   debug.store.password=CHANGE_ME
   ```
   - `*.store.file` paths are resolved relative to the `app/` module directory,
     so `../keystores/x.jks` points at `<repo-root>/keystores/x.jks`.
   - If a keystore was created with one password for both the store and the
     key, keep `*.key.password` and `*.store.password` identical.

4. **Build** (see [Development](#development) for the full list of commands):
   ```bash
   ./gradlew assembleDebug
   ```

> **Note**: `signing.properties` and `keystores/` are git-ignored — never commit
> them. Only `signing.properties.example` is tracked. The build reads
> `signing.properties` at configuration time, so **no** Gradle task can run
> until the file exists with every key filled in.

## Development

### Building

Requires a local `signing.properties` (see [Building](#building)).

```bash
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease      # Release build
./gradlew build                # Full build with tests
```

### Running Tests

```bash
./gradlew test                           # All unit tests
./gradlew testDebugUnitTest              # Unit tests only
./gradlew connectedDebugAndroidTest      # Instrumented tests (requires emulator/device)
./gradlew check                          # All checks (ktlint + lint + unit tests)
```

**Running Single Test:**

```bash
# Unit test example
./gradlew testDebugUnitTest --tests "dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatterTest"

# Instrumented test example (requires connected device/emulator)
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class="dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.ScheduleScreenAppointmentsTest"
```

> **Note**: Android Gradle Plugin 9 rejects the `--tests` flag on
> `connectedDebugAndroidTest`. Always filter instrumented tests with
> `-Pandroid.testInstrumentationRunnerArguments.class=...`.
>
> **Note**: `mockk-android` cannot mock final ViewModel classes on-device.
> Instrumented UI tests extract a stateless content composable and drive it
> through a hand-rolled harness (`MutableStateFlow` + event recorder) — see
> `ScheduleScreenTestHarness` / `ScheduleScreenContent`. MockK remains the
> standard for JVM unit tests.

### Code Quality & Linting

```bash
./gradlew ktlintCheck       # Check code style without fixing
./gradlew ktlintFormat      # Auto-format all Kotlin sources
./gradlew lint              # Android lint checks
./gradlew check             # All checks (ktlint + lint + tests)
```

> **Important**: `ktlintFormat` runs automatically before every build. Always run `./gradlew check` before committing to catch any issues.

> **No CI**: this repository has no automated pipeline. Every gate above is run
> locally and manually, so nothing will catch a violation for you.

### Pre-Commit Checklist

Before pushing your changes:

- [ ] Run `./gradlew ktlintFormat` to ensure code style compliance
- [ ] Run `./gradlew check` to validate all code quality checks and tests pass
- [ ] Write tests for new functionality (both success and failure paths)
- [ ] Keep strings in `res/values/strings.xml` (with Arabic translations in `values-ar/`)
- [ ] Ensure all source files (including tests) are under 300 lines
- [ ] Add a Room `Migration` + version bump (never destructive) and a migration test if the schema changed
- [ ] Verify accessibility: content descriptions, 48dp touch targets, TalkBack
- [ ] Never commit `signing.properties` or `keystores/` — only `signing.properties.example` is tracked
- [ ] Update `AGENTS.md` and `README.md` to reflect new features or technologies
- [ ] Follow naming conventions from [AGENTS.md](AGENTS.md), and use Conventional Commits with a scope

## Project Structure

```
app/src/main/java/dev/nenoeldeeb/education/absencerecord/
├── app/                  # Application class, DI container, ViewModel provider
├── domain/
│   ├── models/           # Domain models (Student, Attendance, etc.)
│   ├── repositories/     # Repository interfaces
│   ├── usecases/         # Use case classes
│   └── services/         # Service interfaces (Serialization, Dispatchers)
├── data/
│   ├── datasources/      # Room DAOs, local data sources
│   │   └── local/        # Room entities, DAOs, database
│   ├── repositories/     # Repository implementations
│   ├── mappers/          # Entity to domain mappers
│   └── utils/            # Data utilities
└── presentation/
    ├── screens/          # Screen composables, ViewModels, state, events
    │   ├── components/   # Shared UI components
    │   └── [feature]/
    │       ├── Screen.kt
    │       ├── ViewModel.kt
    │       ├── ScreenState.kt
    │       ├── ScreenEvent.kt
    │       ├── components/   # Screen-specific components
    │       ├── dialogs/      # Dialog components
    │       ├── handlers/    # Event handler classes
    │       └── delegates/    # State management delegates
    ├── theme/            # Material3 theme (Color, Type, Theme)
    └── utils/            # UI utilities (UiText, DateFormatter, etc.)
```

For detailed development guidelines, code style conventions, and architecture patterns, refer to [AGENTS.md](AGENTS.md).

## Contributing

Contributions are welcome! If you have suggestions for improvements or want to report a bug, please follow these steps:

1. **Fork the Project**: Create your own copy of the repository.
2. **Create your Feature Branch**: `git checkout -b feat/Amazing-feature` or `git checkout -b fix/Critical-bug`
3. **Make Your changes with tests**: Write Your own code and test It.
4. **Commit your Changes**: `git commit -m "Add some AmazingFeature"`
5. **Push to the Branch**: `git push origin feat/Amazing-feature` or `git push origin fix/Critical-bug`
6. **Open a Pull Request**: Submit your changes for review.

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

> This program comes with ABSOLUTELY NO WARRANTY; for details see the LICENSE file.
> This is free software, and you are welcome to redistribute it under certain conditions.
