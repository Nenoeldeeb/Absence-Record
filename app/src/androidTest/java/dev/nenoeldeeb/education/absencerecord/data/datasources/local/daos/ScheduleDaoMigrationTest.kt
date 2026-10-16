package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun migrate3To4_convertsStoredCapacityToDurationAndKeepsAssignments() {
        val dbName = "migration-test-3-to-4"
        migrationHelper.createDatabase(dbName, 3).use { db ->
            db.execSQL("INSERT INTO students (id, name) VALUES (1, 'Legacy Student')")
            db.execSQL(
                "INSERT INTO available_lesson_hours (id, weekday, startMinutes, maxStudents) " +
                    "VALUES (7, 6, 600, 6)"
            )
            db.execSQL(
                "INSERT INTO lesson_assignments (id, availableHourId, studentId, weekday) " +
                    "VALUES (1, 7, 1, 6)"
            )
        }

        migrationHelper.runMigrationsAndValidate(
            dbName,
            4,
            true,
            AppDatabase.MIGRATION_3_4
        ).use { db ->
            // capacity 6 becomes a 60-minute block (FR-040)
            db.query("SELECT durationMinutes FROM available_lesson_hours WHERE id = 7").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(60, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM lesson_assignments").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT name FROM students WHERE id = 1").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Legacy Student", cursor.getString(0))
            }
        }
    }

    @Test
    fun migrate3To4_clampsLegacyCapacityOutsideTheSelectableRange() {
        val dbName = "migration-test-3-to-4-clamp"
        migrationHelper.createDatabase(dbName, 3).use { db ->
            db.execSQL("INSERT INTO students (id, name) VALUES (1, 'Legacy Student')")
            // 99 is the old ceiling; 10x it is 990 minutes, which must clamp to 120
            db.execSQL(
                "INSERT INTO available_lesson_hours (id, weekday, startMinutes, maxStudents) " +
                    "VALUES (8, 1, 0, 99)"
            )
            db.execSQL(
                "INSERT INTO available_lesson_hours (id, weekday, startMinutes, maxStudents) " +
                    "VALUES (9, 2, 0, 0)"
            )
        }

        migrationHelper.runMigrationsAndValidate(
            dbName,
            4,
            true,
            AppDatabase.MIGRATION_3_4
        ).use { db ->
            db.query("SELECT durationMinutes FROM available_lesson_hours WHERE id = 8").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(120, cursor.getInt(0))
            }
            db.query("SELECT durationMinutes FROM available_lesson_hours WHERE id = 9").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(10, cursor.getInt(0))
            }
        }
    }
}