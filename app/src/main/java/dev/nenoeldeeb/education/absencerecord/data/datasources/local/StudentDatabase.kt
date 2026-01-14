package dev.nenoeldeeb.education.absencerecord.data.datasources.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.AttendanceDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.daos.StudentDao
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentAttendanceEntity
import dev.nenoeldeeb.education.absencerecord.data.datasources.local.entities.StudentEntity
import kotlinx.datetime.LocalDate

@Database(
    entities = [StudentEntity::class, StudentAttendanceEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao

    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "students.db"
                ).build().also { instance = it }
            }
    }
}

object DateConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? = dateString?.let { LocalDate.parse(it) }
}