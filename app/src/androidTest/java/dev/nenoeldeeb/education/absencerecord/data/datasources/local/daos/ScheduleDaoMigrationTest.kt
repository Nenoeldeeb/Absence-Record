package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleDaoMigrationTest {
    @get:Rule
    val migrationHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java
        )

    @Test
    fun migrate2To3_preservesStudentsAndCreatesScheduleTables() {
        val dbName = "migration-test"
        migrationHelper.createDatabase(dbName, 2).use { db ->
            db.execSQL("INSERT INTO students (name) VALUES ('Legacy Student')")
        }

        migrationHelper.runMigrationsAndValidate(
            dbName,
            3,
            true,
            AppDatabase.MIGRATION_2_3
        ).use { db ->
            db.query("SELECT COUNT(*) FROM students").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM available_lesson_hours").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM lesson_assignments").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM busy_appointments").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }
}