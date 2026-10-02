@file:OptIn(ExperimentalMaterial3Api::class)

package dev.nenoeldeeb.education.absencerecord.presentation.screens.students.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nenoeldeeb.education.absencerecord.R

@Composable
internal fun StudentsTopBar(
    title: String,
    modifier: Modifier = Modifier,
    isFilterActive: Boolean = false,
    isSortActive: Boolean = false,
    searchQuery: String = "",
    isSearchActive: Boolean = false,
    onToggleClassFilter: () -> Unit,
    onToggleSortPanel: () -> Unit,
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearch: (Boolean) -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BackHandler(enabled = isSearchActive) {
        onToggleSearch(false)
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        } else {
            keyboardController?.hide()
        }
    }

    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            if (isSearchActive) {
                IconButton(
                    onClick = { onToggleSearch(false) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.outline_chevron_left_24),
                        contentDescription = stringResource(R.string.search_close_description)
                    )
                }
            }
        },
        title = {
            if (isSearchActive) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    placeholder = { Text(stringResource(R.string.search_students_hint)) },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(
                                    imageVector =
                                        ImageVector.vectorResource(R.drawable.outline_close_24),
                                    contentDescription =
                                        stringResource(R.string.search_clear_description)
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions =
                        KeyboardActions(
                            onSearch = { keyboardController?.hide() }
                        ),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                )
            } else {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            if (!isSearchActive) {
                val isSearchApplied = searchQuery.isNotBlank()
                if (isSearchApplied) {
                    FilledTonalIconButton(
                        onClick = { onToggleSearch(true) },
                        modifier =
                            Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { selected = true }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_search_24),
                            contentDescription = stringResource(R.string.search_description)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onToggleSearch(true) },
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_search_24),
                            contentDescription = stringResource(R.string.search_description)
                        )
                    }
                }
                if (isFilterActive) {
                    FilledTonalIconButton(
                        onClick = onToggleClassFilter,
                        modifier =
                            Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { selected = true }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_filter_24),
                            contentDescription = stringResource(R.string.filter_description)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onToggleClassFilter,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_filter_24),
                            contentDescription = stringResource(R.string.filter_description)
                        )
                    }
                }
                if (isSortActive) {
                    FilledTonalIconButton(
                        onClick = onToggleSortPanel,
                        modifier =
                            Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .semantics { selected = true }
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_sort_24),
                            contentDescription = stringResource(R.string.sort_description)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onToggleSortPanel,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.outline_sort_24),
                            contentDescription = stringResource(R.string.sort_description)
                        )
                    }
                }
            }
        }
    )
}