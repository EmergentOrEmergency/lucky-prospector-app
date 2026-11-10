package com.luckyprospector.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FindingEntity::class], version = 1, exportSchema = false)
abstract class LuckyProspectorDatabase : RoomDatabase() {
    abstract fun findingDao(): FindingDao

    companion object {
        @Volatile private var instance: LuckyProspectorDatabase? = null

        fun getInstance(context: Context): LuckyProspectorDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LuckyProspectorDatabase::class.java,
                    "lucky-prospector.db",
                ).build().also { instance = it }
            }
    }
}
