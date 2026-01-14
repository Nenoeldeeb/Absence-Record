package dev.nenoeldeeb.education.absencerecord.data.datasources.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/**
 * Test helper module that provides an in-memory Room database instance
 * for instrumentation tests. The in-memory database is destroyed after
 * each test, ensuring test isolation.
 */
object TestDatabaseModule {
    /**
     * Creates an in-memory [AppDatabase] instance for testing.
     *
     * Features:
     * - In-memory database (no persistence between tests)
     * - Allows main thread queries for simpler test setup
     * - Uses instant task executor for predictable test behavior
     *
     * @return A fresh [AppDatabase] instance for testing
     */
    fun createInMemoryDatabase(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }
}