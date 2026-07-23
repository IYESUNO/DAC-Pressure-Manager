package com.iyes.dacpressuremanager.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.iyes.dacpressuremanager.R
import com.iyes.dacpressuremanager.domain.HistoryRecord
import com.iyes.dacpressuremanager.domain.Profile
import com.iyes.dacpressuremanager.domain.formatCenti
import com.iyes.dacpressuremanager.export.AndroidCsvExporter
import com.iyes.dacpressuremanager.export.CsvExporter
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HistoryDialog(
    state: HistoryUiState,
    onAction: (HistoryAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val content = state as? HistoryUiState.Content
    val message = content?.message
    val messageText = message?.let { uiMessageText(it) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(message, messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            onAction(HistoryAction.MessageShown)
        }
    }
    LaunchedEffect(content?.navigateBack) {
        if (content?.navigateBack == true) {
            onAction(HistoryAction.NavigationHandled)
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.72f)
                    .widthIn(max = 400.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                ) {
                    HistoryHeader(
                        content = content,
                        onExportError = {
                            onAction(HistoryAction.ReportMessage(it))
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    when (state) {
                        HistoryUiState.Loading -> HistoryLoading(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                        is HistoryUiState.Error -> HistoryError(
                            onRetry = { onAction(HistoryAction.Retry) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                        is HistoryUiState.Content -> HistoryBody(
                            state = state,
                            onAction = onAction,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showClearConfirmation = true },
                            enabled = content?.records?.isNotEmpty() == true,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.clear_all),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
            )
        }
    }

    if (showClearConfirmation && content != null) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.clear_history_message,
                        content.profile.name,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAction(HistoryAction.ClearHistory(content.profile.id))
                        showClearConfirmation = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.clear_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun HistoryHeader(
    content: HistoryUiState.Content?,
    onExportError: (UiMessage) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = remember { CsvExporter() }
    var exportMenuExpanded by remember { mutableStateOf(false) }
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null && content != null) {
            val document = exporter.build(
                profile = content.profile,
                records = content.records,
            )
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        AndroidCsvExporter.writeToUri(context, uri, document)
                    }
                }.onFailure {
                    onExportError(UiMessage.EXPORT_FAILED)
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.history_records),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (content != null) {
                Text(
                    text = content.profile.name,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box {
            Button(
                onClick = { exportMenuExpanded = true },
                enabled = content?.records?.isNotEmpty() == true,
            ) {
                Text(stringResource(R.string.export))
            }
            DropdownMenu(
                expanded = exportMenuExpanded,
                onDismissRequest = { exportMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.save_as)) },
                    onClick = {
                        exportMenuExpanded = false
                        if (content != null) {
                            val document = exporter.build(
                                profile = content.profile,
                                records = content.records,
                            )
                            saveLauncher.launch(document.fileName)
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    onClick = {
                        exportMenuExpanded = false
                        if (content != null) {
                            val document = exporter.build(
                                profile = content.profile,
                                records = content.records,
                            )
                            runCatching {
                                AndroidCsvExporter.share(context, document)
                            }.onFailure {
                                onExportError(UiMessage.SHARE_FAILED)
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryBody(
    state: HistoryUiState.Content,
    onAction: (HistoryAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.records.size) {
        if (state.records.isNotEmpty()) {
            listState.scrollToItem(state.records.lastIndex)
        }
    }

    Box(modifier = modifier) {
        if (state.records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_history),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = state.records,
                    key = { record -> record.id },
                ) { record ->
                    HistoryRecordRow(
                        profile = state.profile,
                        record = record,
                        onApply = {
                            onAction(HistoryAction.ApplyRecord(record.id))
                        },
                        onDelete = {
                            onAction(HistoryAction.DeleteRecord(record.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryRecordRow(
    profile: Profile,
    record: HistoryRecord,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    val timestamp = remember(record.createdAtEpochMillis) {
        DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.MEDIUM,
        ).format(Date(record.createdAtEpochMillis))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timestamp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = stringResource(
                        R.string.pressure_gpa,
                        formatCenti(record.pressureCenti),
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.history_values,
                        formatCenti(record.referenceCenti),
                        formatCenti(record.measuredCenti),
                        profile.mode.unit,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Button(
                    onClick = onApply,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 9.dp,
                        vertical = 4.dp,
                    ),
                ) {
                    Text(stringResource(R.string.apply))
                }
                Button(
                    onClick = onDelete,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical = 4.dp,
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun HistoryLoading(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun HistoryError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.database_error))
        Spacer(Modifier.height(10.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}
