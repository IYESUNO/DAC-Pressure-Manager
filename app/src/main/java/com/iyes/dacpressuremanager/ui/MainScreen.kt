package com.iyes.dacpressuremanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iyes.dacpressuremanager.R
import com.iyes.dacpressuremanager.domain.MeasurementField
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.PressureResult
import com.iyes.dacpressuremanager.domain.Profile
import com.iyes.dacpressuremanager.domain.formatCenti

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val dense = maxHeight < 650.dp || maxWidth < 380.dp
        val veryDense = maxHeight < 500.dp
        val outerPadding = if (dense) 8.dp else 10.dp
        val sectionGap = when {
            veryDense -> 4.dp
            dense -> 6.dp
            else -> 10.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding),
        ) {
            ModeTabs(
                selectedMode = state.mode,
                onModeSelected = { onAction(MainAction.SelectMode(it)) },
                modifier = Modifier.height(
                    when {
                        veryDense -> 34.dp
                        dense -> 40.dp
                        else -> 44.dp
                    },
                ),
            )
            Spacer(Modifier.height(sectionGap))
            ProfileStrip(
                profiles = state.profiles,
                activeProfileId = state.activeProfile.id,
                onSelect = { onAction(MainAction.SelectProfile(it)) },
                onMove = { id, index ->
                    onAction(MainAction.MoveProfile(id, index))
                },
                onAdd = { profileDialog = ProfileDialogKind.Add },
                modifier = Modifier.height(
                    when {
                        veryDense -> 38.dp
                        dense -> 44.dp
                        else -> 48.dp
                    },
                ),
            )
            Spacer(Modifier.height(sectionGap))
            ProfileToolbar(
                profile = state.activeProfile,
                dense = dense,
                veryDense = veryDense,
                onRename = { profileDialog = ProfileDialogKind.Rename },
                onDelete = { deleteProfileId = state.activeProfile.id },
                onSave = {
                    onAction(MainAction.SaveHistory(state.activeProfile.id))
                },
                onHistory = onOpenHistory,
            )
            Spacer(Modifier.height(sectionGap))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(sectionGap))
            DashboardCounters(
                state = state,
                onAction = onAction,
                dense = dense,
                veryDense = veryDense,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(sectionGap))
            ResultCard(
                mode = state.mode,
                pressure = state.pressure.result,
                shiftCenti = state.pressure.shiftCenti,
                dense = dense,
                veryDense = veryDense,
                onReset = {
                    onAction(MainAction.Reset(state.activeProfile.id))
                },
            )
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
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
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
    dense: Boolean,
    veryDense: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onHistory: () -> Unit,
) {
    val toolbarHeight = when {
        veryDense -> 52.dp
        dense -> 64.dp
        else -> 72.dp
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(toolbarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = profile.name,
            color = if (profile.mode == PressureMode.DIAMOND) {
                Color(0xFF2C3E50)
            } else {
                Color(0xFFC0392B)
            },
            fontSize = if (dense) 16.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        Column(
            modifier = Modifier
                .width(if (dense) 140.dp else 155.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ToolbarAction(
                    label = stringResourceCompat(R.string.rename),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ToolbarAction(
                    label = stringResourceCompat(R.string.save),
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                )
                ToolbarAction(
                    label = stringResourceCompat(R.string.history),
                    color = MaterialTheme.colorScheme.onSurface,
                    onClick = onHistory,
                    modifier = Modifier.weight(1f),
                )
            }
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
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = color,
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DashboardCounters(
    state: MainUiState.Content,
    onAction: (MainAction) -> Unit,
    dense: Boolean,
    veryDense: Boolean,
    modifier: Modifier = Modifier,
) {
    val profile = state.activeProfile
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (dense) 6.dp else 8.dp),
    ) {
        if (veryDense) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ReferenceCounter(
                    state = state,
                    onAction = onAction,
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
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Spacer(Modifier.height(if (dense) 4.dp else 8.dp))
                MeasuredCounter(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
        }
        MiniHistoryPanel(
            records = state.recentRecords,
            modifier = Modifier
                .width(if (dense) 68.dp else 76.dp)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun ReferenceCounter(
    state: MainUiState.Content,
    onAction: (MainAction) -> Unit,
    modifier: Modifier = Modifier,
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
    )
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
    )
}

@Composable
private fun ResultCard(
    mode: PressureMode,
    pressure: PressureResult,
    shiftCenti: Int,
    dense: Boolean,
    veryDense: Boolean,
    onReset: () -> Unit,
) {
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
    val resultHeight = when {
        veryDense -> 66.dp
        dense -> 82.dp
        else -> 100.dp
    }
    val resultColors = if (mode == PressureMode.DIAMOND) {
        listOf(Color(0xFF2980B9), Color(0xFF3498DB))
    } else {
        listOf(Color(0xFFC0392B), Color(0xFFE74C3C))
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
            .padding(horizontal = if (dense) 12.dp else 18.dp, vertical = 8.dp),
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
                fontSize = when {
                    pressure !is PressureResult.Valid -> if (dense) 22.sp else 28.sp
                    veryDense -> 26.sp
                    dense -> 34.sp
                    else -> 44.sp
                },
                fontWeight = FontWeight.ExtraBold,
                lineHeight = if (dense) 36.sp else 46.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = androidx.compose.ui.res.stringResource(
                    R.string.shift,
                    "$shiftPrefix${formatCenti(shiftCenti)}",
                    mode.unit,
                ),
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                modifier = Modifier
                    .size(if (veryDense) 38.dp else 46.dp)
                    .clickable(role = Role.Button, onClick = onReset)
                    .semantics { role = Role.Button },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.18f),
                contentColor = Color.White,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "↺",
                        fontSize = if (veryDense) 20.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Text(
                text = stringResourceCompat(R.string.reset),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = if (veryDense) 8.sp else 10.sp,
                lineHeight = if (veryDense) 9.sp else 12.sp,
                maxLines = 1,
            )
        }
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
    UiMessage.EXPORT_FAILED -> stringResourceCompat(R.string.export_failed)
    UiMessage.SHARE_FAILED -> stringResourceCompat(R.string.share_failed)
}
