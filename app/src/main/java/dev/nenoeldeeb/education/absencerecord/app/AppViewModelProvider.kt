package dev.nenoeldeeb.education.absencerecord.app

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar.CalendarViewModel
import dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.ScheduleViewModel
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailViewModel
import dev.nenoeldeeb.education.absencerecord.presentation.screens.students.StudentsViewModel

object AppViewModelProvider {
    val factory =
        viewModelFactory {
            initializer {
                CalendarViewModel(
                    attendanceUseCases = app().appContainer.attendanceUseCases,
                    studentManagementUseCases = app().appContainer.studentManagementUseCases,
                    classManagementUseCases = app().appContainer.classManagementUseCases,
                    classFilterRepository = app().appContainer.classFilterRepository
                )
            }
            initializer {
                StudentsViewModel(
                    studentManagementUseCases = app().appContainer.studentManagementUseCases,
                    classManagementUseCases = app().appContainer.classManagementUseCases,
                    classFilterRepository = app().appContainer.classFilterRepository,
                    attendanceUseCases = app().appContainer.attendanceUseCases
                )
            }
            initializer {
                ScheduleViewModel(
                    scheduleUseCases = app().appContainer.scheduleUseCases,
                    studentManagementUseCases = app().appContainer.studentManagementUseCases
                )
            }
        }

    fun studentDetailFactory(studentId: Int): ViewModelProvider.Factory =
        viewModelFactory {
            initializer {
                StudentDetailViewModel(
                    studentId = studentId,
                    studentManagementUseCases = app().appContainer.studentManagementUseCases,
                    attendanceUseCases = app().appContainer.attendanceUseCases,
                    reportUseCases = app().appContainer.reportUseCases,
                    classManagementUseCases = app().appContainer.classManagementUseCases,
                    scheduleUseCases = app().appContainer.scheduleUseCases
                )
            }
        }

    fun CreationExtras.app(): Application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
}