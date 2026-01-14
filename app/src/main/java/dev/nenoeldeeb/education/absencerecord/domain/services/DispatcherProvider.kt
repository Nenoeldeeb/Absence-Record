package dev.nenoeldeeb.education.absencerecord.domain.services

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Interface for providing coroutine dispatchers.
 * Follows Dependency Inversion Principle by abstracting dispatcher access.
 * Enables testability by allowing injection of test dispatchers.
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
}