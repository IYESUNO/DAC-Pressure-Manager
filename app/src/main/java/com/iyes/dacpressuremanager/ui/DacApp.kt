package com.iyes.dacpressuremanager.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun DacApp(
    mainState: MainUiState,
    historyState: HistoryUiState,
    onMainAction: (MainAction) -> Unit,
    onHistoryAction: (HistoryAction) -> Unit,
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }

    MainScreen(
        state = mainState,
        onAction = onMainAction,
        onOpenHistory = { showHistory = true },
    )

    if (showHistory) {
        HistoryDialog(
            state = historyState,
            onAction = onHistoryAction,
            onDismiss = { showHistory = false },
        )
    }
}
