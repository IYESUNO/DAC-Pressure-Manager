package com.iyes.dacpressuremanager.ui

import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iyes.dacpressuremanager.R
import com.iyes.dacpressuremanager.domain.PressureMode
import com.iyes.dacpressuremanager.domain.ThemePalette
import com.iyes.dacpressuremanager.domain.ThemeAppearance
import com.iyes.dacpressuremanager.domain.ThemePreferences
import com.iyes.dacpressuremanager.ui.theme.dacAccentColors

private enum class SettingsPage {
    MAIN,
    THEME_APPEARANCE,
    DIAMOND_COLORS,
    RUBY_COLORS,
    INSTRUCTIONS,
}

@Composable
fun SettingsDialog(
    themePreferences: ThemePreferences,
    updateState: UpdateUiState,
    versionName: String,
    onSelectPalette: (PressureMode, ThemePalette) -> Unit,
    onSelectAppearance: (ThemeAppearance) -> Unit,
    onResetPalettes: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenRelease: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.MAIN.name) }
    var showFullImage by rememberSaveable { mutableStateOf(false) }
    val page = SettingsPage.valueOf(pageName)

    BackHandler(enabled = page != SettingsPage.MAIN || showFullImage) {
        if (showFullImage) showFullImage = false else pageName = SettingsPage.MAIN.name
    }

    Dialog(
        onDismissRequest = {
            if (page == SettingsPage.MAIN) onDismiss() else pageName = SettingsPage.MAIN.name
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val panelHeight = minOf(maxHeight, 720.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .height(panelHeight),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                Column(Modifier.fillMaxSize()) {
                    SettingsHeader(
                        title = when (page) {
                            SettingsPage.MAIN -> stringResource(R.string.settings)
                            SettingsPage.THEME_APPEARANCE -> stringResource(R.string.theme)
                            SettingsPage.DIAMOND_COLORS -> stringResource(R.string.diamond_color)
                            SettingsPage.RUBY_COLORS -> stringResource(R.string.ruby_color)
                            SettingsPage.INSTRUCTIONS -> stringResource(R.string.instructions)
                        },
                        showBack = page != SettingsPage.MAIN,
                        onBack = { pageName = SettingsPage.MAIN.name },
                        onClose = onDismiss,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    when (page) {
                        SettingsPage.MAIN -> SettingsHome(
                            themePreferences = themePreferences,
                            updateState = updateState,
                            versionName = versionName,
                            onOpenDiamond = { pageName = SettingsPage.DIAMOND_COLORS.name },
                            onOpenRuby = { pageName = SettingsPage.RUBY_COLORS.name },
                            onOpenAppearance = { pageName = SettingsPage.THEME_APPEARANCE.name },
                            onResetPalettes = onResetPalettes,
                            onOpenInstructions = { pageName = SettingsPage.INSTRUCTIONS.name },
                            onCheckForUpdates = onCheckForUpdates,
                            onOpenRelease = onOpenRelease,
                        )
                        SettingsPage.THEME_APPEARANCE -> ThemeAppearancePage(
                            selected = themePreferences.appearance,
                            onSelect = onSelectAppearance,
                        )
                        SettingsPage.DIAMOND_COLORS -> PalettePage(
                            mode = PressureMode.DIAMOND,
                            selected = themePreferences.diamond,
                            onSelect = { onSelectPalette(PressureMode.DIAMOND, it) },
                        )
                        SettingsPage.RUBY_COLORS -> PalettePage(
                            mode = PressureMode.RUBY,
                            selected = themePreferences.ruby,
                            onSelect = { onSelectPalette(PressureMode.RUBY, it) },
                        )
                        SettingsPage.INSTRUCTIONS -> InstructionsPage(
                            onOpenFullImage = { showFullImage = true },
                        )
                    }
                }
            }
        }
    }

    if (showFullImage) {
        FullScreenInstructions(onDismiss = { showFullImage = false })
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Close, stringResource(R.string.close))
        }
    }
}

@Composable
private fun SettingsHome(
    themePreferences: ThemePreferences,
    updateState: UpdateUiState,
    versionName: String,
    onOpenDiamond: () -> Unit,
    onOpenRuby: () -> Unit,
    onOpenAppearance: () -> Unit,
    onResetPalettes: () -> Unit,
    onOpenInstructions: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenRelease: (String) -> Unit,
) {
    var showUpdateActions by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(updateState) {
        if (updateState is UpdateUiState.Available) showUpdateActions = true
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { SectionLabel(stringResource(R.string.appearance)) }
        item {
            SettingsRow(
                title = stringResource(R.string.theme),
                supporting = stringResource(themePreferences.appearance.labelRes),
                onClick = onOpenAppearance,
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.diamond_color),
                supporting = stringResource(themePreferences.diamond.labelRes),
                onClick = onOpenDiamond,
                leading = {
                    PaletteSwatch(PressureMode.DIAMOND, themePreferences.diamond, 36.dp)
                },
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.ruby_color),
                supporting = stringResource(themePreferences.ruby.labelRes),
                onClick = onOpenRuby,
                leading = { PaletteSwatch(PressureMode.RUBY, themePreferences.ruby, 36.dp) },
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.reset_colors),
                supporting = stringResource(R.string.reset_colors_description),
                onClick = onResetPalettes,
                showChevron = false,
            )
        }
        item {
            SectionLabel(
                text = stringResource(R.string.about),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.instructions),
                supporting = stringResource(R.string.instructions_description),
                onClick = onOpenInstructions,
            )
        }
        item {
            SettingsRow(
                title = stringResource(R.string.web_version),
                supporting = stringResource(R.string.web_version_url),
                onClick = { onOpenRelease(DAC_WEB_APP_URL) },
            )
        }
        item {
            VersionRow(
                updateState = updateState,
                versionName = versionName,
                showUpdateActions = showUpdateActions,
                onCheck = onCheckForUpdates,
                onOpenRelease = onOpenRelease,
                onLater = { showUpdateActions = false },
            )
        }
    }
}

@Composable
private fun ThemeAppearancePage(
    selected: ThemeAppearance,
    onSelect: (ThemeAppearance) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ThemeAppearance.entries, key = ThemeAppearance::name) { appearance ->
            val isSelected = appearance == selected
            val label = stringResource(appearance.labelRes)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .semantics {
                        this.selected = isSelected
                        role = Role.RadioButton
                    }
                    .clickable(role = Role.RadioButton) { onSelect(appearance) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun SettingsRow(
    title: String,
    supporting: String,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VersionRow(
    updateState: UpdateUiState,
    versionName: String,
    showUpdateActions: Boolean,
    onCheck: () -> Unit,
    onOpenRelease: (String) -> Unit,
    onLater: () -> Unit,
) {
    val status = when (updateState) {
        UpdateUiState.Idle -> stringResource(R.string.version_value, versionName)
        UpdateUiState.Checking -> stringResource(R.string.checking_for_updates)
        UpdateUiState.Current -> stringResource(R.string.no_updates_available)
        is UpdateUiState.Available -> stringResource(R.string.update_available, updateState.version)
        UpdateUiState.Failed -> stringResource(R.string.update_check_failed)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = updateState != UpdateUiState.Checking,
                role = Role.Button,
                onClick = onCheck,
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.version),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        status,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (
                            updateState == UpdateUiState.Idle ||
                            updateState == UpdateUiState.Checking
                        ) {
                            FontWeight.Normal
                        } else {
                            FontWeight.SemiBold
                        },
                        color = when (updateState) {
                            is UpdateUiState.Available -> MaterialTheme.colorScheme.primary
                            UpdateUiState.Failed -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                if (updateState == UpdateUiState.Checking) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.check_for_updates),
                    )
                }
            }
            if (updateState is UpdateUiState.Available && showUpdateActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onLater) { Text(stringResource(R.string.later)) }
                    Button(onClick = { onOpenRelease(updateState.releaseUrl) }) {
                        Text(stringResource(R.string.open_release))
                    }
                }
            }
        }
    }
}

@Composable
private fun PalettePage(
    mode: PressureMode,
    selected: ThemePalette,
    onSelect: (ThemePalette) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ThemePalette.entries, key = ThemePalette::name) { palette ->
            val isSelected = palette == selected
            val paletteName = stringResource(palette.labelRes)
            val semanticText = stringResource(
                if (isSelected) R.string.selected_color else R.string.select_color,
                paletteName,
            )
            Surface(
                modifier = Modifier
                    .height(88.dp)
                    .semantics {
                        this.selected = isSelected
                        contentDescription = semanticText
                        role = Role.RadioButton
                    }
                    .clickable(role = Role.RadioButton) { onSelect(palette) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        PaletteSwatch(mode, palette, 34.dp)
                        if (isSelected) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                .size(18.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        paletteName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteSwatch(mode: PressureMode, palette: ThemePalette, size: androidx.compose.ui.unit.Dp) {
    val light = dacAccentColors(mode, palette, darkTheme = false)
    val dark = dacAccentColors(mode, palette, darkTheme = true)
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)
    Canvas(Modifier.size(size)) {
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
        )
        drawArc(
            color = Color(0xFF111111),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
        )
        drawCircle(
            color = outline,
            style = Stroke(width = 1.dp.toPx()),
        )

        val themeCircleRadius = this.size.minDimension * 0.22f
        val centerX = this.size.width / 2f
        drawCircle(
            color = light.action,
            radius = themeCircleRadius,
            center = Offset(centerX, this.size.height * 0.27f),
        )
        drawCircle(
            color = dark.action,
            radius = themeCircleRadius,
            center = Offset(centerX, this.size.height * 0.73f),
        )
    }
}

private const val DAC_WEB_APP_URL = "https://apps.9527857.xyz/DAC-Pressure-Manager"

private val ThemeAppearance.labelRes: Int
    @StringRes get() = when (this) {
        ThemeAppearance.SYSTEM -> R.string.theme_follow_system
        ThemeAppearance.LIGHT -> R.string.theme_light
        ThemeAppearance.DARK -> R.string.theme_dark
    }

private val ThemePalette.labelRes: Int
    @StringRes get() = when (this) {
        ThemePalette.DEFAULT -> R.string.palette_default
        ThemePalette.GRAY -> R.string.palette_gray
        ThemePalette.MAUVE -> R.string.palette_mauve
        ThemePalette.SLATE -> R.string.palette_slate
        ThemePalette.SAGE -> R.string.palette_sage
        ThemePalette.OLIVE -> R.string.palette_olive
        ThemePalette.SAND -> R.string.palette_sand
        ThemePalette.TOMATO -> R.string.palette_tomato
        ThemePalette.RED -> R.string.palette_red
        ThemePalette.RUBY -> R.string.palette_ruby
        ThemePalette.CRIMSON -> R.string.palette_crimson
        ThemePalette.PINK -> R.string.palette_pink
        ThemePalette.PLUM -> R.string.palette_plum
        ThemePalette.PURPLE -> R.string.palette_purple
        ThemePalette.VIOLET -> R.string.palette_violet
        ThemePalette.IRIS -> R.string.palette_iris
        ThemePalette.INDIGO -> R.string.palette_indigo
        ThemePalette.BLUE -> R.string.palette_blue
        ThemePalette.CYAN -> R.string.palette_cyan
        ThemePalette.TEAL -> R.string.palette_teal
        ThemePalette.JADE -> R.string.palette_jade
        ThemePalette.GREEN -> R.string.palette_green
        ThemePalette.GRASS -> R.string.palette_grass
        ThemePalette.LIME -> R.string.palette_lime
        ThemePalette.MINT -> R.string.palette_mint
        ThemePalette.SKY -> R.string.palette_sky
        ThemePalette.GOLD -> R.string.palette_gold
        ThemePalette.BRONZE -> R.string.palette_bronze
        ThemePalette.BROWN -> R.string.palette_brown
        ThemePalette.ORANGE -> R.string.palette_orange
        ThemePalette.AMBER -> R.string.palette_amber
        ThemePalette.YELLOW -> R.string.palette_yellow
    }

@Composable
private fun InstructionsPage(onOpenFullImage: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.dac_help),
            contentDescription = stringResource(R.string.instructions),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable(onClick = onOpenFullImage)
                .padding(8.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            stringResource(R.string.view_full_size),
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FullScreenInstructions(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE000000))
                .clickable(onClick = onDismiss),
        ) {
            Image(
                painter = painterResource(R.drawable.dac_help),
                contentDescription = stringResource(R.string.instructions),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(48.dp)
                    .background(Color(0x88000000), CircleShape),
            ) {
                Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White)
            }
        }
    }
}
