package com.iyes.dacpressuremanager.data

import com.iyes.dacpressuremanager.domain.CommandResult
import com.iyes.dacpressuremanager.domain.DacDataState
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.ThemePalette
import com.iyes.dacpressuremanager.domain.ThemeAppearance
import com.iyes.dacpressuremanager.domain.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface DacRepository {
    val dataState: StateFlow<DacDataState>
    val themePreferences: StateFlow<ThemePreferences>
        get() = DefaultThemePreferences

    fun retryInitialization()

    suspend fun setCurrentMode(mode: PressureMode)
    suspend fun selectProfile(profileId: Long)
    suspend fun addProfile(name: String): Long
    suspend fun renameProfile(profileId: Long, name: String)
    suspend fun deleteProfile(profileId: Long): CommandResult
    suspend fun moveProfile(profileId: Long, targetIndex: Int)
    suspend fun adjustValue(profileId: Long, field: MeasurementField, deltaCenti: Int)
    suspend fun setTemperature(profileId: Long, temperatureK: Int)
    suspend fun resetMeasured(profileId: Long)
    suspend fun saveHistory(profileId: Long): CommandResult
    suspend fun restoreHistory(recordId: Long)
    suspend fun deleteHistory(recordId: Long)
    suspend fun clearHistory(profileId: Long)

    suspend fun setThemePalette(mode: PressureMode, palette: ThemePalette) = Unit
    suspend fun setThemeAppearance(appearance: ThemeAppearance) = Unit
    suspend fun resetThemePalettes() = Unit

    private companion object {
        val DefaultThemePreferences = MutableStateFlow(ThemePreferences())
    }
}
