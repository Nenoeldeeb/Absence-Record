package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.ParsedStudentImportData

@Composable
internal fun ImportSelectionDialog(
    parsedStudentsFromFile: List<ParsedStudentImportData>?,
    importSelectionMap: Map<Int, Boolean>,
    modifier: Modifier = Modifier,
    onToggleSelection: (Int) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onImport: () -> Unit,
    onDismiss: () -> Unit
) {
    val items = parsedStudentsFromFile.orEmpty()
    val allSelected = items.isNotEmpty() && items.all { importSelectionMap[it.id] == true }
    val noneSelected = items.isEmpty() || items.none { importSelectionMap[it.id] == true }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.select_students_import_title))
        },
        text = {
            Column {
                Text(
                    text = pluralStringResource(
                        R.plurals.import_preview_message,
                        items.size,
                        items.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (items.isNotEmpty()) {
                    SelectAllRow(
                        allSelected = allSelected,
                        onSelectAll = onSelectAll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                    HorizontalDivider()
                }

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    if (items.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.no_items_available),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(items = items, key = { it.id }) { item ->
                            StudentImportItemRow(
                                item = item,
                                isSelected = importSelectionMap[item.id] == true,
                                onToggle = { onToggleSelection(item.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = !noneSelected
            ) {
                Text(stringResource(R.string.import_selected))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SelectAllRow(
    allSelected: Boolean,
    onSelectAll: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable { onSelectAll(!allSelected) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = allSelected,
            onCheckedChange = onSelectAll
        )
        Text(
            text = stringResource(R.string.toggle_students_selection),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun StudentImportItemRow(
    item: ParsedStudentImportData,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.originalData.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                R.string.imported_dates_count, item.originalData.dates.size
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
