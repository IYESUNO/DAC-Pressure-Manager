package com.iyes.dacpressuremanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AppStateEntity::class,
        ProfileEntity::class,
        HistoryRecordEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DacDatabase : RoomDatabase() {
    abstract fun dacDao(): DacDao
}

