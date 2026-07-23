package com.iyes.dacpressuremanager.ui

import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.Profile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileDragTest {
    private val profiles = (1L..4L).map { id ->
        Profile(
            id = id,
            mode = PressureMode.DIAMOND,
            name = "D$id",
            referenceCenti = 133_300,
            measuredCenti = 133_300,
            sortOrder = id.toInt() - 1,
        )
    }
    private val bounds = profiles.mapIndexed { index, profile ->
        val start = index * 68f
        DragItemBounds(
            key = profile.id,
            start = start,
            end = start + 60f,
        )
    }

    @Test
    fun draggingCanSkipAnIntermediatePosition() {
        val result = reorderProfilesForDrag(
            profiles = profiles,
            draggedId = 1L,
            visibleItems = bounds,
            pointerX = 180f,
            movementX = 150f,
        )

        assertEquals(listOf(2L, 3L, 1L, 4L), result.map(Profile::id))
    }

    @Test
    fun targetDoesNotChangeUntilPointerCrossesItsMidpoint() {
        val result = reorderProfilesForDrag(
            profiles = profiles,
            draggedId = 1L,
            visibleItems = bounds,
            pointerX = 150f,
            movementX = 120f,
        )

        assertEquals(profiles, result)
    }

    @Test
    fun draggingBackwardsMovesToEarlierPosition() {
        val result = reorderProfilesForDrag(
            profiles = profiles,
            draggedId = 3L,
            visibleItems = bounds,
            pointerX = 25f,
            movementX = -140f,
        )

        assertEquals(listOf(3L, 1L, 2L, 4L), result.map(Profile::id))
    }

    @Test
    fun edgeScrollIsProportionalAndStopsAwayFromEdges() {
        assertEquals(
            -12f,
            calculateAutoScroll(
                pointerX = 0f,
                viewportStart = 0f,
                viewportEnd = 360f,
                edgeThreshold = 48f,
                maximumPerFrame = 12f,
            ),
            0.001f,
        )
        assertEquals(
            0f,
            calculateAutoScroll(
                pointerX = 180f,
                viewportStart = 0f,
                viewportEnd = 360f,
                edgeThreshold = 48f,
                maximumPerFrame = 12f,
            ),
            0.001f,
        )
    }
}
