package dev.nenoeldeeb.education.absencerecord.presentation.screens.schedule.components

/**
 * Test tags for the schedule screen's day view.
 *
 * Kept apart from the composables so that adding a tag does not push
 * `HourRow.kt` — which sits exactly at the 300-line cap — over it, and so a
 * test can name what it is reaching for without importing a composable file.
 */
const val HOUR_ROW_TAG_PREFIX = "hour_row_"

/** On the scrollable list of a day's blocks, so off-screen ones can be reached. */
const val SCHEDULE_HOURS_LIST_TAG = "schedule_hours_list"

/** On a student's own ten-minute slot inside an expanded block. */
const val HOUR_ASSIGNED_SLOT_TAG_PREFIX = "assigned_slot_"

/** On a student's row within an expanded block. */
const val HOUR_ASSIGNED_STUDENT_TAG_PREFIX = "assigned_student_"

/** On the "Add hour" button at the bottom of the day view. */
const val HOURS_ADD_TAG = "hours_add"
const val HOURS_EMPTY_ADD_TAG = "hours_empty_add"
const val HOURS_ERROR_RETRY_TAG = "hours_error_retry"

const val HOUR_EXPAND_TAG_PREFIX = "hour_expand_"
const val HOUR_OPTIONS_TAG_PREFIX = "hour_options_"
const val HOUR_EDIT_TAG_PREFIX = "hour_edit_"
const val HOUR_DELETE_TAG_PREFIX = "hour_delete_"