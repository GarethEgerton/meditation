package com.example.meditation.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MeditationGoal::class, MeditationCompletion::class, DailyMinutesGoal::class],
    version = 5,
    exportSchema = false
)
abstract class MeditationDatabase : RoomDatabase() {
    abstract fun meditationDao(): MeditationDao

    companion object {
        @Volatile
        private var INSTANCE: MeditationDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add actualDuration column to meditation_completions table
                database.execSQL(
                    "ALTER TABLE meditation_completions ADD COLUMN actualDuration INTEGER DEFAULT NULL"
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create daily_minutes_goals table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_minutes_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        targetMinutes INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): MeditationDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    MeditationDatabase::class.java,
                    "meditation_database"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
} 