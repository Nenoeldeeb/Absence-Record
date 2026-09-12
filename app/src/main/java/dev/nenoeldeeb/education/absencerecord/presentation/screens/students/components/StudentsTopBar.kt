@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun StudentsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onAddStudent: () -> Unit,
    onToggleClassFilter: () -> Unit,
    onToggleSortPanel: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title) },
        actions = {
            IconButton(onClick = onToggleClassFilter) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_filter_24),
                    contentDescription = stringResource(R.string.filter_description)
                )
            }
            IconButton(onClick = onToggleSortPanel) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_sort_24),
                    contentDescription = stringResource(R.string.sort_description)
                )
            }
            IconButton(onClick = onAddStudent) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.outline_add_24),
                    contentDescription = stringResource(R.string.new_student_label)
                )
            }
        }
    )
}