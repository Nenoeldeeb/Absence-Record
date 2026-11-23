package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.delegates

import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ObserveStudentScheduleUseCase
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Observes the student's live schedule view into state. Mutates state only
 * through `updateState` and launches only through `scope`.
 */
class StudentScheduleDelegate(
    private val observeStudentScheduleUseCase: ObserveStudentScheduleUseCase,
    private val scope: CoroutineScope,
    private val updateState: ((StudentDetailScreenState) -> StudentDetailScreenState) -> Unit
) {
    fun observe(studentId: Int) {
        scope.launch {
            observeStudentScheduleUseCase(studentId).collectLatest { result ->
                result
                    .onSuccess { schedule ->
                        updateState { it.copy(studentSchedule = schedule) }
                    }
                    .onFailure { e ->
                        updateState { it.copy(error = e.toUiText()) }
                    }
            }
        }
    }
}