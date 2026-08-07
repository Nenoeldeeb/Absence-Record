package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.Student

@Composable
internal fun StudentList(
    allStudents: List<Student>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    selectedStudentIds: Set<Int> = emptySet(),
    isMultiSelectionMode: Boolean = false,
    onStudentClick: (Student) -> Unit,
    onStudentLongClick: (Int) -> Unit
) {
    LazyColumn(modifier = modifier, state = state) {
        items(allStudents, key = { it.id }) { student ->
            val isSelected = selectedStudentIds.contains(student.id)
            val selectionText =
                if (isMultiSelectionMode) {
                    if (isSelected) {
                        stringResource(R.string.student_selected, student.name)
                    } else {
                        stringResource(R.string.student_not_selected, student.name)
                    }
                } else {
                    student.name
                }

            ListItem(
                modifier =
                    Modifier
                        .combinedClickable(
                            onClick = { onStudentClick(student) },
                            onLongClick = { onStudentLongClick(student.id) }
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = selectionText
                        },
                headlineContent = {
                    Text(
                        student.name,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().wrapContentWidth()
                    )
                },
                leadingContent =
                    if (isMultiSelectionMode && isSelected) {
                        {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.outline_check_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        null
                    },
                colors =
                    ListItemDefaults.colors(
                        containerColor =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                ListItemDefaults.colors().containerColor
                            }
                    )
            )
            HorizontalDivider()
        }
    }
}