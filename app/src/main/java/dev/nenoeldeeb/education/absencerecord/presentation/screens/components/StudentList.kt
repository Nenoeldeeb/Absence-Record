package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.utils.DateFormatter.toMonthYearUiText
import kotlinx.datetime.LocalDate

@Composable
internal fun StudentList(
    allStudents: List<Student>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    selectedStudentIds: Set<Int> = emptySet(),
    isMultiSelectionMode: Boolean = false,
    availableClasses: List<StudentClass> = emptyList(),
    selectedMonth: LocalDate? = null,
    onStudentClick: (Student) -> Unit,
    onStudentLongClick: (Int) -> Unit
) {
    val classNameById = remember(availableClasses) { availableClasses.associate { it.id to it.name } }
    val unassignedLabel = stringResource(R.string.class_unassigned_label)
    val allMonthsLabel = stringResource(R.string.all_months)
    val context = LocalContext.current
    val monthLabel =
        remember(selectedMonth, allMonthsLabel, context) {
            selectedMonth?.toMonthYearUiText(fullName = true)?.asString(context)
                ?: allMonthsLabel
        }
    val hasMonthFilter = selectedMonth != null
    // Hoisted once per list composition instead of once per row per
    // recomposition. Only stable value types cross the row boundary here
    // (Color, ImageVector lookups stay in composition, never inside
    // remember{}): each row resolves its own ListItemColors locally, so
    // toggling an unrelated panel no longer hands fresh color objects to
    // every visible row and defeats skipping.
    val selectedContainer = MaterialTheme.colorScheme.primaryContainer
    val personIcon = ImageVector.vectorResource(R.drawable.outline_person_24)
    val checkIcon = ImageVector.vectorResource(R.drawable.outline_check_24)
    LazyColumn(modifier = modifier, state = state) {
        items(
            allStudents,
            key = { it.id },
            contentType = { "student_row" }
        ) { student ->
            val isSelected = selectedStudentIds.contains(student.id)
            val className = classNameById[student.classId] ?: unassignedLabel
            StudentRow(
                student = student,
                isSelected = isSelected,
                isMultiSelectionMode = isMultiSelectionMode,
                className = className,
                monthLabel = monthLabel,
                hasMonthFilter = hasMonthFilter,
                personIcon = personIcon,
                checkIcon = checkIcon,
                selectedContainer = selectedContainer,
                onStudentClick = onStudentClick,
                onStudentLongClick = onStudentLongClick
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun StudentRow(
    student: Student,
    isSelected: Boolean,
    isMultiSelectionMode: Boolean,
    className: String,
    monthLabel: String,
    hasMonthFilter: Boolean,
    personIcon: ImageVector,
    checkIcon: ImageVector,
    selectedContainer: Color,
    onStudentClick: (Student) -> Unit,
    onStudentLongClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val enterSelectionModeLabel = stringResource(R.string.student_enter_selection_mode)
    val supportingText =
        if (hasMonthFilter) {
            "$className · $monthLabel"
        } else {
            className
        }
    val selectionText =
        if (isMultiSelectionMode) {
            if (isSelected) {
                stringResource(R.string.student_selected, student.name)
            } else {
                stringResource(R.string.student_not_selected, student.name)
            }
        } else {
            "${
                student.name
            }, $className"
        }
    // Class identity rides on the text label alone. The avatar stays neutral:
    // cycling primary/secondary/tertiary by class id repurposed fixed-meaning
    // chalk roles as decoration and left TalkBack with hue-only information.
    val chalkTint = MaterialTheme.colorScheme.onSurfaceVariant
    // TextStyle copies are constant per theme, so cache them instead of
    // allocating two new styles for every visible row on every state
    // change (selection toggles, panel expand/collapse). Keyed by the
    // theme so a light/dark switch still rebuilds them.
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme
    val bodyLargeContent =
        remember(typography) {
            typography.bodyLarge.copy(textDirection = TextDirection.Content)
        }
    val bodySmallContent =
        remember(typography) {
            typography.bodySmall.copy(textDirection = TextDirection.Content)
        }
    val rowColors =
        if (isSelected) {
            ListItemDefaults.colors(containerColor = selectedContainer)
        } else {
            ListItemDefaults.colors()
        }

    ListItem(
        modifier =
            modifier
                .sizeIn(minHeight = 48.dp)
                .combinedClickable(
                    onClick = { onStudentClick(student) },
                    onLongClick = { onStudentLongClick(student.id) },
                    onLongClickLabel = enterSelectionModeLabel
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = selectionText
                },
        headlineContent = {
            Text(
                student.name,
                style = bodyLargeContent,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = personIcon,
                    contentDescription = null,
                    tint = chalkTint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = supportingText,
                    style = bodySmallContent,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
        },
        trailingContent = null,
        leadingContent =
            if (isMultiSelectionMode && isSelected) {
                {
                    Icon(
                        imageVector = checkIcon,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                }
            } else {
                null
            },
        colors = rowColors
    )
}