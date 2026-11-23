@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.studentdetail.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun StudentDetailTopBar(
    studentFirstName: String?,
    hasStudent: Boolean,
    onBack: () -> Unit,
    onEditName: () -> Unit,
    onChangeClass: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = studentFirstName ?: stringResource(R.string.student_detail_title_placeholder),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        textDirection = TextDirection.Content
                    )
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector =
                        ImageVector.vectorResource(
                            R.drawable.outline_chevron_left_24
                        ),
                    contentDescription =
                        stringResource(R.string.student_detail_back_description)
                )
            }
        },
        actions = {
            if (hasStudent) {
                IconButton(onClick = onEditName) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(R.drawable.outline_edit_24),
                        contentDescription =
                            stringResource(R.string.student_detail_edit_name_title)
                    )
                }
                IconButton(onClick = onChangeClass) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(R.drawable.outline_swap_horiz_24),
                        contentDescription =
                            stringResource(R.string.student_detail_change_class_icon_description)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector =
                            ImageVector.vectorResource(R.drawable.outline_delete_24),
                        contentDescription =
                            stringResource(R.string.student_detail_delete_icon_description),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}