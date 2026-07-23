package com.iyes.dacpressuremanager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.iyes.dacpressuremanager.data.DacRepository
import com.iyes.dacpressuremanager.domain.CommandResult
import com.iyes.dacpressuremanager.domain.DacDataState
import com.iyes.dacpressuremanager.domain.DacSnapshot
import com.iyes.dacpressuremanager.domain.HistoryRecord
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureCalculator
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.Profile
import com.iyes.dacpressuremanager.ui.theme.DacTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DacComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mainScreenExposesModeProfileDigitSaveAndHistoryActions() {
        val actions = mutableListOf<MainAction>()
        var openedHistory = false
        composeRule.setContent {
            DacTheme(PressureMode.DIAMOND) {
                MainScreen(
                    state = mainContent(),
                    onAction = actions::add,
                    onOpenHistory = { openedHistory = true },
                )
            }
        }

        composeRule.onNodeWithText("RUBY").performClick()
        composeRule.onNodeWithContentDescription("Increase Measured by 1.00").performClick()
        composeRule.onNodeWithContentDescription("Reset measured to reference").performClick()
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNodeWithText("History").performClick()

        assertTrue(actions.any { it == MainAction.SelectMode(PressureMode.RUBY) })
        assertTrue(
            actions.any {
                it is MainAction.Adjust &&
                    it.field == com.iyes.dacpressuremanager.domain.MeasurementField.MEASURED &&
                    it.deltaCenti == 100
            },
        )
        assertTrue(actions.any { it is MainAction.Reset })
        assertTrue(actions.any { it is MainAction.SaveHistory })
        assertTrue(openedHistory)
    }

    @Test
    fun switchingModeReplacesAllModeSpecificDashboardContent() {
        val repository = FakeDacRepository()
        val viewModel = MainViewModel(repository)
        composeRule.setContent {
            val state by viewModel.uiState.collectAsState()
            val mode = (state as? MainUiState.Content)?.mode ?: PressureMode.DIAMOND
            DacTheme(mode) {
                MainScreen(
                    state = state,
                    onAction = viewModel::dispatch,
                    onOpenHistory = {},
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Diamond #1")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("RUBY").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Ruby #1")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Reference (λ₀)").assertIsDisplayed()
        composeRule.onNodeWithText("Measured (λ)").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Diamond #1")
                .fetchSemanticsNodes().isEmpty(),
        )

        composeRule.onNodeWithText("DIAMOND").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Diamond #1")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Reference (ω₀)").assertIsDisplayed()
        composeRule.onNodeWithText("Measured (ω)").assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Ruby #1")
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun confirmedClearAllReturnsFromHistoryToMain() {
        val repository = FakeDacRepository()
        val mainViewModel = MainViewModel(repository)
        val historyViewModel = HistoryViewModel(repository)
        composeRule.setContent {
            val mainState by mainViewModel.uiState.collectAsState()
            val historyState by historyViewModel.uiState.collectAsState()
            val mode = (mainState as? MainUiState.Content)?.mode ?: PressureMode.DIAMOND
            DacTheme(mode) {
                DacApp(
                    mainState = mainState,
                    historyState = historyState,
                    onMainAction = mainViewModel::dispatch,
                    onHistoryAction = historyViewModel::dispatch,
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("History")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("History").performClick()
        composeRule.onNodeWithText("History Records").assertIsDisplayed()
        composeRule.onNodeWithText("Clear All").performClick()
        composeRule.onAllNodesWithText("Clear All")[1].performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("History Records")
                .fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNodeWithText("DIAMOND").assertIsDisplayed()
        assertEquals(listOf(1L), repository.clearedProfileIds)
    }

    @Test
    fun navigationOpensHistoryAndApplyReturnsToMain() {
        val historyActions = mutableListOf<HistoryAction>()
        var historyState by mutableStateOf(historyContent())
        composeRule.setContent {
            DacTheme(PressureMode.DIAMOND) {
                DacApp(
                    mainState = mainContent(),
                    historyState = historyState,
                    onMainAction = {},
                    onHistoryAction = { action ->
                        historyActions += action
                        if (action is HistoryAction.ApplyRecord) {
                            historyState = historyState.copy(navigateBack = true)
                        }
                    },
                )
            }
        }

        composeRule.onNodeWithText("History").performClick()
        composeRule.onNodeWithText("History Records").assertIsDisplayed()
        composeRule.onNodeWithText("Apply").performClick()
        assertTrue(historyActions.any { it is HistoryAction.ApplyRecord })
        composeRule.onNodeWithText("DIAMOND").assertIsDisplayed()
    }

    @Test
    fun closeButtonDismissesOriginalStyleHistoryDialog() {
        var wentBack = false
        composeRule.setContent {
            DacTheme(PressureMode.DIAMOND) {
                HistoryDialog(
                    state = historyContent(),
                    onAction = {},
                    onDismiss = { wentBack = true },
                )
            }
        }

        composeRule.onNodeWithText("Close").performClick()
        assertTrue(wentBack)
    }

    @Test
    fun compactDashboardKeepsEveryOriginalSectionOnOneScreen() {
        composeRule.setContent {
            DacTheme(PressureMode.DIAMOND) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(568.dp),
                ) {
                    MainScreen(
                        state = mainContent(),
                        onAction = {},
                        onOpenHistory = {},
                    )
                }
            }
        }

        listOf(
            "DIAMOND",
            "Diamond #1",
            "Rename",
            "Delete",
            "Save",
            "History",
            "Reference (ω₀)",
            "Measured (ω)",
            "Records",
            "29.39 GPa",
            "Reset",
        ).forEach { text ->
            composeRule.onAllNodesWithText(text)[0].assertIsDisplayed()
        }
    }

    @Test
    fun longPressRepeatsDigitAdjustmentWithoutAddingATapAtRelease() {
        val actions = mutableListOf<MainAction>()
        composeRule.setContent {
            DacTheme(PressureMode.DIAMOND) {
                MainScreen(
                    state = mainContent(),
                    onAction = actions::add,
                    onOpenHistory = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Increase Measured by 1.00")
            .performTouchInput {
                down(center)
                advanceEventTime(950)
                moveBy(Offset.Zero)
                advanceEventTime(120)
                up()
            }
        composeRule.waitForIdle()

        val adjustments = actions.filterIsInstance<MainAction.Adjust>()
        assertTrue(adjustments.size >= 2)
        assertTrue(adjustments.all { it.deltaCenti == 100 })
    }

    @Test
    fun profileLongPressCanSkipPositionsAndCommitsOnlyOnceAtDrop() {
        val moves = mutableListOf<Pair<Long, Int>>()
        val profiles = (1L..4L).map { id ->
            profile().copy(
                id = id,
                name = "D$id",
                sortOrder = id.toInt() - 1,
            )
        }
        composeRule.setContent {
            DacTheme(PressureMode.DIAMOND) {
                ProfileStrip(
                    profiles = profiles,
                    activeProfileId = profiles.first().id,
                    onSelect = {},
                    onMove = { id, index -> moves += id to index },
                    onAdd = {},
                    modifier = Modifier
                        .width(360.dp)
                        .height(48.dp),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Selected profile: D1")
            .performTouchInput {
                down(center)
                advanceEventTime(650)
                moveBy(Offset(width * 2.5f, 0f))
                up()
            }
        composeRule.waitForIdle()

        assertEquals(listOf(1L to 2), moves)
    }

    @Test
    fun historyDeleteAndConfirmedClearEmitTheirActions() {
        val actions = mutableListOf<HistoryAction>()
        composeRule.setContent {
            DacTheme(PressureMode.DIAMOND) {
                HistoryDialog(
                    state = historyContent(),
                    onAction = actions::add,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithText("Clear All").performClick()
        composeRule.onAllNodesWithText("Clear All")[1].performClick()

        assertTrue(actions.any { it is HistoryAction.DeleteRecord })
        assertTrue(actions.any { it is HistoryAction.ClearHistory })
    }

    @Test
    fun emptyHistoryDisablesExport() {
        composeRule.setContent {
            DacTheme(PressureMode.DIAMOND) {
                HistoryDialog(
                    state = HistoryUiState.Content(
                        profile = profile(),
                        records = emptyList(),
                        message = null,
                        navigateBack = false,
                    ),
                    onAction = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Export").assertIsNotEnabled()
        composeRule.onNodeWithText("No history yet").assertIsDisplayed()
    }

    private fun mainContent(
        profiles: List<Profile> = listOf(profile()),
    ): MainUiState.Content {
        val profile = profiles.first()
        return MainUiState.Content(
            mode = PressureMode.DIAMOND,
            profiles = profiles,
            activeProfile = profile,
            recentRecords = listOf(record()),
            pressure = PressureCalculator.calculate(
                PressureMode.DIAMOND,
                profile.referenceCenti,
                profile.measuredCenti,
            ),
            message = null,
        )
    }

    private fun historyContent() = HistoryUiState.Content(
        profile = profile(),
        records = listOf(record()),
        message = null,
        navigateBack = false,
    )

    private fun profile() = Profile(
        id = 1,
        mode = PressureMode.DIAMOND,
        name = "Diamond #1",
        referenceCenti = 133_300,
        measuredCenti = 140_000,
        sortOrder = 0,
    )

    private fun record() = HistoryRecord(
        id = 5,
        profileId = 1,
        createdAtEpochMillis = 1_000,
        referenceCenti = 133_300,
        measuredCenti = 140_000,
        pressureCenti = 2_939,
    )

    private class FakeDacRepository : DacRepository {
        val clearedProfileIds = mutableListOf<Long>()

        private val mutableDataState = MutableStateFlow<DacDataState>(
            DacDataState.Ready(
                DacSnapshot(
                    currentMode = PressureMode.DIAMOND,
                    diamondActiveProfileId = 1,
                    rubyActiveProfileId = 2,
                    profiles = listOf(
                        Profile(
                            id = 1,
                            mode = PressureMode.DIAMOND,
                            name = "Diamond #1",
                            referenceCenti = 133_300,
                            measuredCenti = 140_000,
                            sortOrder = 0,
                        ),
                        Profile(
                            id = 2,
                            mode = PressureMode.RUBY,
                            name = "Ruby #1",
                            referenceCenti = 69_424,
                            measuredCenti = 70_000,
                            sortOrder = 0,
                        ),
                    ),
                    historyRecords = listOf(
                        HistoryRecord(
                            id = 5,
                            profileId = 1,
                            createdAtEpochMillis = 1_000,
                            referenceCenti = 133_300,
                            measuredCenti = 140_000,
                            pressureCenti = 2_939,
                        ),
                    ),
                ),
            ),
        )

        override val dataState: StateFlow<DacDataState> = mutableDataState

        override fun retryInitialization() = Unit

        override suspend fun setCurrentMode(mode: PressureMode) {
            updateSnapshot { it.copy(currentMode = mode) }
        }

        override suspend fun selectProfile(profileId: Long) {
            updateSnapshot { snapshot ->
                val mode = snapshot.profiles.first { it.id == profileId }.mode
                if (mode == PressureMode.DIAMOND) {
                    snapshot.copy(diamondActiveProfileId = profileId)
                } else {
                    snapshot.copy(rubyActiveProfileId = profileId)
                }
            }
        }

        override suspend fun addProfile(name: String): Long =
            error("Not used by this test")

        override suspend fun renameProfile(profileId: Long, name: String) = Unit

        override suspend fun deleteProfile(profileId: Long): CommandResult =
            CommandResult.Success

        override suspend fun moveProfile(profileId: Long, targetIndex: Int) = Unit

        override suspend fun adjustValue(
            profileId: Long,
            field: MeasurementField,
            deltaCenti: Int,
        ) = Unit

        override suspend fun resetMeasured(profileId: Long) = Unit

        override suspend fun saveHistory(profileId: Long): CommandResult =
            CommandResult.Success

        override suspend fun restoreHistory(recordId: Long) = Unit

        override suspend fun deleteHistory(recordId: Long) = Unit

        override suspend fun clearHistory(profileId: Long) {
            clearedProfileIds += profileId
            updateSnapshot { snapshot ->
                snapshot.copy(
                    historyRecords = snapshot.historyRecords.filterNot {
                        it.profileId == profileId
                    },
                )
            }
        }

        private fun updateSnapshot(transform: (DacSnapshot) -> DacSnapshot) {
            val ready = mutableDataState.value as DacDataState.Ready
            mutableDataState.value = DacDataState.Ready(transform(ready.snapshot))
        }
    }
}
