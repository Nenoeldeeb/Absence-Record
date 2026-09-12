package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@Composable
fun HourOverflowMenu(
    hourId: Int,
    startLabel: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editDescription = stringResource(R.string.schedule_edit_hour_description, startLabel)
    val deleteDescription =
        stringResource(R.string.schedule_delete_hour_description, startLabel)
    var isOverflowOpen by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { isOverflowOpen = true },
            modifier =
                Modifier
                    .defaultMinSize(48.dp, 48.dp)
                    .testTag(HOUR_OPTIONS_TAG_PREFIX + hourId)
        ) {
            Icon(
                imageVector =
                    ImageVector.vectorResource(R.drawable.outline_more_vert_24),
                contentDescription =
                    stringResource(
                        R.string.schedule_hour_options_description,
                        startLabel
                    )
            )
        }
        DropdownMenu(
            expanded = isOverflowOpen,
            onDismissRequest = { isOverflowOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                leadingIcon = {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(R.drawable.outline_edit_24),
                        contentDescription = null
                    )
                },
                onClick = {
                    isOverflowOpen = false
                    onEditClick()
                },
                modifier =
                    Modifier
                        .testTag(HOUR_EDIT_TAG_PREFIX + hourId)
                        .semantics { contentDescription = editDescription }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_delete)) },
                leadingIcon = {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(
                                R.drawable.outline_delete_24
                            ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    isOverflowOpen = false
                    onDeleteClick()
                },
                modifier =
                    Modifier
                        .testTag(HOUR_DELETE_TAG_PREFIX + hourId)
                        .semantics { contentDescription = deleteDescription }
            )
        }
    }
}