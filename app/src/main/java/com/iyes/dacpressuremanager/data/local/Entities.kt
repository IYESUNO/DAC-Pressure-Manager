package com.iyes.dacpressuremanager.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val currentMode: String,
    val diamondActiveProfileId: Long?,
    val rubyActiveProfileId: Long?,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(
    tableName = "profiles",
    indices = [
        Index(value = ["mode"]),
        Index(value = ["mode", "sortOrder"]),
    ],
)
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val name: String,
    val referenceCenti: Int,
    val measuredCenti: Int,
    val sortOrder: Int,
)

@Entity(
    tableName = "history_records",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["profileId"]),
        Index(value = ["profileId", "createdAtEpochMillis"]),
    ],
)
data class HistoryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val createdAtEpochMillis: Long,
    val referenceCenti: Int,
    val measuredCenti: Int,
    val pressureCenti: Int,
)

