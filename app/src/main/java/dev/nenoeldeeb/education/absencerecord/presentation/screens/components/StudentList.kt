package dev.nenoeldeeb.education.absencerecord.presentation.screens.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import dev.nenoeldeeb.education.absencerecord.domain.models.Student

@Composable
internal fun StudentList(
    allStudents: List<Student>,
    modifier: Modifier = Modifier,
    selectedStudentIds: Set<Int> = emptySet(),
    onStudentClick: (Student) -> Unit,
    onStudentLongClick: (Int) -> Unit
) {
    LazyColumn(modifier = modifier) {
        items(allStudents, key = { it.id }) { student ->
            val isSelected = selectedStudentIds.contains(student.id)
            ListItem(
                modifier = Modifier
                    .combinedClickable(
                        onClick = { onStudentClick(student) },
                        onLongClick = { onStudentLongClick(student.id) }
                    ),
                headlineContent = {
                    Text(
                        student.name,
                        style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                    )
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
