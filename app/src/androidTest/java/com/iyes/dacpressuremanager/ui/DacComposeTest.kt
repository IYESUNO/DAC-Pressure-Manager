package com.iyes.dacpressuremanager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.iyes.dacpressuremanager.domain.HistoryRecord
import com.iyes.dacpressuremanager.domain.PressureCalculator
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.Profile
import com.iyes.dacpressuremanager.ui.theme.DacTheme
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
        assertTrue(actions.any { it is MainAction.SaveHistory })
        assertTrue(openedHistory)
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
}
