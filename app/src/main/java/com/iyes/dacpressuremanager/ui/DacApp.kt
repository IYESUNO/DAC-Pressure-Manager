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
    versionName: String = "",
    onOpenRelease: (String) -> Unit = {},
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    MainScreen(
        state = mainState,
        onAction = onMainAction,
        onOpenHistory = { showHistory = true },
        onOpenSettings = { showSettings = true },
    )

    if (showHistory) {
        HistoryDialog(
            state = historyState,
            onAction = onHistoryAction,
            onDismiss = { showHistory = false },
        )
    }


    val content = mainState as? MainUiState.Content
    if (showSettings && content != null) {
        SettingsDialog(
            themePreferences = content.themePreferences,
            updateState = content.updateState,
            versionName = versionName,
            onSelectPalette = { mode, palette ->
                onMainAction(MainAction.SelectThemePalette(mode, palette))
            },
            onSelectAppearance = { appearance ->
                onMainAction(MainAction.SelectThemeAppearance(appearance))
            },
            onResetPalettes = { onMainAction(MainAction.ResetThemePalettes) },
            onCheckForUpdates = { onMainAction(MainAction.CheckForUpdates) },
            onOpenRelease = onOpenRelease,
            onDismiss = { showSettings = false },
        )
    }
}
