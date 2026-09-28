package com.whispervault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class TaskStatusConverters {
    @TypeConverter
    fun toStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromStatus(status: TaskStatus): String = status.name
}

@Database(
    entities = [TranscriptionTaskEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(TaskStatusConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transcriptionTaskDao(): TranscriptionTaskDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whispervault.db",
                ).build().also { instance = it }
            }
        }
    }
}
