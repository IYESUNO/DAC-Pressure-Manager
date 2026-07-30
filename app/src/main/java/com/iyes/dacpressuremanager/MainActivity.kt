package com.iyes.dacpressuremanager

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.ui.DacApp
import com.iyes.dacpressuremanager.ui.HistoryUiState
import com.iyes.dacpressuremanager.ui.HistoryViewModel
import com.iyes.dacpressuremanager.ui.MainUiState
import com.iyes.dacpressuremanager.ui.MainViewModel
import com.iyes.dacpressuremanager.ui.theme.DacTheme

class MainActivity : ComponentActivity() {
    private val screenTimeoutHandler = Handler(Looper.getMainLooper())
    private val stopKeepingScreenOn = Runnable {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private val repository by lazy {
        (application as DacApplication).container.repository
    }
    private val mainViewModel by viewModels<MainViewModel> {
        MainViewModel.Factory(repository)
    }
    private val historyViewModel by viewModels<HistoryViewModel> {
        HistoryViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()
            val mode = when {
                historyState is HistoryUiState.Content ->
                    (historyState as HistoryUiState.Content).profile.mode
                mainState is MainUiState.Content ->
                    (mainState as MainUiState.Content).mode
                else -> PressureMode.DIAMOND
            }
            DacTheme(mode = mode) {
                DacApp(
                    mainState = mainState,
                    historyState = historyState,
                    onMainAction = mainViewModel::dispatch,
                    onHistoryAction = historyViewModel::dispatch,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        restartExtendedScreenTimeout()
    }

    override fun onPause() {
        screenTimeoutHandler.removeCallbacks(stopKeepingScreenOn)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onPause()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        restartExtendedScreenTimeout()
    }

    private fun restartExtendedScreenTimeout() {
        val systemTimeoutMs = Settings.System.getLong(
            contentResolver,
            Settings.System.SCREEN_OFF_TIMEOUT,
            DEFAULT_SYSTEM_SCREEN_TIMEOUT_MS,
        )
        val extendedTimeoutMs = calculateExtendedScreenTimeoutMs(systemTimeoutMs)
        screenTimeoutHandler.removeCallbacks(stopKeepingScreenOn)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        screenTimeoutHandler.postDelayed(stopKeepingScreenOn, extendedTimeoutMs)
    }

    private companion object {
        const val DEFAULT_SYSTEM_SCREEN_TIMEOUT_MS = 2 * 60 * 1_000L
    }
}

internal fun calculateExtendedScreenTimeoutMs(systemTimeoutMs: Long): Long {
    val usableSystemTimeoutMs = systemTimeoutMs.takeIf { it > 0 }
        ?: 2 * 60 * 1_000L
    return (usableSystemTimeoutMs.coerceAtMost(5 * 60 * 1_000L) * 2)
        .coerceIn(
            minimumValue = 3 * 60 * 1_000L,
            maximumValue = 10 * 60 * 1_000L,
        )
}
