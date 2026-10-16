package dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The 3 → 4 migration rebuilds `available_lesson_hours`.
 *
 * `ALTER TABLE … RENAME COLUMN` would have avoided the rebuild, but it needs
 * SQLite 3.25 (API 30) and this app supports API 26, so the table is copied
 * instead. That introduces a hazard the single-statement version did not have:
 * renaming a table on SQLite 3.25+ rewrites the foreign keys that point at it,
 * which would leave `lesson_assignments` referencing the discarded copy — and
 * dropping it would then CASCADE-delete every assignment.
 *
 * These run on whatever device Gradle picks, so the migration is proven on both
 * modern SQLite and the API 26 SQLite that cannot rename a column.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleDaoMigrationRebuildTest {
    private val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory()
        )

    /** A v3 database holding one block with several students assigned to it. */
    private fun seed(dbName: String) {
        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL("INSERT INTO students (id, name) VALUES (1, 'A'), (2, 'B'), (3, 'C')")
            db.execSQL(
                "INSERT INTO available_lesson_hours (id, weekday, startMinutes, maxStudents) " +
                    "VALUES (7, 6, 540, 6)"
            )
            db.execSQL(
                "INSERT INTO lesson_assignments (id, availableHourId, studentId, weekday) " +
                    "VALUES (1, 7, 1, 6), (2, 7, 2, 6), (3, 7, 3, 6)"
            )
        }
    }

    @Test
    fun everyAssignmentSurvivesTheRebuild() {
        val dbName = "rebuild-keeps-assignments"
        seed(dbName)

        helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4).use { db ->
            db.query("SELECT COUNT(*) FROM lesson_assignments").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(
                    "a cascade from the discarded table would leave none of these",
                    3,
                    cursor.getInt(0)
                )
            }
        }
    }

    @Test
    fun assignmentsStillPointAtTheRebuiltBlock() {
        val dbName = "rebuild-keeps-foreign-key"
        seed(dbName)

        helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4).use { db ->
            // A dangling foreign key would not be caught by a row count alone.
            db.query("PRAGMA foreign_key_check").use { cursor ->
                assertTrue("foreign key violations after the rebuild", cursor.count == 0)
            }
            db.query(
                "SELECT availableHourId FROM lesson_assignments ORDER BY id"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    assertEquals(7, cursor.getInt(0))
                }
            }
        }
    }

    @Test
    fun theLegacyTableIsNotLeftBehind() {
        val dbName = "rebuild-drops-legacy-table"
        seed(dbName)

        helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4).use { db ->
            db.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' " +
                    "AND name = 'available_lesson_hours_old'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("the temporary table must be dropped", 0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun theWeekdayIndexIsRecreated() {
        val dbName = "rebuild-recreates-index"
        seed(dbName)

        helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4).use { db ->
            db.query(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' " +
                    "AND name = 'index_available_lesson_hours_weekday'"
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("queries by weekday would silently scan", 1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun blockIdentifiersAndStartTimesArePreserved() {
        val dbName = "rebuild-preserves-identity"
        seed(dbName)

        helper.runMigrationsAndValidate(dbName, 4, true, AppDatabase.MIGRATION_3_4).use { db ->
            db.query("SELECT id, weekday, startMinutes FROM available_lesson_hours").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(7, cursor.getInt(0))
                assertEquals(6, cursor.getInt(1))
                assertEquals(540, cursor.getInt(2))
            }
        }
    }
}