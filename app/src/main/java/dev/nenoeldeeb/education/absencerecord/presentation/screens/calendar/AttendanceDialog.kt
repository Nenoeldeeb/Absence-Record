@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentAttendance
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassCheckboxFilter
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toUiText
import kotlinx.datetime.LocalDate

@Composable
internal fun AttendanceDialog(
    allStudents: List<Student>,
    studentsForSelectedDate: List<StudentAttendance>,
    selectedDate: LocalDate,
    availableClasses: List<StudentClass>,
    selectedClassIds: Set<Int>,
    filterDropdownExpanded: Boolean,
    onDismiss: () -> Unit,
    onToggleAttendance: (Student, LocalDate, Boolean) -> Unit,
    onToggleClassSelection: (Int) -> Unit,
    onToggleFilterDropdown: () -> Unit,
    modifier: Modifier = Modifier,
    isFutureDate: Boolean = false,
    toggleEnabled: Boolean = true
) {
    BoxWithConstraints(modifier = modifier) {
        val isLandscape = maxWidth > maxHeight
        val listMaxHeight = (maxHeight * 0.55f).coerceAtLeast(240.dp).coerceAtMost(480.dp)
        AlertDialog(
            modifier =
                if (isLandscape) {
                    Modifier.fillMaxWidth(0.92f).widthIn(max = 800.dp)
                } else {
                    Modifier.fillMaxWidth()
                },
            properties = DialogProperties(usePlatformDefaultWidth = !isLandscape),
            onDismissRequest = onDismiss,
            title = {
                DialogTitle(
                    selectedDate = selectedDate,
                    totalStudents = allStudents.size,
                    presentCount = studentsForSelectedDate.size
                )
            },
            text = {
                Column {
                    ClassCheckboxFilter(
                        availableClasses = availableClasses,
                        selectedClassIds = selectedClassIds,
                        expanded = filterDropdownExpanded,
                        onExpandedChange = { onToggleFilterDropdown() },
                        onClassToggle = onToggleClassSelection,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    DialogStatusSlot(
                        showEmptyHint = selectedClassIds.isEmpty(),
                        isFutureDate = isFutureDate
                    )
                    DialogContent(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = listMaxHeight),
                        allStudents = allStudents,
                        studentsForSelectedDate = studentsForSelectedDate,
                        selectedDate = selectedDate,
                        toggleEnabled = toggleEnabled && !isFutureDate,
                        onToggleAttendance = onToggleAttendance,
                        isLandscape = isLandscape
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.sizeIn(minHeight = 48.dp, minWidth = 48.dp)
                ) { Text(stringResource(R.string.action_close)) }
            }
        )
    }
}

@Composable
internal fun DialogTitle(
    selectedDate: LocalDate,
    totalStudents: Int,
    presentCount: Int
) {
    val month = selectedDate.month.toUiText(fullName = false).asString()
    val dayName = selectedDate.dayOfWeek.toUiText(fullName = true).asString()
    val formattedDate =
        stringResource(
            R.string.calendar_dialog_date,
            dayName,
            month,
            selectedDate.day,
            selectedDate.year
        )
    val countText = stringResource(R.string.attendance_count, totalStudents, presentCount)
    val progress = if (totalStudents > 0) presentCount.toFloat() / totalStudents.toFloat() else 0f
    val isComplete = totalStudents > 0 && presentCount >= totalStudents
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mark_attendance_title, formattedDate),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .semantics(mergeDescendants = true) { contentDescription = countText },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = presentCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 20.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = totalStudents.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 20.dp)
            )
        }
        if (isComplete) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_check_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.attendance_day_complete),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun DialogContent(
    allStudents: List<Student>,
    studentsForSelectedDate: List<StudentAttendance>,
    selectedDate: LocalDate,
    onToggleAttendance: (Student, LocalDate, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    toggleEnabled: Boolean = true,
    isLandscape: Boolean = false
) {
    val presentIds =
        remember(studentsForSelectedDate, selectedDate) {
            studentsForSelectedDate
                .filter { it.date == selectedDate }
                .map { it.studentId }
                .toSet()
        }
    when {
        allStudents.isEmpty() -> {
            Text(
                stringResource(R.string.no_students_for_attendance),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
            )
        }

        isLandscape -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allStudents, key = { it.id }) { student ->
                    val isPresent = student.id in presentIds
                    Column {
                        StudentListItem(
                            student = student,
                            isPresent = isPresent,
                            onToggle = {
                                if (toggleEnabled) {
                                    onToggleAttendance(student, selectedDate, isPresent)
                                }
                            },
                            enabled = toggleEnabled
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        else -> {
            LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
                items(allStudents, key = { it.id }) { student ->
                    val isPresent = student.id in presentIds
                    StudentListItem(
                        student = student,
                        isPresent = isPresent,
                        onToggle = {
                            if (toggleEnabled) {
                                onToggleAttendance(student, selectedDate, isPresent)
                            }
                        },
                        enabled = toggleEnabled
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}