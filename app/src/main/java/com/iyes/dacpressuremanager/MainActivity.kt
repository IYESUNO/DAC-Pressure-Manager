package com.iyes.dacpressuremanager

import android.os.Bundle
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
}
