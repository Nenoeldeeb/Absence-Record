package dev.nenoeldeeb.education.absencerecord.app

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarViewModel
import dev.nenoeldeeb.education.absencerecord.presentation.screens.report.ReportViewModel
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsViewModel

object AppViewModelProvider {
    val factory = viewModelFactory {
        initializer {
            CalendarViewModel(
                attendanceUseCases = app().appContainer.attendanceUseCases,
                studentManagementUseCases = app().appContainer.studentManagementUseCases,
                classManagementUseCases = app().appContainer.classManagementUseCases
            )
        }
        initializer {
            StudentsViewModel(
                studentManagementUseCases = app().appContainer.studentManagementUseCases,
                classManagementUseCases = app().appContainer.classManagementUseCases
            )
        }
        initializer {
            ReportViewModel(
                studentManagementUseCases = app().appContainer.studentManagementUseCases,
                attendanceUseCases = app().appContainer.attendanceUseCases,
                reportUseCases = app().appContainer.reportUseCases,
                classManagementUseCases = app().appContainer.classManagementUseCases
            )
        }
    }

    fun CreationExtras.app(): Application =
        (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
}
