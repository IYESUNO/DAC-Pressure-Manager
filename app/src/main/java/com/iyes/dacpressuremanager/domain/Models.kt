package com.iyes.dacpressuremanager.domain

data class Profile(
    val id: Long,
    val mode: PressureMode,
    val name: String,
    val referenceCenti: Int,
    val measuredCenti: Int,
    val sortOrder: Int,
)

data class HistoryRecord(
    val id: Long,
    val profileId: Long,
    val createdAtEpochMillis: Long,
    val referenceCenti: Int,
    val measuredCenti: Int,
    val pressureCenti: Int,
)

data class DacSnapshot(
    val currentMode: PressureMode,
    val diamondActiveProfileId: Long,
    val rubyActiveProfileId: Long,
    val profiles: List<Profile>,
    val historyRecords: List<HistoryRecord>,
) {
    fun profilesFor(mode: PressureMode): List<Profile> =
        profiles.filter { it.mode == mode }.sortedBy(Profile::sortOrder)

    fun activeProfileId(mode: PressureMode): Long =
        if (mode == PressureMode.DIAMOND) diamondActiveProfileId else rubyActiveProfileId

    fun activeProfile(mode: PressureMode = currentMode): Profile? {
        val activeId = activeProfileId(mode)
        return profiles.firstOrNull { it.id == activeId && it.mode == mode }
            ?: profilesFor(mode).firstOrNull()
    }

    fun historyFor(profileId: Long): List<HistoryRecord> =
        historyRecords
            .filter { it.profileId == profileId }
            .sortedWith(
                compareByDescending<HistoryRecord> { it.createdAtEpochMillis }
                    .thenByDescending { it.id },
            )
}

sealed interface DacDataState {
    data object Loading : DacDataState
    data class Ready(val snapshot: DacSnapshot) : DacDataState
    data class Error(val cause: Throwable) : DacDataState
}

sealed interface CommandResult {
    data object Success : CommandResult
    data object KeepOneProfile : CommandResult
    data class PressureOutOfRange(val result: PressureResult) : CommandResult
}

