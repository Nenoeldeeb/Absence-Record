package dev.nenoeldeeb.education.absencerecord.data.services

import dev.nenoeldeeb.education.absencerecord.domain.services.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Default implementation of DispatcherProvider using standard Android dispatchers.
 * Production implementation that provides real dispatchers.
 */
class DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val default: CoroutineDispatcher = Dispatchers.Default
}