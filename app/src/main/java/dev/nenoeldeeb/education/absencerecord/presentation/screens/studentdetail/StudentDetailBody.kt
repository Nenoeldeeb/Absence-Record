package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage

@Composable
internal fun StudentDetailBody(
    uiState: StudentDetailScreenState,
    onEvent: (StudentDetailScreenEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            uiState.student == null -> {
                EmptyStateMessage(
                    message = R.string.no_students_found,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                val student = uiState.student
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            student.name,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        uiState.assignedClassName?.let { className ->
                            Text(
                                className,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AttendanceReportSection(
                        attendanceDates = uiState.allAttendanceDates,
                        selectedMonth = uiState.selectedMonth,
                        onMonthSelected = { month ->
                            onEvent(StudentDetailScreenEvent.SelectMonth(month))
                        },
                        onShare = {
                            onEvent(StudentDetailScreenEvent.ShareAttendanceReport)
                        },
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}