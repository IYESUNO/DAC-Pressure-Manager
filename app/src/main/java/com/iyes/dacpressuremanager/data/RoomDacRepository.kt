package com.iyes.dacpressuremanager.data

import androidx.room.withTransaction
import com.iyes.dacpressuremanager.data.local.AppStateEntity
import com.iyes.dacpressuremanager.data.local.DacDao
import com.iyes.dacpressuremanager.data.local.DacDatabase
import com.iyes.dacpressuremanager.data.local.HistoryRecordEntity
import com.iyes.dacpressuremanager.data.local.ProfileEntity
import com.iyes.dacpressuremanager.domain.CommandResult
import com.iyes.dacpressuremanager.domain.DacDataState
import com.iyes.dacpressuremanager.domain.DacSnapshot
import com.iyes.dacpressuremanager.domain.HistoryRecord
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureCalculator
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.PressureResult
import com.iyes.dacpressuremanager.domain.Profile
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomDacRepository(
    private val database: DacDatabase,
    private val applicationScope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
) : DacRepository {
    private val dao: DacDao = database.dacDao()
    private val initialization = MutableStateFlow<Initialization>(Initialization.Loading)
    private val initializationJob = AtomicReference<Job?>()
    private val writeMutex = Mutex()

    override val dataState: StateFlow<DacDataState> = combine(
        initialization,
        dao.observeAppState(),
        dao.observeProfiles(),
        dao.observeHistory(),
    ) { init, appState, profiles, history ->
        when (init) {
            Initialization.Loading -> DacDataState.Loading
            is Initialization.Error -> DacDataState.Error(init.cause)
            Initialization.Ready -> {
                val diamondProfiles = profiles.filter {
                    it.mode == PressureMode.DIAMOND.storageValue
                }
                val rubyProfiles = profiles.filter {
                    it.mode == PressureMode.RUBY.storageValue
                }
                if (appState == null || diamondProfiles.isEmpty() || rubyProfiles.isEmpty()) {
                    DacDataState.Loading
                } else {
                    DacDataState.Ready(
                        DacSnapshot(
                            currentMode = PressureMode.fromStorage(appState.currentMode),
                            diamondActiveProfileId =
                                appState.diamondActiveProfileId ?: diamondProfiles.first().id,
                            rubyActiveProfileId =
                                appState.rubyActiveProfileId ?: rubyProfiles.first().id,
                            profiles = profiles.map(ProfileEntity::toDomain),
                            historyRecords = history.map(HistoryRecordEntity::toDomain),
                        ),
                    )
                }
            }
        }
    }.stateIn(
        scope = applicationScope,
        started = SharingStarted.Eagerly,
        initialValue = DacDataState.Loading,
    )

    init {
        retryInitialization()
    }

    override fun retryInitialization() {
        initializationJob.getAndSet(
            applicationScope.launch {
                initialization.value = Initialization.Loading
                initialization.value = runCatching {
                    ensureInitialized()
                    Initialization.Ready
                }.getOrElse(Initialization::Error)
            },
        )?.cancel()
    }

    override suspend fun setCurrentMode(mode: PressureMode) = writeMutex.withLock {
        database.withTransaction {
            val state = requireState()
            dao.upsertAppState(state.copy(currentMode = mode.storageValue))
        }
    }

    override suspend fun selectProfile(profileId: Long) = writeMutex.withLock {
        database.withTransaction {
            val profile = dao.getProfile(profileId) ?: return@withTransaction
            val mode = PressureMode.fromStorage(profile.mode)
            val state = requireState()
            dao.upsertAppState(state.withActiveProfile(mode, profileId))
        }
    }

    override suspend fun addProfile(name: String): Long = writeMutex.withLock {
        database.withTransaction {
            val cleanName = name.trim()
            require(cleanName.isNotEmpty())
            val state = requireState()
            val mode = PressureMode.fromStorage(state.currentMode)
            val profiles = dao.getProfiles(mode.storageValue)
            val newId = dao.insertProfile(
                ProfileEntity(
                    mode = mode.storageValue,
                    name = cleanName,
                    referenceCenti = mode.defaultValueCenti,
                    measuredCenti = mode.defaultValueCenti,
                    sortOrder = (profiles.maxOfOrNull(ProfileEntity::sortOrder) ?: -1) + 1,
                ),
            )
            dao.upsertAppState(state.withActiveProfile(mode, newId))
            newId
        }
    }

    override suspend fun renameProfile(profileId: Long, name: String) = writeMutex.withLock {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty())
        dao.updateProfileName(profileId, cleanName)
    }

    override suspend fun deleteProfile(profileId: Long): CommandResult = writeMutex.withLock {
        database.withTransaction {
            val profile = dao.getProfile(profileId) ?: return@withTransaction CommandResult.Success
            val mode = PressureMode.fromStorage(profile.mode)
            val profiles = dao.getProfiles(mode.storageValue)
            if (profiles.size <= 1) return@withTransaction CommandResult.KeepOneProfile

            dao.deleteProfile(profile)
            val state = requireState()
            if (state.activeProfileId(mode) == profileId) {
                val replacementId = profiles.first { it.id != profileId }.id
                dao.upsertAppState(state.withActiveProfile(mode, replacementId))
            }
            CommandResult.Success
        }
    }

    override suspend fun moveProfile(profileId: Long, targetIndex: Int) = writeMutex.withLock {
        database.withTransaction {
            val profile = dao.getProfile(profileId) ?: return@withTransaction
            val profiles = dao.getProfiles(profile.mode).toMutableList()
            val fromIndex = profiles.indexOfFirst { it.id == profileId }
            if (fromIndex == -1) return@withTransaction
            val boundedTarget = targetIndex.coerceIn(0, profiles.lastIndex)
            if (fromIndex == boundedTarget) return@withTransaction

            val moved = profiles.removeAt(fromIndex)
            profiles.add(boundedTarget, moved)
            profiles.forEachIndexed { index, item ->
                if (item.sortOrder != index) dao.updateProfileSortOrder(item.id, index)
            }
        }
    }

    override suspend fun adjustValue(
        profileId: Long,
        field: MeasurementField,
        deltaCenti: Int,
    ) = writeMutex.withLock {
        database.withTransaction {
            val profile = dao.getProfile(profileId) ?: return@withTransaction
            val mode = PressureMode.fromStorage(profile.mode)
            val current = when (field) {
                MeasurementField.REFERENCE -> profile.referenceCenti
                MeasurementField.MEASURED -> profile.measuredCenti
            }
            val adjusted = current + deltaCenti
            if (adjusted !in mode.minimumValueCenti..mode.maximumValueCenti) {
                return@withTransaction
            }
            dao.updateProfileValues(
                profileId = profileId,
                referenceCenti = if (field == MeasurementField.REFERENCE) {
                    adjusted
                } else {
                    profile.referenceCenti
                },
                measuredCenti = if (field == MeasurementField.MEASURED) {
                    adjusted
                } else {
                    profile.measuredCenti
                },
            )
        }
    }

    override suspend fun resetMeasured(profileId: Long) = writeMutex.withLock {
        database.withTransaction {
            val profile = dao.getProfile(profileId) ?: return@withTransaction
            dao.updateProfileValues(
                profileId = profileId,
                referenceCenti = profile.referenceCenti,
                measuredCenti = profile.referenceCenti,
            )
        }
    }

    override suspend fun saveHistory(profileId: Long): CommandResult = writeMutex.withLock {
        database.withTransaction {
            val profile = dao.getProfile(profileId) ?: return@withTransaction CommandResult.Success
            val mode = PressureMode.fromStorage(profile.mode)
            val result = PressureCalculator.calculate(
                mode = mode,
                referenceCenti = profile.referenceCenti,
                measuredCenti = profile.measuredCenti,
            ).result
            if (result !is PressureResult.Valid) {
                return@withTransaction CommandResult.PressureOutOfRange(result)
            }
            dao.insertHistoryRecord(
                HistoryRecordEntity(
                    profileId = profileId,
                    createdAtEpochMillis = now(),
                    referenceCenti = profile.referenceCenti,
                    measuredCenti = profile.measuredCenti,
                    pressureCenti = result.pressureCenti,
                ),
            )
            dao.trimHistory(profileId, HISTORY_LIMIT)
            CommandResult.Success
        }
    }

    override suspend fun restoreHistory(recordId: Long) = writeMutex.withLock {
        database.withTransaction {
            val record = dao.getHistoryRecord(recordId) ?: return@withTransaction
            val profile = dao.getProfile(record.profileId) ?: return@withTransaction
            dao.updateProfileValues(
                profileId = profile.id,
                referenceCenti = record.referenceCenti,
                measuredCenti = record.measuredCenti,
            )
        }
    }

    override suspend fun deleteHistory(recordId: Long) = writeMutex.withLock {
        dao.deleteHistoryRecord(recordId)
    }

    override suspend fun clearHistory(profileId: Long) = writeMutex.withLock {
        dao.clearHistory(profileId)
    }

    private suspend fun ensureInitialized() {
        writeMutex.withLock {
            database.withTransaction {
                val diamondProfiles = ensureModeProfile(PressureMode.DIAMOND)
                val rubyProfiles = ensureModeProfile(PressureMode.RUBY)
                val previous = dao.getAppState()
                val currentMode = PressureMode.fromStorage(previous?.currentMode)
                val diamondActive = previous?.diamondActiveProfileId
                    ?.takeIf { id -> diamondProfiles.any { it.id == id } }
                    ?: diamondProfiles.first().id
                val rubyActive = previous?.rubyActiveProfileId
                    ?.takeIf { id -> rubyProfiles.any { it.id == id } }
                    ?: rubyProfiles.first().id
                dao.upsertAppState(
                    AppStateEntity(
                        currentMode = currentMode.storageValue,
                        diamondActiveProfileId = diamondActive,
                        rubyActiveProfileId = rubyActive,
                    ),
                )
            }
        }
    }

    private suspend fun ensureModeProfile(mode: PressureMode): List<ProfileEntity> {
        val existing = dao.getProfiles(mode.storageValue)
        if (existing.isNotEmpty()) return existing
        val id = dao.insertProfile(
            ProfileEntity(
                mode = mode.storageValue,
                name = "${mode.profilePrefix} #1",
                referenceCenti = mode.defaultValueCenti,
                measuredCenti = mode.defaultValueCenti,
                sortOrder = 0,
            ),
        )
        return listOf(requireNotNull(dao.getProfile(id)))
    }

    private suspend fun requireState(): AppStateEntity =
        requireNotNull(dao.getAppState()) { "Repository is not initialized" }

    private fun AppStateEntity.activeProfileId(mode: PressureMode): Long? =
        if (mode == PressureMode.DIAMOND) diamondActiveProfileId else rubyActiveProfileId

    private fun AppStateEntity.withActiveProfile(
        mode: PressureMode,
        profileId: Long,
    ): AppStateEntity = if (mode == PressureMode.DIAMOND) {
        copy(diamondActiveProfileId = profileId)
    } else {
        copy(rubyActiveProfileId = profileId)
    }

    private sealed interface Initialization {
        data object Loading : Initialization
        data object Ready : Initialization
        data class Error(val cause: Throwable) : Initialization
    }

    private companion object {
        const val HISTORY_LIMIT = 50
    }
}

private fun ProfileEntity.toDomain(): Profile = Profile(
    id = id,
    mode = PressureMode.fromStorage(mode),
    name = name,
    referenceCenti = referenceCenti,
    measuredCenti = measuredCenti,
    sortOrder = sortOrder,
)

private fun HistoryRecordEntity.toDomain(): HistoryRecord = HistoryRecord(
    id = id,
    profileId = profileId,
    createdAtEpochMillis = createdAtEpochMillis,
    referenceCenti = referenceCenti,
    measuredCenti = measuredCenti,
    pressureCenti = pressureCenti,
)

