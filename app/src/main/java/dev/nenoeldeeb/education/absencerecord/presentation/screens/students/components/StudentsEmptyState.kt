package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.EmptyStateMessage

@Composable
internal fun StudentsEmptyState(
    totalStudentsCount: Int,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    onAddFirstStudent: () -> Unit = {},
    onImportStudents: () -> Unit = {},
    onClearSearch: () -> Unit = {}
) {
    if (totalStudentsCount > 0) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (searchQuery.isNotBlank()) {
                EmptyStateMessage(
                    message = R.string.search_no_results,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    searchQuery.trim()
                )
                OutlinedButton(
                    onClick = onClearSearch,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text(stringResource(R.string.clear_search))
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                EmptyStateMessage(
                    message = R.string.no_students_found,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyStateMessage(
                message = R.string.no_students_message,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Button(
                onClick = onAddFirstStudent,
                modifier = Modifier.sizeIn(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.add_first_student))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onImportStudents,
                modifier = Modifier.sizeIn(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.import_students_action))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.empty_state_class_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}