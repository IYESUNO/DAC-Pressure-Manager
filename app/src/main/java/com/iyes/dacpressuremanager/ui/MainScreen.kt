package com.iyes.dacpressuremanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iyes.dacpressuremanager.R
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.PressureResult
import com.iyes.dacpressuremanager.domain.Profile
import com.iyes.dacpressuremanager.domain.RubyTemperature
import com.iyes.dacpressuremanager.domain.formatCenti
import com.iyes.dacpressuremanager.ui.theme.DacResultFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    state: MainUiState,
    onAction: (MainAction) -> Unit,
    onOpenHistory: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val content = state as? MainUiState.Content
    val message = content?.message
    val messageText = message?.let { uiMessageText(it) }
    LaunchedEffect(message, messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onAction(MainAction.MessageShown)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        when (state) {
            MainUiState.Loading -> LoadingContent(innerPadding)
            is MainUiState.Error -> ErrorContent(
                padding = innerPadding,
                onRetry = { onAction(MainAction.Retry) },
            )
            is MainUiState.Content -> MainDashboard(
                state = state,
                padding = innerPadding,
                onAction = onAction,
                onOpenHistory = onOpenHistory,
            )
        }
    }
}

@Composable
private fun MainDashboard(
    state: MainUiState.Content,
    padding: PaddingValues,
    onAction: (MainAction) -> Unit,
    onOpenHistory: () -> Unit,
) {
    var profileDialog by rememberSaveable { mutableStateOf<ProfileDialogKind?>(null) }
    var deleteProfileId by rememberSaveable { mutableStateOf<Long?>(null) }
    var temperatureDialogProfileId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingSave by remember { mutableStateOf<Pair<Long, Long?>?>(null) }
    var showSaved by remember { mutableStateOf(false) }
    val latestRecordId = state.recentRecords.firstOrNull()?.id

    LaunchedEffect(state.activeProfile.id) {
        pendingSave = null
        showSaved = false
        if (temperatureDialogProfileId != state.activeProfile.id) {
            temperatureDialogProfileId = null
        }
    }
    LaunchedEffect(latestRecordId, pendingSave) {
        val request = pendingSave
        if (
            request != null &&
            request.first == state.activeProfile.id &&
            latestRecordId != request.second
        ) {
            pendingSave = null
            showSaved = true
            delay(900)
            showSaved = false
        }
    }

    val deleteProfile = state.profiles.firstOrNull { it.id == deleteProfileId }
    if (profileDialog != null) {
        val dialogKind = requireNotNull(profileDialog)
        val initialName = when (dialogKind) {
            ProfileDialogKind.Add ->
                "${state.mode.profilePrefix} #${state.profiles.size + 1}"
            ProfileDialogKind.Rename -> state.activeProfile.name
        }
        ProfileNameDialog(
            kind = dialogKind,
            initialName = initialName,
            onDismiss = { profileDialog = null },
            onConfirm = { name ->
                when (profileDialog) {
                    ProfileDialogKind.Add -> onAction(MainAction.AddProfile(name))
                    ProfileDialogKind.Rename -> onAction(
                        MainAction.RenameProfile(state.activeProfile.id, name),
                    )
                    null -> Unit
                }
                profileDialog = null
            },
        )
    }
    if (deleteProfile != null) {
        AlertDialog(
            onDismissRequest = { deleteProfileId = null },
            title = { Text(stringResourceCompat(R.string.delete_profile_title)) },
            text = {
                Text(
                    androidx.compose.ui.res.stringResource(
                        R.string.delete_profile_message,
                        deleteProfile.name,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(MainAction.DeleteProfile(deleteProfile.id))
                        deleteProfileId = null
                    },
                ) {
                    Text(
                        text = stringResourceCompat(R.string.delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteProfileId = null }) {
                    Text(stringResourceCompat(R.string.cancel))
                }
            },
        )
    }
    if (
        temperatureDialogProfileId == state.activeProfile.id &&
        state.activeProfile.mode == PressureMode.RUBY
    ) {
        TemperatureDialog(
            initialTemperatureK = state.activeProfile.temperatureK,
            onDismiss = { temperatureDialogProfileId = null },
            onApply = { temperatureK ->
                onAction(
                    MainAction.SetTemperature(
                        profileId = state.activeProfile.id,
                        temperatureK = temperatureK,
                    ),
                )
                temperatureDialogProfileId = null
            },
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val fontScale = LocalDensity.current.fontScale
        val layout = dacLayoutMetrics(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            fontScale = fontScale,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(layout.outerPadding),
        ) {
            ModeTabs(
                selectedMode = state.mode,
                onModeSelected = { onAction(MainAction.SelectMode(it)) },
                modifier = Modifier.height(layout.modeHeight),
            )
            key(state.mode) {
                Spacer(Modifier.height(layout.sectionGap))
                ProfileStrip(
                    profiles = state.profiles,
                    activeProfileId = state.activeProfile.id,
                    onSelect = { onAction(MainAction.SelectProfile(it)) },
                    onMove = { id, index ->
                        onAction(MainAction.MoveProfile(id, index))
                    },
                    onAdd = { profileDialog = ProfileDialogKind.Add },
                    modifier = Modifier.height(layout.profileHeight),
                )
                Spacer(Modifier.height(layout.sectionGap))
                ProfileToolbar(
                    profile = state.activeProfile,
                    onRename = { profileDialog = ProfileDialogKind.Rename },
                    onDelete = { deleteProfileId = state.activeProfile.id },
                )
                Spacer(Modifier.height(layout.sectionGap))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(layout.sectionGap))
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    DashboardCounters(
                        state = state,
                        onAction = onAction,
                        onTemperatureClick = {
                            temperatureDialogProfileId = state.activeProfile.id
                        },
                        layout = layout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(minOf(maxHeight, layout.dashboardMaxHeight)),
                    )
                }
                Spacer(Modifier.height(layout.sectionGap))
                ResultCard(
                    mode = state.mode,
                    pressure = state.pressure.result,
                    shiftCenti = state.pressure.shiftCenti,
                    temperatureK = state.activeProfile.temperatureK,
                    layout = layout,
                    saved = showSaved,
                    onSave = {
                        pendingSave = state.activeProfile.id to latestRecordId
                        onAction(MainAction.SaveHistory(state.activeProfile.id))
                    },
                    onHistory = onOpenHistory,
                )
                Spacer(Modifier.height(layout.resultBottomLift))
            }
        }
    }
}

@Composable
private fun ModeTabs(
    selectedMode: PressureMode,
    onModeSelected: (PressureMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PressureMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                val activeColors = if (mode == PressureMode.DIAMOND) {
                    listOf(Color(0xFF2980B9), Color(0xFF3498DB))
                } else {
                    listOf(Color(0xFFC0392B), Color(0xFFE74C3C))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.linearGradient(
                                if (isSelected) {
                                    activeColors
                                } else {
                                    listOf(Color.Transparent, Color.Transparent)
                                },
                            ),
                        )
                        .clickable(
                            role = Role.Tab,
                            onClick = { onModeSelected(mode) },
                        )
                        .semantics { selected = isSelected },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (mode == PressureMode.DIAMOND) {
                            stringResourceCompat(R.string.mode_diamond)
                        } else {
                            stringResourceCompat(R.string.mode_ruby)
                        },
                        color = if (isSelected) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileToolbar(
    profile: Profile,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = profile.name,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Row(
            modifier = Modifier
                .width(140.dp)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToolbarAction(
                label = stringResourceCompat(R.string.rename),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                onClick = onRename,
                modifier = Modifier.weight(1f),
            )
            ToolbarAction(
                label = stringResourceCompat(R.string.delete),
                color = MaterialTheme.colorScheme.error,
                onClick = onDelete,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToolbarAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = if (isPressed) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "toolbar-action-scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "toolbar-action-color",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            contentColor = color,
            border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DashboardCounters(
    state: MainUiState.Content,
    onAction: (MainAction) -> Unit,
    onTemperatureClick: () -> Unit,
    layout: DacLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (layout.compact) 6.dp else 8.dp),
    ) {
        if (layout.short) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ReferenceCounter(
                    state = state,
                    onAction = onAction,
                    onTemperatureClick = onTemperatureClick,
                    reserveActionHeader = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                MeasuredCounter(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                ReferenceCounter(
                    state = state,
                    onAction = onAction,
                    onTemperatureClick = onTemperatureClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Spacer(Modifier.height(if (layout.compact) 4.dp else 8.dp))
                MeasuredCounter(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxWidth(),
                )
            }
        }
        MiniHistoryPanel(
            records = state.recentRecords,
            modifier = Modifier
                .width(layout.recordsWidth)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun ReferenceCounter(
    state: MainUiState.Content,
    onAction: (MainAction) -> Unit,
    onTemperatureClick: () -> Unit,
    modifier: Modifier = Modifier,
    reserveActionHeader: Boolean = false,
) {
    val profile = state.activeProfile
    DigitCounter(
        title = if (state.mode == PressureMode.DIAMOND) {
            stringResourceCompat(R.string.reference_diamond)
        } else {
            stringResourceCompat(R.string.reference_ruby)
        },
        mode = state.mode,
        field = MeasurementField.REFERENCE,
        valueCenti = profile.referenceCenti,
        onAdjust = { delta ->
            onAction(
                MainAction.Adjust(
                    profileId = profile.id,
                    field = MeasurementField.REFERENCE,
                    deltaCenti = delta,
                ),
            )
        },
        modifier = modifier,
        headerAction = if (state.mode == PressureMode.RUBY) {
            {
                TemperatureHeaderAction(
                    temperatureK = profile.temperatureK,
                    onClick = onTemperatureClick,
                )
            }
        } else {
            null
        },
        reserveActionHeader = reserveActionHeader,
    )
}

@Composable
private fun TemperatureHeaderAction(
    temperatureK: Int,
    onClick: () -> Unit,
) {
    val atRoomTemperature = temperatureK == RubyTemperature.ROOM_K
    val accent = if (atRoomTemperature) Color(0xFF00A63D) else {
        MaterialTheme.colorScheme.primary
    }
    val textColor = if (atRoomTemperature) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    val description = androidx.compose.ui.res.stringResource(
        R.string.temperature_description,
        temperatureK,
    )
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(82.dp)
            .height(36.dp)
            .semantics { contentDescription = description },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = textColor,
        border = BorderStroke(1.dp, accent),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.temperature_button,
                    temperatureK,
                ),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TemperatureDialog(
    initialTemperatureK: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit,
) {
    var draft by rememberSaveable(
        initialTemperatureK,
        stateSaver = TextFieldValue.Saver,
    ) {
        val initialText = initialTemperatureK.toString()
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length),
            ),
        )
    }
    val temperatureK = draft.text.toIntOrNull()
    val isValid = temperatureK?.let(RubyTemperature::isValid) == true
    val focusManager = LocalFocusManager.current
    val kelvinUnit = stringResourceCompat(R.string.temperature_kelvin_unit)
    val unitColor = MaterialTheme.colorScheme.onSurfaceVariant
    val unitFontSize = MaterialTheme.typography.titleLarge.fontSize
    val temperatureVisualTransformation = remember(
        kelvinUnit,
        unitColor,
        unitFontSize,
    ) {
        VisualTransformation { text ->
            val originalLength = text.length
            TransformedText(
                text = buildAnnotatedString {
                    append(text)
                    append(' ')
                    withStyle(
                        SpanStyle(
                            color = unitColor,
                            fontSize = unitFontSize,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(kelvinUnit)
                    }
                },
                offsetMapping = object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int =
                        offset.coerceIn(0, originalLength)

                    override fun transformedToOriginal(offset: Int): Int =
                        offset.coerceIn(0, originalLength)
                },
            )
        }
    }

    fun step(delta: Int) {
        val current = temperatureK ?: initialTemperatureK
        val steppedText = (current + delta)
            .coerceIn(RubyTemperature.MIN_K, RubyTemperature.MAX_K)
            .toString()
        draft = TextFieldValue(
            text = steppedText,
            selection = TextRange(steppedText.length),
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = stringResourceCompat(R.string.temperature),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RepeatTemperatureButton(
                        label = stringResourceCompat(R.string.temperature_minus_symbol),
                        description = stringResourceCompat(R.string.temperature_decrease),
                        onStep = { step(-1) },
                    )
                    BasicTextField(
                        value = draft,
                        onValueChange = { candidate ->
                            if (
                                candidate.text.length <= 3 &&
                                candidate.text.all(Char::isDigit)
                            ) {
                                draft = candidate
                            }
                        },
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 124.dp, max = 160.dp)
                            .height(72.dp)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (draft.text.isNotEmpty() && !isValid) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                ),
                                RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 12.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = temperatureVisualTransformation,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isValid) {
                                    focusManager.clearFocus()
                                    onApply(requireNotNull(temperatureK))
                                }
                            },
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    innerTextField()
                                }
                            }
                        },
                    )
                    RepeatTemperatureButton(
                        label = stringResourceCompat(R.string.temperature_plus_symbol),
                        description = stringResourceCompat(R.string.temperature_increase),
                        onStep = { step(1) },
                    )
                }
                if (!isValid) {
                    Text(
                        text = stringResourceCompat(R.string.temperature_range_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(
                    onClick = {
                        val roomTemperature = RubyTemperature.ROOM_K.toString()
                        draft = TextFieldValue(
                            text = roomTemperature,
                            selection = TextRange(roomTemperature.length),
                        )
                        focusManager.clearFocus()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = stringResourceCompat(R.string.room_temperature),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (temperatureK != null) {
                            androidx.compose.ui.res.stringResource(
                                R.string.temperature_celsius,
                                RubyTemperature.toRoundedCelsius(temperatureK),
                            )
                        } else {
                            stringResourceCompat(R.string.temperature_celsius_unavailable)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) {
                        Text(stringResourceCompat(R.string.cancel))
                    }
                    Spacer(Modifier.width(6.dp))
                    Button(
                        enabled = isValid,
                        onClick = { onApply(requireNotNull(temperatureK)) },
                    ) {
                        Text(stringResourceCompat(R.string.apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun RepeatTemperatureButton(
    label: String,
    description: String,
    onStep: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnStep by androidx.compose.runtime.rememberUpdatedState(onStep)
    Surface(
        modifier = Modifier
            .size(64.dp)
            .semantics {
                role = Role.Button
                contentDescription = description
                onClick {
                    onStep()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            var repeated = false
                            kotlinx.coroutines.coroutineScope {
                                val repeatJob = launch {
                                    delay(600)
                                    repeated = true
                                    currentOnStep()
                                    while (true) {
                                        delay(120)
                                        currentOnStep()
                                    }
                                }
                                val released = tryAwaitRelease()
                                repeatJob.cancelAndJoin()
                                if (released && !repeated) currentOnStep()
                            }
                        } finally {
                            isPressed = false
                        }
                    },
                )
            },
        shape = RoundedCornerShape(14.dp),
        color = if (isPressed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun MeasuredCounter(
    state: MainUiState.Content,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = state.activeProfile
    DigitCounter(
        title = if (state.mode == PressureMode.DIAMOND) {
            stringResourceCompat(R.string.measured_diamond)
        } else {
            stringResourceCompat(R.string.measured_ruby)
        },
        mode = state.mode,
        field = MeasurementField.MEASURED,
        valueCenti = profile.measuredCenti,
        onAdjust = { delta ->
            onAction(
                MainAction.Adjust(
                    profileId = profile.id,
                    field = MeasurementField.MEASURED,
                    deltaCenti = delta,
                ),
            )
        },
        modifier = modifier,
        headerAction = {
            MeasuredResetAction(
                onClick = {
                    onAction(MainAction.Reset(profile.id))
                },
            )
        },
    )
}

@Composable
private fun MeasuredResetAction(
    onClick: () -> Unit,
) {
    val resetDescription = stringResourceCompat(R.string.reset_measured_description)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = if (isPressed) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "measured-reset-scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "measured-reset-color",
    )

    Box(
        modifier = Modifier
            .width(82.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = resetDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResourceCompat(R.string.reset_symbol),
                    fontSize = 17.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResourceCompat(R.string.reset),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ResultCard(
    mode: PressureMode,
    pressure: PressureResult,
    shiftCenti: Int,
    temperatureK: Int,
    layout: DacLayoutMetrics,
    saved: Boolean,
    onSave: () -> Unit,
    onHistory: () -> Unit,
) {
    val dense = layout.compact
    val veryDense = layout.short
    val pressureText = when (pressure) {
        is PressureResult.Valid ->
            "${formatCenti(pressure.pressureCenti)} GPa"
        else -> stringResourceCompat(R.string.out_of_range)
    }
    val calibrationText = when (pressure) {
        is PressureResult.OutOfRangeHigh ->
            stringResourceCompat(R.string.calibration_high)
        is PressureResult.OutOfRangeNegative ->
            stringResourceCompat(R.string.calibration_negative)
        else -> null
    }
    val shiftPrefix = if (shiftCenti > 0) "+" else ""
    val shiftText = "$shiftPrefix${formatCenti(shiftCenti)}"
    val showTemperature = mode == PressureMode.RUBY &&
        temperatureK != RubyTemperature.ROOM_K
    val resultHeight = layout.resultHeight
    val resultColors = if (mode == PressureMode.DIAMOND) {
        listOf(Color(0xFF2980B9), Color(0xFF3498DB))
    } else {
        listOf(Color(0xFFC0392B), Color(0xFFE74C3C))
    }
    val actionAccent = if (mode == PressureMode.DIAMOND) {
        Color(0xFF236F9D)
    } else {
        Color(0xFF9F3027)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(resultHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    resultColors,
                ),
            )
            .padding(
                horizontal = if (dense) 12.dp else 18.dp,
                vertical = if (dense && !veryDense) 6.dp else 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
                Text(
                    text = pressureText,
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = when {
                            pressure !is PressureResult.Valid ->
                                if (dense) 22.sp else 28.sp
                            veryDense -> 26.sp
                            dense -> 34.sp
                            else -> 44.sp
                        },
                        fontFamily = DacResultFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                        letterSpacing = (-0.35).sp,
                        lineHeight = if (dense) 36.sp else 46.sp,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (showTemperature) {
                    androidx.compose.ui.res.stringResource(
                        R.string.shift_with_temperature,
                        shiftText,
                        mode.unit,
                        temperatureK,
                    )
                } else {
                    androidx.compose.ui.res.stringResource(
                        R.string.shift,
                        shiftText,
                        mode.unit,
                    )
                },
                color = if (shiftCenti < 0) {
                    Color(0xFFFFCCCB)
                } else {
                    Color.White
                },
                fontSize = when {
                    veryDense -> 9.sp
                    dense -> 11.sp
                    else -> 13.sp
                },
                lineHeight = if (veryDense) 10.sp else 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (calibrationText != null) {
                Text(
                    text = calibrationText,
                    color = Color(0xFFFFF1B8),
                    fontSize = if (veryDense) 8.sp else 10.sp,
                    lineHeight = if (veryDense) 9.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (veryDense) {
            Row(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ResultActionButton(
                    label = stringResourceCompat(
                        if (saved) R.string.saved else R.string.save,
                    ),
                    primary = true,
                    enabled = pressure is PressureResult.Valid,
                    accent = actionAccent,
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                ResultActionButton(
                    label = stringResourceCompat(R.string.history),
                    primary = false,
                    enabled = true,
                    accent = actionAccent,
                    onClick = onHistory,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .width(if (dense) 88.dp else 100.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ResultActionButton(
                    label = stringResourceCompat(
                        if (saved) R.string.saved else R.string.save,
                    ),
                    primary = true,
                    enabled = pressure is PressureResult.Valid,
                    accent = actionAccent,
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                ResultActionButton(
                    label = stringResourceCompat(R.string.history),
                    primary = false,
                    enabled = true,
                    accent = actionAccent,
                    onClick = onHistory,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ResultActionButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = if (isPressed) {
            snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            )
        },
        label = "result-action-scale",
    )
    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled && primary && darkTheme ->
                MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            !enabled && primary -> Color.White.copy(alpha = 0.22f)
            primary && darkTheme && isPressed ->
                MaterialTheme.colorScheme.surfaceVariant
            primary && darkTheme -> MaterialTheme.colorScheme.surface
            primary && isPressed -> Color.White.copy(alpha = 0.88f)
            primary -> Color.White
            isPressed -> Color.White.copy(alpha = 0.12f)
            else -> Color.Transparent
        },
        label = "result-action-color",
    )
    val contentColor = when {
        !enabled -> Color.White.copy(alpha = 0.58f)
        primary && darkTheme -> MaterialTheme.colorScheme.onSurface
        primary -> accent
        else -> Color.White
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = RoundedCornerShape(8.dp),
            color = containerColor,
            contentColor = contentColor,
            border = if (primary && darkTheme) {
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.72f),
                )
            } else if (primary) {
                null
            } else {
                BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                ResultActionLabel(label)
            }
        }
    }
}

@Composable
private fun ResultActionLabel(label: String) {
    Crossfade(
        targetState = label,
        animationSpec = tween(durationMillis = 140),
        label = "result-action-label",
    ) { currentLabel ->
        Text(
            text = currentLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class ProfileDialogKind {
    Add,
    Rename,
}

@Composable
private fun ProfileNameDialog(
    kind: ProfileDialogKind,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by rememberSaveable(kind, initialName) { mutableStateOf(initialName) }
    val isValid = name.trim().isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (kind == ProfileDialogKind.Add) {
                    stringResourceCompat(R.string.new_experiment_name)
                } else {
                    stringResourceCompat(R.string.rename_profile)
                },
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                isError = !isValid,
                supportingText = {
                    if (!isValid) {
                        Text(stringResourceCompat(R.string.profile_name_required))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = isValid,
            ) {
                Text(
                    if (kind == ProfileDialogKind.Add) {
                        stringResourceCompat(R.string.create)
                    } else {
                        stringResourceCompat(R.string.save)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResourceCompat(R.string.cancel))
            }
        },
    )
}

@Composable
private fun LoadingContent(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(
    padding: PaddingValues,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResourceCompat(R.string.database_error),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(stringResourceCompat(R.string.retry))
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int): String =
    androidx.compose.ui.res.stringResource(id)

@Composable
internal fun uiMessageText(message: UiMessage): String = when (message) {
    UiMessage.KEEP_ONE_PROFILE -> stringResourceCompat(R.string.keep_one_profile)
    UiMessage.CANNOT_SAVE_OUT_OF_RANGE -> stringResourceCompat(R.string.cannot_save_range)
    UiMessage.DATABASE_ERROR -> stringResourceCompat(R.string.database_error)
    UiMessage.EXPORT_SUCCESS -> stringResourceCompat(R.string.export_success)
    UiMessage.EXPORT_FAILED -> stringResourceCompat(R.string.export_failed)
    UiMessage.SHARE_FAILED -> stringResourceCompat(R.string.share_failed)
}
