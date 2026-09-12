package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.runtime.Composable
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenEvent
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.AddLessonDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.BusyAppointmentDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.ChangeClassDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.DeleteConfirmationDialog
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.dialogs.EditStudentNameDialog

@Composable
internal fun StudentDetailDialogs(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit
) {
    if (uiState.isDeleteConfirmationDialogOpen) {
        DeleteConfirmationDialog(
            onConfirm = {
                onEvent(StudentDetailScreenEvent.ConfirmDeleteStudent)
            },
            onDismiss = {
                onEvent(StudentDetailScreenEvent.ToggleDeleteConfirmationDialog)
            }
        )
    }

    val student = uiState.student
    if (uiState.isEditNameDialogOpen && student != null) {
        EditStudentNameDialog(
            currentName = student.name,
            onConfirm = { name ->
                onEvent(StudentDetailScreenEvent.UpdateStudentName(name))
            },
            onDismiss = {
                onEvent(StudentDetailScreenEvent.ToggleEditNameDialog)
            }
        )
    }

    if (uiState.isChangeClassDialogOpen) {
        ChangeClassDialog(
            availableClasses = uiState.availableClasses,
            currentClassId = uiState.student?.classId,
            onConfirm = { classId ->
                onEvent(StudentDetailScreenEvent.UpdateStudentClass(classId))
            },
            onDismiss = {
                onEvent(StudentDetailScreenEvent.ToggleChangeClassDialog)
            }
        )
    }

    if (uiState.isAddLessonDialogOpen) {
        val weekdayBusy =
            uiState.studentSchedule?.busy.orEmpty()
                .filter { it.weekday == uiState.selectedScheduleWeekday }
        AddLessonDialog(
            hours = uiState.hoursForWeekday,
            busyAppointments = weekdayBusy,
            selectedHourId = uiState.selectedLessonHourId,
            onHourSelected = { hourId ->
                onEvent(StudentDetailScreenEvent.SelectLessonHour(hourId))
            },
            onConfirm = {
                onEvent(StudentDetailScreenEvent.ConfirmAddLesson)
            },
            onDismiss = {
                onEvent(StudentDetailScreenEvent.DismissAddLessonDialog)
            }
        )
    }

    if (uiState.isBusyDialogOpen) {
        BusyAppointmentDialog(
            startMinutes = uiState.busyStartMinutes,
            durationMinutes = uiState.busyDurationMinutes,
            validationError = uiState.busyValidationError,
            isEdit = uiState.editingBusyAppointment != null,
            conflictLessons = uiState.busyConflictLessons,
            onStartSelected = { minutes ->
                onEvent(StudentDetailScreenEvent.SetBusyStart(minutes))
            },
            onDurationSelected = { duration ->
                onEvent(StudentDetailScreenEvent.SetBusyDuration(duration))
            },
            onSave = {
                onEvent(StudentDetailScreenEvent.SaveBusyAppointment)
            },
            onConfirmConflict = {
                onEvent(StudentDetailScreenEvent.ConfirmBusyConflict)
            },
            onDismiss = {
                onEvent(StudentDetailScreenEvent.DismissBusyDialog)
            }
        )
    }
}