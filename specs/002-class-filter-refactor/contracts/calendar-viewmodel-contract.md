# CalendarViewModel Contract

## Public API Surface

### State Contract (`CalendarScreenState`)

```
CalendarScreenState(
    studentsForSelectedDate: List<StudentAttendance>,
    allStudents: List<Student>,
    availableClasses: List<StudentClass>,
    selectedClassIds: Set<Int>,
    filterDropdownExpanded: Boolean,
    selectedDateForDialog: LocalDate?,
    error: UiText?
)
```

**Invariants**:
1. `studentsForSelectedDate` only contains records for students in `allStudents` (present count ≤ total count)
2. `allStudents.size` ≥ `studentsForSelectedDate.size` always
3. `selectedClassIds` is a subset of `availableClasses.map { it.id }`
4. When `selectedClassIds` is empty, `allStudents` = all students including unassigned
5. When `selectedClassIds` is non-empty, unassigned students are excluded from `allStudents`

### Events Contract (`CalendarScreenEvent`)

| Event | Precondition | State Effect |
|-------|-------------|--------------|
| `ToggleClassSelection(classId)` | `classId` exists in `availableClasses` | Toggles `classId` in/out of `selectedClassIds`, re-filters `allStudents`, re-clamps `studentsForSelectedDate` |
| `ToggleFilterDropdown` | none | Toggles `filterDropdownExpanded` |
| `MarkStudentAttendance(studentId, date)` | `studentId` is in `allStudents` | Inserts attendance record, updates `studentsForSelectedDate` via flow |
| `DeleteStudentAttendance(studentId, date)` | `studentId` is in `allStudents` | Deletes attendance record, updates `studentsForSelectedDate` via flow |
| `SelectDateForDialog(date)` | none | Sets `selectedDateForDialog`, clears students list if null |

### ViewModel Guarantees

1. **Filter consistency**: `allStudents` is always derived from `rawStudents` filtered by `selectedClassIds`
2. **Clamping**: When filter changes, `studentsForSelectedDate` is filtered to only include students in `allStudents`
3. **Deduplication**: Students appearing in multiple selected classes appear once in `allStudents`
4. **Error recovery**: On use case failure, `error` is set via `UiText`; filter state unchanged
5. **Reactivity**: Attendance records stream reactively via `flatMapLatest` on `selectedDateForDialog`
