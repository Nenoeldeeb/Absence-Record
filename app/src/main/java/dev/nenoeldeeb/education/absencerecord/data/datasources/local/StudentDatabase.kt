package dev.nenoeldeeb.education.absencerecord.data.datasources.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.AttendanceDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.ScheduleDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentClassDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.AvailableLessonHourEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.BusyAppointmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.LessonAssignmentEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentAttendanceEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentClassEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import kotlinx.datetime.LocalDate

@Database(
    entities = [
        StudentEntity::class,
        StudentAttendanceEntity::class,
        StudentClassEntity::class,
        AvailableLessonHourEntity::class,
        LessonAssignmentEntity::class,
        BusyAppointmentEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao

    abstract fun attendanceDao(): AttendanceDao

    abstract fun studentClassDao(): StudentClassDao

    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `classes` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL("ALTER TABLE `students` ADD COLUMN `classId` INTEGER")
                }
            }

        /**
         * Duration-based scheduling replaces the stored student count with a lesson
         * length (FR-040), converting each legacy capacity to a length and clamping
         * it into the selectable 10–120 range (FR-041).
         *
         * The table IS rebuilt, because `ALTER TABLE … RENAME COLUMN` needs SQLite 3.25
         * (API 30) and this app supports API 26. The rebuild is written so it is safe
         * on every supported version:
         *
         *  - `legacy_alter_table` makes SQLite 3.25+ behave like older versions when
         *    renaming, so `lesson_assignments`' foreign key keeps pointing at
         *    `available_lesson_hours` rather than being rewritten to the old name.
         *    Older SQLite ignores the pragma, having never rewritten those references.
         *  - the legacy table is therefore referenced by nothing, so dropping it
         *    cannot cascade-delete the assignments.
         *
         * Every row is copied across; the only values changed are the clamped ones.
         */
        internal val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("PRAGMA legacy_alter_table = ON")
                    db.execSQL("ALTER TABLE `available_lesson_hours` RENAME TO `available_lesson_hours_old`")
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `available_lesson_hours` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `weekday` INTEGER NOT NULL,
                            `startMinutes` INTEGER NOT NULL,
                            `durationMinutes` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        INSERT INTO `available_lesson_hours`
                            (`id`, `weekday`, `startMinutes`, `durationMinutes`)
                        SELECT `id`, `weekday`, `startMinutes`, MIN(120, MAX(10, `maxStudents` * 10))
                        FROM `available_lesson_hours_old`
                        """.trimIndent()
                    )
                    db.execSQL("DROP TABLE `available_lesson_hours_old`")
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_available_lesson_hours_weekday` " +
                            "ON `available_lesson_hours` (`weekday`)"
                    )
                }
            }

        internal val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `available_lesson_hours` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `weekday` INTEGER NOT NULL,
                            `startMinutes` INTEGER NOT NULL,
                            `maxStudents` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `lesson_assignments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `availableHourId` INTEGER NOT NULL,
                            `studentId` INTEGER NOT NULL,
                            `weekday` INTEGER NOT NULL,
                            FOREIGN KEY(`availableHourId`) REFERENCES `available_lesson_hours`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE,
                            FOREIGN KEY(`studentId`) REFERENCES `students`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `busy_appointments` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `studentId` INTEGER NOT NULL,
                            `weekday` INTEGER NOT NULL,
                            `startMinutes` INTEGER NOT NULL,
                            `durationMinutes` INTEGER NOT NULL,
                            FOREIGN KEY(`studentId`) REFERENCES `students`(`id`)
                                ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_available_lesson_hours_weekday` " +
                            "ON `available_lesson_hours` (`weekday`)"
                    )
                    db.execSQL(
                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_lesson_assignments_studentId_weekday` " +
                            "ON `lesson_assignments` (`studentId`, `weekday`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_lesson_assignments_availableHourId` " +
                            "ON `lesson_assignments` (`availableHourId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_lesson_assignments_studentId` " +
                            "ON `lesson_assignments` (`studentId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_busy_appointments_studentId_weekday` " +
                            "ON `busy_appointments` (`studentId`, `weekday`)"
                    )
                }
            }

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "students.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}

object DateConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }
}