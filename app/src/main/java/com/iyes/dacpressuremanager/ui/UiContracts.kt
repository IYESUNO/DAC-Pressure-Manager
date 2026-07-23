package com.iyes.dacpressuremanager.ui

import com.iyes.dacpressuremanager.domain.HistoryRecord
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureComputation
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.Profile

enum class UiMessage {
    KEEP_ONE_PROFILE,
    CANNOT_SAVE_OUT_OF_RANGE,
    DATABASE_ERROR,
    EXPORT_FAILED,
    SHARE_FAILED,
}

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Error(val message: UiMessage = UiMessage.DATABASE_ERROR) : MainUiState
    data class Content(
        val mode: PressureMode,
        val profiles: List<Profile>,
        val activeProfile: Profile,
        val recentRecords: List<HistoryRecord>,
        val pressure: PressureComputation,
        val message: UiMessage?,
    ) : MainUiState
}

sealed interface MainAction {
    data class SelectMode(val mode: PressureMode) : MainAction
    data class SelectProfile(val profileId: Long) : MainAction
    data class AddProfile(val name: String) : MainAction
    data class RenameProfile(val profileId: Long, val name: String) : MainAction
    data class DeleteProfile(val profileId: Long) : MainAction
    data class MoveProfile(val profileId: Long, val targetIndex: Int) : MainAction
    data class Adjust(
        val profileId: Long,
        val field: MeasurementField,
        val deltaCenti: Int,
    ) : MainAction
    data class Reset(val profileId: Long) : MainAction
    data class SaveHistory(val profileId: Long) : MainAction
    data object Retry : MainAction
    data object MessageShown : MainAction
}

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Error(val message: UiMessage = UiMessage.DATABASE_ERROR) : HistoryUiState
    data class Content(
        val profile: Profile,
        val records: List<HistoryRecord>,
        val message: UiMessage?,
        val navigateBack: Boolean,
    ) : HistoryUiState
}

sealed interface HistoryAction {
    data class ApplyRecord(val recordId: Long) : HistoryAction
    data class DeleteRecord(val recordId: Long) : HistoryAction
    data class ClearHistory(val profileId: Long) : HistoryAction
    data class ReportMessage(val message: UiMessage) : HistoryAction
    data object Retry : HistoryAction
    data object MessageShown : HistoryAction
    data object NavigationHandled : HistoryAction
}

