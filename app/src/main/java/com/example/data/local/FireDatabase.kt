package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FireHotspotEntity::class], version = 1, exportSchema = false)
abstract class FireDatabase : RoomDatabase() {
    abstract fun hotspotDao(): HotspotDao

    companion object {
        @Volatile
        private var INSTANCE: FireDatabase? = null

        fun getInstance(context: Context): FireDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FireDatabase::class.java,
                    "kalimantan_fire_monitor.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
