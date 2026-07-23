package com.iyes.dacpressuremanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iyes.dacpressuremanager.data.DacRepository
import com.iyes.dacpressuremanager.domain.CommandResult
import com.iyes.dacpressuremanager.domain.DacDataState
import com.iyes.dacpressuremanager.domain.DacSnapshot
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureCalculator
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.Profile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: DacRepository,
) : ViewModel() {
    private val message = MutableStateFlow<UiMessage?>(null)
    private val optimisticState = MutableStateFlow(MainOptimisticState())
    private val commandQueue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    val uiState: StateFlow<MainUiState> = combine(
        repository.dataState,
        message,
        optimisticState,
    ) { dataState, currentMessage, optimistic ->
        when (dataState) {
            DacDataState.Loading -> MainUiState.Loading
            is DacDataState.Error -> MainUiState.Error()
            is DacDataState.Ready -> {
                val snapshot = dataState.snapshot.applyOptimistic(optimistic)
                val activeProfile = requireNotNull(snapshot.activeProfile())
                MainUiState.Content(
                    mode = snapshot.currentMode,
                    profiles = snapshot.profilesFor(snapshot.currentMode),
                    activeProfile = activeProfile,
                    recentRecords = snapshot.historyFor(activeProfile.id),
                    pressure = PressureCalculator.calculate(
                        mode = snapshot.currentMode,
                        referenceCenti = activeProfile.referenceCenti,
                        measuredCenti = activeProfile.measuredCenti,
                    ),
                    message = currentMessage,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState.Loading,
    )

    init {
        viewModelScope.launch {
            for (command in commandQueue) {
                command()
            }
        }
        viewModelScope.launch {
            repository.dataState
                .filterIsInstance<DacDataState.Ready>()
                .collect { ready ->
                    optimisticState.update { it.reconcile(ready.snapshot) }
                }
        }
    }

    fun dispatch(action: MainAction) {
        when (action) {
            is MainAction.SelectMode -> {
                optimisticState.update { state ->
                    state.copy(mode = state.mode.next(action.mode))
                }
                enqueueCommand(
                    onSuccess = ::completeModeWrite,
                    onFailure = ::clearModeOverride,
                ) {
                    repository.setCurrentMode(action.mode)
                }
            }
            is MainAction.SelectProfile -> {
                val mode = contentProfile(action.profileId)?.mode
                if (mode == null) {
                    enqueueCommand { repository.selectProfile(action.profileId) }
                } else {
                    optimisticState.update { state ->
                        state.copy(
                            activeProfiles = state.activeProfiles +
                                (mode to state.activeProfiles[mode].next(action.profileId)),
                        )
                    }
                    enqueueCommand(
                        onSuccess = { completeActiveProfileWrite(mode) },
                        onFailure = { clearActiveProfileOverride(mode) },
                    ) {
                        repository.selectProfile(action.profileId)
                    }
                }
            }
            is MainAction.AddProfile -> enqueueCommand {
                repository.addProfile(action.name)
            }
            is MainAction.RenameProfile -> {
                val cleanName = action.name.trim()
                val profile = contentProfile(action.profileId)
                if (profile == null || cleanName.isEmpty()) {
                    enqueueCommand {
                        repository.renameProfile(action.profileId, cleanName)
                    }
                } else {
                    optimisticState.update { state ->
                        state.copy(
                            names = state.names +
                                (action.profileId to state.names[action.profileId].next(cleanName)),
                        )
                    }
                    enqueueCommand(
                        onSuccess = { completeNameWrite(action.profileId) },
                        onFailure = { clearNameOverride(action.profileId) },
                    ) {
                        repository.renameProfile(action.profileId, cleanName)
                    }
                }
            }
            is MainAction.DeleteProfile -> enqueueCommand {
                when (repository.deleteProfile(action.profileId)) {
                    CommandResult.KeepOneProfile -> message.value = UiMessage.KEEP_ONE_PROFILE
                    else -> Unit
                }
            }
            is MainAction.MoveProfile -> {
                val content = uiState.value as? MainUiState.Content
                val ids = content?.profiles?.map(Profile::id).orEmpty()
                val sourceIndex = ids.indexOf(action.profileId)
                if (content == null || sourceIndex == -1 || ids.isEmpty()) {
                    enqueueCommand {
                        repository.moveProfile(action.profileId, action.targetIndex)
                    }
                } else {
                    val targetIndex = action.targetIndex.coerceIn(0, ids.lastIndex)
                    if (sourceIndex != targetIndex) {
                        val reordered = ids.toMutableList().apply {
                            add(targetIndex, removeAt(sourceIndex))
                        }
                        val mode = content.mode
                        optimisticState.update { state ->
                            state.copy(
                                profileOrders = state.profileOrders +
                                    (mode to state.profileOrders[mode].next(reordered)),
                            )
                        }
                        enqueueCommand(
                            onSuccess = { completeProfileOrderWrite(mode) },
                            onFailure = { clearProfileOrderOverride(mode) },
                        ) {
                            repository.moveProfile(action.profileId, targetIndex)
                        }
                    }
                }
            }
            is MainAction.Adjust -> {
                val profile = contentProfile(action.profileId)
                if (profile != null && beginValueAdjustment(profile, action)) {
                    enqueueCommand(
                        onSuccess = { completeValueWrite(action.profileId) },
                        onFailure = { clearValueOverride(action.profileId) },
                    ) {
                        repository.adjustValue(
                            action.profileId,
                            action.field,
                            action.deltaCenti,
                        )
                    }
                }
            }
            is MainAction.Reset -> {
                val profile = contentProfile(action.profileId)
                if (profile == null) {
                    enqueueCommand { repository.resetMeasured(action.profileId) }
                } else {
                    optimisticState.update { state ->
                        val current = state.values[action.profileId]
                            ?: PendingOverride(
                                target = ProfileValues(
                                    referenceCenti = profile.referenceCenti,
                                    measuredCenti = profile.measuredCenti,
                                ),
                                pendingWrites = 0,
                            )
                        state.copy(
                            values = state.values + (
                                action.profileId to current.copy(
                                    target = current.target.copy(
                                        measuredCenti = current.target.referenceCenti,
                                    ),
                                    pendingWrites = current.pendingWrites + 1,
                                )
                            ),
                        )
                    }
                    enqueueCommand(
                        onSuccess = { completeValueWrite(action.profileId) },
                        onFailure = { clearValueOverride(action.profileId) },
                    ) {
                        repository.resetMeasured(action.profileId)
                    }
                }
            }
            is MainAction.SaveHistory -> enqueueCommand {
                if (repository.saveHistory(action.profileId) is CommandResult.PressureOutOfRange) {
                    message.value = UiMessage.CANNOT_SAVE_OUT_OF_RANGE
                }
            }
            MainAction.Retry -> repository.retryInitialization()
            MainAction.MessageShown -> message.value = null
        }
    }

    private fun enqueueCommand(
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit = {},
        block: suspend () -> Unit,
    ) {
        val result = commandQueue.trySend {
            runCatching { block() }
                .onSuccess { onSuccess() }
                .onFailure {
                    onFailure()
                    message.value = UiMessage.DATABASE_ERROR
                }
        }
        if (result.isFailure) {
            onFailure()
            message.value = UiMessage.DATABASE_ERROR
        }
    }

    private fun contentProfile(profileId: Long): Profile? =
        (uiState.value as? MainUiState.Content)
            ?.profiles
            ?.firstOrNull { it.id == profileId }

    private fun beginValueAdjustment(
        profile: Profile,
        action: MainAction.Adjust,
    ): Boolean {
        var accepted = false
        optimisticState.update { state ->
            val current = state.values[action.profileId]
                ?: PendingOverride(
                    target = ProfileValues(
                        referenceCenti = profile.referenceCenti,
                        measuredCenti = profile.measuredCenti,
                    ),
                    pendingWrites = 0,
                )
            val currentValue = when (action.field) {
                MeasurementField.REFERENCE -> current.target.referenceCenti
                MeasurementField.MEASURED -> current.target.measuredCenti
            }
            val adjusted = currentValue + action.deltaCenti
            if (adjusted !in profile.mode.minimumValueCenti..profile.mode.maximumValueCenti) {
                state
            } else {
                accepted = true
                val target = when (action.field) {
                    MeasurementField.REFERENCE ->
                        current.target.copy(referenceCenti = adjusted)
                    MeasurementField.MEASURED ->
                        current.target.copy(measuredCenti = adjusted)
                }
                state.copy(
                    values = state.values + (
                        action.profileId to current.copy(
                            target = target,
                            pendingWrites = current.pendingWrites + 1,
                        )
                    ),
                )
            }
        }
        return accepted
    }

    private fun completeModeWrite() = updateOptimisticAfterWrite { state ->
        state.copy(mode = state.mode.decrement())
    }

    private fun clearModeOverride() {
        optimisticState.update { it.copy(mode = null) }
    }

    private fun completeActiveProfileWrite(mode: PressureMode) =
        updateOptimisticAfterWrite { state ->
            state.copy(
                activeProfiles = state.activeProfiles.updatePending(mode),
            )
        }

    private fun clearActiveProfileOverride(mode: PressureMode) {
        optimisticState.update { state ->
            state.copy(activeProfiles = state.activeProfiles - mode)
        }
    }

    private fun completeNameWrite(profileId: Long) =
        updateOptimisticAfterWrite { state ->
            state.copy(names = state.names.updatePending(profileId))
        }

    private fun clearNameOverride(profileId: Long) {
        optimisticState.update { state ->
            state.copy(names = state.names - profileId)
        }
    }

    private fun completeProfileOrderWrite(mode: PressureMode) =
        updateOptimisticAfterWrite { state ->
            state.copy(profileOrders = state.profileOrders.updatePending(mode))
        }

    private fun clearProfileOrderOverride(mode: PressureMode) {
        optimisticState.update { state ->
            state.copy(profileOrders = state.profileOrders - mode)
        }
    }

    private fun completeValueWrite(profileId: Long) =
        updateOptimisticAfterWrite { state ->
            state.copy(values = state.values.updatePending(profileId))
        }

    private fun clearValueOverride(profileId: Long) {
        optimisticState.update { state ->
            state.copy(values = state.values - profileId)
        }
    }

    private fun updateOptimisticAfterWrite(
        transform: (MainOptimisticState) -> MainOptimisticState,
    ) {
        val snapshot = (repository.dataState.value as? DacDataState.Ready)?.snapshot
        optimisticState.update { state ->
            val updated = transform(state)
            if (snapshot == null) updated else updated.reconcile(snapshot)
        }
    }

    class Factory(
        private val repository: DacRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository) as T
    }
}

private data class PendingOverride<T>(
    val target: T,
    val pendingWrites: Int,
)

private data class ProfileValues(
    val referenceCenti: Int,
    val measuredCenti: Int,
)

private data class MainOptimisticState(
    val mode: PendingOverride<PressureMode>? = null,
    val activeProfiles: Map<PressureMode, PendingOverride<Long>> = emptyMap(),
    val names: Map<Long, PendingOverride<String>> = emptyMap(),
    val profileOrders: Map<PressureMode, PendingOverride<List<Long>>> = emptyMap(),
    val values: Map<Long, PendingOverride<ProfileValues>> = emptyMap(),
)

private fun <T> PendingOverride<T>?.next(target: T): PendingOverride<T> =
    PendingOverride(
        target = target,
        pendingWrites = (this?.pendingWrites ?: 0) + 1,
    )

private fun <T> PendingOverride<T>?.decrement(): PendingOverride<T>? =
    this?.copy(pendingWrites = (pendingWrites - 1).coerceAtLeast(0))

private fun <K, V> Map<K, PendingOverride<V>>.updatePending(
    key: K,
): Map<K, PendingOverride<V>> {
    val pending = this[key] ?: return this
    return this + (key to pending.copy(
        pendingWrites = (pending.pendingWrites - 1).coerceAtLeast(0),
    ))
}

private fun DacSnapshot.applyOptimistic(
    optimistic: MainOptimisticState,
): DacSnapshot {
    val orderIndexes = optimistic.profileOrders.mapValues { (_, pending) ->
        pending.target.withIndex().associate { it.value to it.index }
    }
    val adjustedProfiles = profiles.map { profile ->
        val values = optimistic.values[profile.id]?.target
        profile.copy(
            name = optimistic.names[profile.id]?.target ?: profile.name,
            referenceCenti = values?.referenceCenti ?: profile.referenceCenti,
            measuredCenti = values?.measuredCenti ?: profile.measuredCenti,
            sortOrder = orderIndexes[profile.mode]?.get(profile.id) ?: profile.sortOrder,
        )
    }
    return copy(
        currentMode = optimistic.mode?.target ?: currentMode,
        diamondActiveProfileId =
            optimistic.activeProfiles[PressureMode.DIAMOND]?.target
                ?: diamondActiveProfileId,
        rubyActiveProfileId =
            optimistic.activeProfiles[PressureMode.RUBY]?.target
                ?: rubyActiveProfileId,
        profiles = adjustedProfiles,
    )
}

private fun MainOptimisticState.reconcile(
    snapshot: DacSnapshot,
): MainOptimisticState = copy(
    mode = mode.keepUnlessPersisted(snapshot.currentMode),
    activeProfiles = activeProfiles.filterValuesByKey { mode, pending ->
        snapshot.activeProfileId(mode) == pending.target
    },
    names = names.filterValuesByKey { profileId, pending ->
        snapshot.profiles.firstOrNull { it.id == profileId }?.name == pending.target
    },
    profileOrders = profileOrders.filterValuesByKey { mode, pending ->
        snapshot.profilesFor(mode).map(Profile::id) == pending.target
    },
    values = values.filterValuesByKey { profileId, pending ->
        snapshot.profiles.firstOrNull { it.id == profileId }?.let { profile ->
            profile.referenceCenti == pending.target.referenceCenti &&
                profile.measuredCenti == pending.target.measuredCenti
        } == true
    },
)

private fun <T> PendingOverride<T>?.keepUnlessPersisted(
    persisted: T,
): PendingOverride<T>? =
    this?.takeUnless { it.pendingWrites == 0 && it.target == persisted }

private inline fun <K, V> Map<K, PendingOverride<V>>.filterValuesByKey(
    isPersisted: (K, PendingOverride<V>) -> Boolean,
): Map<K, PendingOverride<V>> = filterNot { (key, pending) ->
    pending.pendingWrites == 0 && isPersisted(key, pending)
}
