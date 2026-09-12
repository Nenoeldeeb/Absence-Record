package dev.nenoeldeeb.education.absencerecord.app.navigation

sealed interface AppDestination {
    data object MainPager : AppDestination

    data class StudentDetail(val studentId: Int) : AppDestination
}