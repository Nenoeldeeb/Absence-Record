package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.delegates

import android.net.Uri
import androidx.core.net.toUri
import dev.nenoeldeeb.education.absencerecord.domain.usecases.AttendanceUseCases
import dev.nenoeldeeb.education.absencerecord.domain.usecases.ReportUseCases
import dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.StudentDetailScreenState
import dev.nenoeldeeb.education.absencerecord.presentation.utils.UiText
import dev.nenoeldeeb.education.absencerecord.presentation.utils.toUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class StudentDetailReportDelegate(
    private val studentId: Int,
    private val attendanceUseCases: AttendanceUseCases,
    private val reportUseCases: ReportUseCases,
    private val uiState: StateFlow<StudentDetailScreenState>,
    private val scope: CoroutineScope,
    private val updateState: ((StudentDetailScreenState) -> StudentDetailScreenState) -> Unit
) {
    fun shareAttendanceReport() {
        val state = uiState.value
        val student = state.student ?: return
        val month = state.selectedMonth ?: currentMonth()
        val startOfMonth = LocalDate(month.year, month.month, 1)
        val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)

        scope.launch {
            attendanceUseCases.getAttendanceHistoryForDateRangeUseCase(
                studentId,
                startOfMonth,
                endOfMonth
            ).collectLatest { result ->
                result.onSuccess { items ->
                    reportUseCases.shareReportUseCase(
                        student.name,
                        month,
                        items.map { it.date }
                    ).onSuccess { filePath ->
                        updateState { it.copy(shareFileUri = filePath.toUri()) }
                    }.onFailure { e ->
                        updateState { it.copy(toastMessage = e.toUiText()) }
                    }
                }.onFailure { e ->
                    updateState { it.copy(toastMessage = e.toUiText()) }
                }
            }
        }
    }

    fun handleShareFileResult(
        uri: Uri,
        error: UiText?
    ) {
        if (error != null) {
            updateState { it.copy(toastMessage = error) }
        }
        updateState { it.copy(shareFileUri = null) }
    }

    private fun currentMonth(): LocalDate {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return LocalDate(now.year, now.month, 1)
    }
}