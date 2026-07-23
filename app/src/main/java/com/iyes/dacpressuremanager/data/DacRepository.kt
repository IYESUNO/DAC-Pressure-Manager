package com.iyes.dacpressuremanager.data

import com.iyes.dacpressuremanager.domain.CommandResult
import com.iyes.dacpressuremanager.domain.DacDataState
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureMode
import kotlinx.coroutines.flow.StateFlow

interface DacRepository {
    val dataState: StateFlow<DacDataState>

    fun retryInitialization()

    suspend fun setCurrentMode(mode: PressureMode)
    suspend fun selectProfile(profileId: Long)
    suspend fun addProfile(name: String): Long
    suspend fun renameProfile(profileId: Long, name: String)
    suspend fun deleteProfile(profileId: Long): CommandResult
    suspend fun moveProfile(profileId: Long, targetIndex: Int)
    suspend fun adjustValue(profileId: Long, field: MeasurementField, deltaCenti: Int)
    suspend fun resetMeasured(profileId: Long)
    suspend fun saveHistory(profileId: Long): CommandResult
    suspend fun restoreHistory(recordId: Long)
    suspend fun deleteHistory(recordId: Long)
    suspend fun clearHistory(profileId: Long)
}

