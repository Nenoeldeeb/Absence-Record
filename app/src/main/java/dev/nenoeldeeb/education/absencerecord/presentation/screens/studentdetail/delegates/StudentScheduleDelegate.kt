package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.delegates

import dev.nenoeldeeb.education.absencerecord.domain.models.StudentScheduleView
import dev.nenoeldeeb.education.absencerecord.domain.usecases.schedule.ObserveStudentScheduleUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class StudentScheduleDelegate(
    private val observeStudentScheduleUseCase: ObserveStudentScheduleUseCase,
    private val scope: CoroutineScope,
    private val onScheduleUpdated: (StudentScheduleView) -> Unit,
    private val onFailure: (Throwable) -> Unit
) {
    fun observe(studentId: Int) {
        scope.launch {
            observeStudentScheduleUseCase(studentId).collectLatest { result ->
                result.onSuccess(onScheduleUpdated).onFailure(onFailure)
            }
        }
    }
}