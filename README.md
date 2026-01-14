# Absence Record

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="100" height="100">
</p>

Absence Record is a comprehensive Android application designed to streamline student attendance tracking. Built with modern Android development practices, it offers a robust solution for educators and administrators to manage student records, track daily attendance, and generate insightful reports.

## Features

- **Student Management**: Efficiently manage student profiles. Facilitates adding new students, editing existing details with support for photos and personal information, and viewing a comprehensive list of all students.
- **Calendar & Attendance Tracking**: A visual calendar interface designed for easy attendance recording. Allows quick toggling of student presence or absence for specific dates directly from a calendar view.
- **Comprehensive Reporting**: Generate detailed attendance reports to analyze student attendance trends over custom date ranges. Reports help in identifying patterns and maintaining accurate records.
- **Data Import/Export**: Robust data management capabilities allowing users to export database records for backup purposes and import them to restore data or migrate between devices. Support for JSON format ensures compatibility.

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

### Architecture & Design Principles

- **Clean Architecture**: The codebase is strictly separated into Presentation, Domain, and Data layers to ensure testability, maintainability, and independence of frameworks.
- **MVVM (Model-View-ViewModel)**: Utilized to decouple UI logic from business rules and state management, making the UI reactive and easier to test.
- **Repository Pattern**: Abstracts the data layer, providing a clean API for the domain layer to access data irrespective of the source (Database, API, etc.).
- **SOLID Principles**: Adherence to key object-oriented design principles (SRP, OCP, LSP, ISP, DIP) for robust and scalable code.
- **UDF (Unidirectional Data Flow)**: Ensures consistent state management within the Compose UI, where state flows down and events flow up.

## Building

To get this project up and running on your local machine:

2.  **Clone the repository**:
    ```bash
    git clone https://github.com/Nenoeldeeb/Absence-Record.git
    ```
    **Create Your own copy of `signing.properties` file in the root directory** Put the following properties in the file:
    ```properties
    release.store.file=path/to/keystore
    release.key.alias=alias_name
    release.key.password=alias_password
    release.store.password=store_password(use the same password as alias_password)
    debug.store.file=path/to/keystore
    debug.key.alias=alias_name
    debug.key.password=alias_password
    debug.store.password=store_password(use the same password as alias_password)
    ```
3.  **Build the project**:
    ```bash
    ./gradlew assembleRelease
    ```
    OR
    ```bash
    ./gradlew assembleDebug
    ```

## Contributing

Contributions are welcome! If you have suggestions for improvements or want to report a bug, please follow these steps:

1.  **Fork the Project**: Create your own copy of the repository.
2.  **Create your Feature Branch**: `git checkout -b feat/Amazing-feature` or `git checkout -b fix/Critical-bug`
3.  **Make Your changes with tests**: Write Your own code and test It.
4.  **Commit your Changes**: `git commit -m "Add some AmazingFeature"`
5.  **Push to the Branch**: `git push origin feat/Amazing-feature` or `git push origin fix/Critical-bug`
6.  **Open a Pull Request**: Submit your changes for review.

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.

> This program comes with ABSOLUTELY NO WARRANTY; for details see the LICENSE file.
> This is free software, and you are welcome to redistribute it under certain conditions.
