package com.iyes.dacpressuremanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.iyes.dacpressuremanager.data.DacRepository
import com.iyes.dacpressuremanager.domain.DacDataState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: DacRepository,
) : ViewModel() {
    private val message = MutableStateFlow<UiMessage?>(null)
    private val navigateBack = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.dataState,
        message,
        navigateBack,
    ) { dataState, currentMessage, shouldNavigateBack ->
        when (dataState) {
            DacDataState.Loading -> HistoryUiState.Loading
            is DacDataState.Error -> HistoryUiState.Error()
            is DacDataState.Ready -> {
                val snapshot = dataState.snapshot
                val profile = requireNotNull(snapshot.activeProfile())
                HistoryUiState.Content(
                    profile = profile,
                    records = snapshot.historyFor(profile.id).asReversed(),
                    message = currentMessage,
                    navigateBack = shouldNavigateBack,
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState.Loading,
    )

    fun dispatch(action: HistoryAction) {
        when (action) {
            is HistoryAction.ApplyRecord -> launchCommand {
                repository.restoreHistory(action.recordId)
                navigateBack.value = true
            }
            is HistoryAction.DeleteRecord -> launchCommand {
                repository.deleteHistory(action.recordId)
            }
            is HistoryAction.ClearHistory -> launchCommand {
                repository.clearHistory(action.profileId)
            }
            is HistoryAction.ReportMessage -> message.value = action.message
            HistoryAction.Retry -> repository.retryInitialization()
            HistoryAction.MessageShown -> message.value = null
            HistoryAction.NavigationHandled -> navigateBack.value = false
        }
    }

    private fun launchCommand(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { message.value = UiMessage.DATABASE_ERROR }
        }
    }

    class Factory(
        private val repository: DacRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository) as T
    }
}

