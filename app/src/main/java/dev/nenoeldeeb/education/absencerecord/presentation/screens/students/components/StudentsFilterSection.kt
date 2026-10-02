package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R
import dev.nenoeldeeb.education.absencerecord.domain.models.StudentClass
import dev.nenoeldeeb.education.absencerecord.presentation.screens.components.ClassCheckboxFilter

@Composable
internal fun StudentsFilterSection(
    availableClasses: List<StudentClass>,
    selectedClassIds: Set<Int>,
    classDropdownExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClassToggle: (Int) -> Unit,
    onManageClasses: () -> Unit,
    modifier: Modifier = Modifier,
    onClearFilters: (() -> Unit)? = null
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ClassCheckboxFilter(
                availableClasses = availableClasses,
                selectedClassIds = selectedClassIds,
                expanded = classDropdownExpanded,
                onExpandedChange = onExpandedChange,
                onClassToggle = onClassToggle,
                modifier = Modifier.weight(1f),
                onClearFilters = onClearFilters
            )
            IconButton(
                onClick = onManageClasses,
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_edit_24),
                    contentDescription = stringResource(R.string.manage_classes_title),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (availableClasses.isNotEmpty() && selectedClassIds.isEmpty()) {
            Text(
                text = stringResource(R.string.calendar_filter_empty_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}