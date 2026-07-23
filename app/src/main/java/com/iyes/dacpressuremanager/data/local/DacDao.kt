package com.iyes.dacpressuremanager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DacDao {
    @Query("SELECT * FROM app_state WHERE id = 1")
    fun observeAppState(): Flow<AppStateEntity?>

    @Query("SELECT * FROM app_state WHERE id = 1")
    suspend fun getAppState(): AppStateEntity?

    @Upsert
    suspend fun upsertAppState(state: AppStateEntity)

    @Query("SELECT * FROM profiles ORDER BY mode ASC, sortOrder ASC, id ASC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE mode = :mode ORDER BY sortOrder ASC, id ASC")
    suspend fun getProfiles(mode: String): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :profileId")
    suspend fun getProfile(profileId: Long): ProfileEntity?

    @Insert
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET name = :name WHERE id = :profileId")
    suspend fun updateProfileName(profileId: Long, name: String)

    @Query(
        """
        UPDATE profiles
        SET referenceCenti = :referenceCenti, measuredCenti = :measuredCenti
        WHERE id = :profileId
        """,
    )
    suspend fun updateProfileValues(
        profileId: Long,
        referenceCenti: Int,
        measuredCenti: Int,
    )

    @Query("UPDATE profiles SET sortOrder = :sortOrder WHERE id = :profileId")
    suspend fun updateProfileSortOrder(profileId: Long, sortOrder: Int)

    @Query(
        """
        SELECT * FROM history_records
        ORDER BY createdAtEpochMillis DESC, id DESC
        """,
    )
    fun observeHistory(): Flow<List<HistoryRecordEntity>>

    @Query("SELECT * FROM history_records WHERE id = :recordId")
    suspend fun getHistoryRecord(recordId: Long): HistoryRecordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHistoryRecord(record: HistoryRecordEntity): Long

    @Query("DELETE FROM history_records WHERE id = :recordId")
    suspend fun deleteHistoryRecord(recordId: Long)

    @Query("DELETE FROM history_records WHERE profileId = :profileId")
    suspend fun clearHistory(profileId: Long)

    @Query(
        """
        DELETE FROM history_records
        WHERE profileId = :profileId
          AND id NOT IN (
              SELECT id FROM history_records
              WHERE profileId = :profileId
              ORDER BY createdAtEpochMillis DESC, id DESC
              LIMIT :keepCount
          )
        """,
    )
    suspend fun trimHistory(profileId: Long, keepCount: Int)
}

