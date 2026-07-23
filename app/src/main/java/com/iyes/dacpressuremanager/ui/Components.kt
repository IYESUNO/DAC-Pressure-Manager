package com.iyes.dacpressuremanager.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.iyes.dacpressuremanager.R
import com.iyes.dacpressuremanager.domain.HistoryRecord
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.Profile
import com.iyes.dacpressuremanager.domain.formatCenti
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class DigitDefinition(
    val deltaCenti: Int,
    val divisor: Int,
)

private val AllDigitDefinitions = listOf(
    DigitDefinition(deltaCenti = 100_000, divisor = 100_000),
    DigitDefinition(deltaCenti = 10_000, divisor = 10_000),
    DigitDefinition(deltaCenti = 1_000, divisor = 1_000),
    DigitDefinition(deltaCenti = 100, divisor = 100),
    DigitDefinition(deltaCenti = 10, divisor = 10),
    DigitDefinition(deltaCenti = 1, divisor = 1),
)

@Composable
fun DigitCounter(
    title: String,
    mode: PressureMode,
    field: MeasurementField,
    valueCenti: Int,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    reserveActionHeader: Boolean = false,
) {
    val definitions = if (mode == PressureMode.RUBY) {
        AllDigitDefinitions.drop(1)
    } else {
        AllDigitDefinitions
    }
    Column(
        modifier = modifier,
    ) {
        if (headerAction != null || reserveActionHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                headerAction?.invoke()
            }
            Spacer(Modifier.height(2.dp))
        } else {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Top,
        ) {
            definitions.forEachIndexed { index, definition ->
                if (index == definitions.size - 2) {
                    DecimalSeparator(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight(),
                    )
                }
                DigitColumn(
                    field = field,
                    valueCenti = valueCenti,
                    definition = definition,
                    onAdjust = onAdjust,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun DigitColumn(
    field: MeasurementField,
    valueCenti: Int,
    definition: DigitDefinition,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldName = if (field == MeasurementField.REFERENCE) {
        stringResource(R.string.reference_diamond).substringBefore(" ")
    } else {
        stringResource(R.string.measured_diamond).substringBefore(" ")
    }
    val step = formatCenti(definition.deltaCenti)
    val digit = (valueCenti / definition.divisor) % 10
    val digitDescription = stringResource(
        R.string.digit_value,
        "$fieldName $step",
        digit.toString(),
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RepeatAdjustButton(
            glyph = OperatorGlyph.Plus,
            description = stringResource(R.string.increase_digit, fieldName, step),
            onAdjust = { onAdjust(definition.deltaCenti) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        Spacer(Modifier.height(2.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.45f)
                .semantics {
                    contentDescription = digitDescription
                },
            color = Color(0xFF0F2C1C),
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 2.dp,
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = digit.toString(),
                    color = Color(0xFF00FF41),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = if (maxHeight < 38.dp) 18.sp else 27.sp,
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    ),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        RepeatAdjustButton(
            glyph = OperatorGlyph.Minus,
            description = stringResource(R.string.decrease_digit, fieldName, step),
            onAdjust = { onAdjust(-definition.deltaCenti) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun DecimalSeparator(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.45f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            DecimalGlyph(
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(bottom = 5.dp)
                    .size(5.dp),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RepeatAdjustButton(
    glyph: OperatorGlyph,
    description: String,
    onAdjust: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val background by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "digit-button-color",
    )
    Surface(
        modifier = modifier
            .scale(if (isPressed) 0.96f else 1f)
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick {
                    onAdjust()
                    true
                }
            }
            .pointerInput(onAdjust) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        var repeated = false
                        coroutineScope {
                            val repeatJob = launch {
                                delay(800)
                                repeated = true
                                onAdjust()
                                while (true) {
                                    delay(100)
                                    onAdjust()
                                }
                            }
                            val released = tryAwaitRelease()
                            repeatJob.cancelAndJoin()
                            if (released && !repeated) onAdjust()
                        }
                        isPressed = false
                    },
                )
            },
        shape = RoundedCornerShape(8.dp),
        color = background,
        tonalElevation = if (isPressed) 0.dp else 1.dp,
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            OperatorGlyphIcon(
                glyph = glyph,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(
                    if (maxHeight < 28.dp) 16.dp else 20.dp,
                ),
            )
        }
    }
}

@Composable
fun ProfileStrip(
    profiles: List<Profile>,
    activeProfileId: Long,
    onSelect: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = remember { LazyListState() }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val edgeThresholdPx = with(density) { 48.dp.toPx() }
    val maxAutoScrollPerFramePx = with(density) { 9.dp.toPx() }
    var displayedProfiles by remember { mutableStateOf(profiles) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var settlingId by remember { mutableStateOf<Long?>(null) }
    var settlingJob by remember { mutableStateOf<Job?>(null) }
    val settlingTranslation = remember { Animatable(0f) }
    var dragSourceIndex by remember { mutableIntStateOf(-1) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var dragPointerX by remember { mutableFloatStateOf(0f) }
    var initialDraggingItemOffset by remember { mutableFloatStateOf(0f) }
    var autoScrollPerFrame by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(profiles) {
        if (draggingId == null) {
            displayedProfiles = profiles
        }
    }

    fun updatePreviewOrder(movementX: Float) {
        val id = draggingId ?: return
        displayedProfiles = reorderProfilesForDrag(
            profiles = displayedProfiles,
            draggedId = id,
            visibleItems = listState.layoutInfo.visibleItemsInfo.map { item ->
                DragItemBounds(
                    key = item.key,
                    start = item.offset.toFloat(),
                    end = (item.offset + item.size).toFloat(),
                )
            },
            pointerX = dragPointerX,
            movementX = movementX,
        )
    }

    LaunchedEffect(draggingId) {
        if (draggingId == null) return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            val requestedScroll = autoScrollPerFrame
            if (requestedScroll != 0f) {
                val consumedScroll = listState.scrollBy(requestedScroll)
                if (consumedScroll == 0f) {
                    autoScrollPerFrame = 0f
                } else {
                    updatePreviewOrder(requestedScroll)
                }
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(
            items = displayedProfiles,
            key = { _, profile -> profile.id },
        ) { index, profile ->
            val isDragging = draggingId == profile.id
            val isSettling = settlingId == profile.id
            val isLifted = isDragging || isSettling
            val chipScale by animateFloatAsState(
                targetValue = if (isLifted) 1.04f else 1f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMedium,
                    dampingRatio = Spring.DampingRatioNoBouncy,
                ),
                label = "profile-chip-scale-${profile.id}",
            )
            val selectedDescription = stringResource(
                if (profile.id == activeProfileId) {
                    R.string.selected_profile
                } else {
                    R.string.select_profile
                },
                profile.name,
            )
            val moveLeftDescription = if (index > 0) {
                stringResource(
                    R.string.move_profile_before,
                    profile.name,
                    displayedProfiles[index - 1].name,
                )
            } else {
                null
            }
            val moveRightDescription = if (index < displayedProfiles.lastIndex) {
                stringResource(
                    R.string.move_profile_after,
                    profile.name,
                    displayedProfiles[index + 1].name,
                )
            } else {
                null
            }
            val accessibilityActions = buildList {
                if (moveLeftDescription != null) {
                    add(
                        CustomAccessibilityAction(
                            label = moveLeftDescription,
                            action = {
                                onMove(profile.id, index - 1)
                                true
                            },
                        ),
                    )
                }
                if (moveRightDescription != null) {
                    add(
                        CustomAccessibilityAction(
                            label = moveRightDescription,
                            action = {
                                onMove(profile.id, index + 1)
                                true
                            },
                        ),
                    )
                }
            }
            FilterChip(
                selected = profile.id == activeProfileId,
                onClick = {
                    if (draggingId == null && settlingId == null) {
                        onSelect(profile.id)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = if (isLifted) {
                        profile.mode.dragPreviewColor
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    labelColor = if (isLifted) {
                        Color(0xFF2D2D2D)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    selectedContainerColor = when {
                        isLifted -> profile.mode.dragPreviewColor
                        profile.mode == PressureMode.DIAMOND -> Color(0xFF2C3E50)
                        else -> Color(0xFFC0392B)
                    },
                    selectedLabelColor = if (isLifted) Color(0xFF2D2D2D) else Color.White,
                ),
                label = {
                    Text(
                        text = profile.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = if (isLifted) {
                            null
                        } else {
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioNoBouncy,
                            )
                        },
                    )
                    .widthIn(min = 60.dp, max = 120.dp)
                    .heightIn(min = 40.dp, max = 48.dp)
                    .zIndex(if (isLifted) 2f else 0f)
                    .shadow(
                        elevation = if (isLifted) 8.dp else 0.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                    )
                    .graphicsLayer {
                        translationX = when {
                            isDragging -> {
                                val currentOffset = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == profile.id }
                                    ?.offset
                                    ?.toFloat()
                                if (currentOffset == null) {
                                    0f
                                } else {
                                    initialDraggingItemOffset + dragDistance - currentOffset
                                }
                            }
                            isSettling -> settlingTranslation.value
                            else -> 0f
                        }
                        scaleX = chipScale
                        scaleY = chipScale
                    }
                    .semantics {
                        contentDescription = selectedDescription
                        customActions = accessibilityActions
                    }
                    .pointerInput(profile.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { touchOffset ->
                                val itemInfo = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == profile.id }
                                    ?: return@detectDragGesturesAfterLongPress
                                settlingJob?.cancel()
                                settlingId = null
                                scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                    settlingTranslation.snapTo(0f)
                                }
                                displayedProfiles = profiles
                                draggingId = profile.id
                                dragSourceIndex = profiles.indexOfFirst {
                                    it.id == profile.id
                                }
                                dragDistance = 0f
                                initialDraggingItemOffset = itemInfo.offset.toFloat()
                                dragPointerX = itemInfo.offset + touchOffset.x
                                autoScrollPerFrame = 0f
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDistance += dragAmount.x
                                dragPointerX += dragAmount.x
                                updatePreviewOrder(dragAmount.x)
                                val layoutInfo = listState.layoutInfo
                                autoScrollPerFrame = calculateAutoScroll(
                                    pointerX = dragPointerX,
                                    viewportStart = layoutInfo.viewportStartOffset.toFloat(),
                                    viewportEnd = layoutInfo.viewportEndOffset.toFloat(),
                                    edgeThreshold = edgeThresholdPx,
                                    maximumPerFrame = maxAutoScrollPerFramePx,
                                )
                            },
                            onDragEnd = {
                                val id = draggingId
                                val targetIndex = displayedProfiles.indexOfFirst {
                                    it.id == id
                                }
                                val currentOffset = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.key == id }
                                    ?.offset
                                    ?.toFloat()
                                val releaseTranslation = if (currentOffset == null) {
                                    0f
                                } else {
                                    initialDraggingItemOffset + dragDistance - currentOffset
                                }
                                autoScrollPerFrame = 0f
                                if (id != null) {
                                    settlingJob?.cancel()
                                    settlingJob = scope.launch(
                                        start = CoroutineStart.UNDISPATCHED,
                                    ) {
                                        settlingTranslation.snapTo(releaseTranslation)
                                        settlingId = id
                                        draggingId = null
                                        dragDistance = 0f
                                        settlingTranslation.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                            ),
                                        )
                                        if (settlingId == id) {
                                            settlingId = null
                                        }
                                    }
                                } else {
                                    draggingId = null
                                    dragDistance = 0f
                                }
                                if (
                                    id != null &&
                                    dragSourceIndex >= 0 &&
                                    targetIndex >= 0 &&
                                    dragSourceIndex != targetIndex
                                ) {
                                    onMove(id, targetIndex)
                                } else {
                                    displayedProfiles = profiles
                                }
                                dragSourceIndex = -1
                            },
                            onDragCancel = {
                                settlingJob?.cancel()
                                autoScrollPerFrame = 0f
                                draggingId = null
                                settlingId = null
                                dragSourceIndex = -1
                                dragDistance = 0f
                                displayedProfiles = profiles
                            },
                        )
                    },
            )
        }
        item(key = "add-profile") {
            val addProfileDescription = stringResource(R.string.add_profile_description)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = addProfileDescription
                        onClick {
                            onAdd()
                            true
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onAdd() })
                    },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        OperatorGlyphIcon(
                            glyph = OperatorGlyph.Plus,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniHistoryPanel(
    records: List<HistoryRecord>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
        ) {
            Text(
                text = stringResource(R.string.records),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(2.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(2.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (records.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Text(
                            text = "--",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                } else {
                    val visibleCount = (maxHeight.value / 26f)
                        .toInt()
                        .coerceAtLeast(1)
                    val latestId = records.first().id
                    val visibleRecords = records
                        .take(visibleCount)
                        .asReversed()
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        visibleRecords.forEach { record ->
                            MiniHistoryItem(
                                record = record,
                                isLatest = record.id == latestId,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniHistoryItem(
    record: HistoryRecord,
    isLatest: Boolean,
) {
    val latestDescription = stringResource(R.string.latest_record)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .semantics {
                if (isLatest) contentDescription = latestDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
        if (isLatest) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(5.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape,
                    ),
            )
        }
        Text(
            text = formatCenti(record.pressureCenti),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private val PressureMode.dragPreviewColor: Color
    get() = if (this == PressureMode.DIAMOND) {
        Color(0xFFF1C40F)
    } else {
        Color(0xFFFFBABA)
    }
