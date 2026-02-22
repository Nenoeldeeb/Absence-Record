package dev.nenoeldeeb.education.absencerecord.presentation.screens.report.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import dev.nenoeldeeb.education.absencerecord.domain.models.Student

@Composable
internal fun StudentList(
    students: List<Student>,
    modifier: Modifier = Modifier,
    onStudentClick: (Student) -> Unit
) {
    Column(modifier = modifier) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(students, key = { it.id }) { student ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth()
                        )
                    },
                    modifier = Modifier.clickable { onStudentClick(student) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider()
            }
        }
    }
}
