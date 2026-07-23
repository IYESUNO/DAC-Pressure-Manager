package com.iyes.dacpressuremanager.ui

import com.iyes.dacpressuremanager.domain.Profile
import kotlin.math.abs

internal data class DragItemBounds(
    val key: Any,
    val start: Float,
    val end: Float,
)

internal fun reorderProfilesForDrag(
    profiles: List<Profile>,
    draggedId: Long,
    visibleItems: List<DragItemBounds>,
    pointerX: Float,
    movementX: Float,
): List<Profile> {
    if (movementX == 0f) return profiles
    val sourceIndex = profiles.indexOfFirst { it.id == draggedId }
    if (sourceIndex == -1) return profiles

    val targetBounds = visibleItems
        .asSequence()
        .filter { it.key != draggedId && it.key is Long }
        .filter { pointerX in it.start..it.end }
        .minByOrNull { bounds ->
            abs(pointerX - (bounds.start + bounds.end) / 2f)
        }
        ?: return profiles
    val targetId = targetBounds.key as Long
    val targetIndex = profiles.indexOfFirst { it.id == targetId }
    if (targetIndex == -1 || targetIndex == sourceIndex) return profiles

    val targetMiddle = (targetBounds.start + targetBounds.end) / 2f
    val crossedTarget = when {
        movementX > 0f -> targetIndex > sourceIndex && pointerX >= targetMiddle
        else -> targetIndex < sourceIndex && pointerX <= targetMiddle
    }
    if (!crossedTarget) return profiles

    return profiles.toMutableList().apply {
        add(targetIndex, removeAt(sourceIndex))
    }
}

internal fun calculateAutoScroll(
    pointerX: Float,
    viewportStart: Float,
    viewportEnd: Float,
    edgeThreshold: Float,
    maximumPerFrame: Float,
): Float = when {
    pointerX < viewportStart + edgeThreshold -> {
        val intensity = (
            (viewportStart + edgeThreshold - pointerX) / edgeThreshold
            ).coerceIn(0f, 1f)
        -maximumPerFrame * intensity
    }
    pointerX > viewportEnd - edgeThreshold -> {
        val intensity = (
            (pointerX - (viewportEnd - edgeThreshold)) / edgeThreshold
            ).coerceIn(0f, 1f)
        maximumPerFrame * intensity
    }
    else -> 0f
}
