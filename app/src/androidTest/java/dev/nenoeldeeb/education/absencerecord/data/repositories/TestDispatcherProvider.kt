package dev.nenoeldeeb.education.absencerecord.data.repositories

import dev.nenoeldeeb.education.absencerecord.domain.services.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher

/**
 * Test implementation of [DispatcherProvider] that uses [StandardTestDispatcher]
 * for predictable and controllable test execution.
 *
 * This allows tests to:
 * - Control the timing of coroutine execution
 * - Avoid flakiness caused by timing issues
 * - Run synchronously within runTest blocks
 */
internal class TestDispatcherProvider(
    testDispatcher: CoroutineDispatcher
) : DispatcherProvider {
    override val io: CoroutineDispatcher = testDispatcher
    override val main: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
}