package com.example.meditation.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MeditationGoal::class, MeditationCompletion::class],
    version = 2,
    exportSchema = false
)
abstract class MeditationDatabase : RoomDatabase() {
    abstract fun meditationDao(): MeditationDao

    companion object {
        @Volatile
        private var INSTANCE: MeditationDatabase? = null

        fun getDatabase(context: Context): MeditationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MeditationDatabase::class.java,
                    "meditation_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 