package com.iyes.dacpressuremanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AppStateEntity::class,
        ProfileEntity::class,
        HistoryRecordEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class DacDatabase : RoomDatabase() {
    abstract fun dacDao(): DacDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE profiles " +
                        "ADD COLUMN temperatureK INTEGER NOT NULL DEFAULT 298",
                )
                db.execSQL(
                    "ALTER TABLE history_records " +
                        "ADD COLUMN temperatureK INTEGER NOT NULL DEFAULT 298",
                )
            }
        }
    }
}
