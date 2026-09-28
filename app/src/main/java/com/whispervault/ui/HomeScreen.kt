package com.whispervault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whispervault.R
import com.whispervault.data.AudioFile
import com.whispervault.data.TranscriptionTaskEntity
import com.whispervault.viewmodel.HomeViewModel
import com.whispervault.viewmodel.label

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSelectAudio: () -> Unit,
    onSelectModel: () -> Unit,
    onStartTranscription: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.privacy_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Engine: ${uiState.engineVersion.ifBlank { "…" }} · ABI arm64-v8a",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onSelectModel, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.select_model))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (uiState.modelReady) {
                "Model: ${uiState.modelLabel}"
            } else {
                stringResource(R.string.model_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onSelectAudio, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.select_audio))
        }
        if (uiState.selectedFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onStartTranscription,
                enabled = uiState.modelReady && !uiState.isEnqueueing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.start_transcription))
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { viewModel.clearSelection() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.clear_selection))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Status: ${uiState.statusMessage}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (uiState.selectedFiles.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.selected_files_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(uiState.selectedFiles, key = { "sel-${it.uri}" }) { file ->
                    SelectedAudioRow(file)
                    HorizontalDivider()
                }
            }
            if (tasks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.queue_header),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(tasks, key = { "task-${it.id}" }) { task ->
                    TaskRow(task)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SelectedAudioRow(file: AudioFile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = file.displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatSize(file.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = formatDuration(file.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun TaskRow(task: TranscriptionTaskEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = task.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = task.status.label(),
                style = MaterialTheme.typography.labelLarge,
                color = when (task.status.name) {
                    "DONE" -> MaterialTheme.colorScheme.primary
                    "FAILED" -> MaterialTheme.colorScheme.error
                    "RUNNING" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onBackground
                },
            )
        }
        task.errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 0) return "size unknown"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}

private fun formatDuration(durationMs: Long?): String {
    if (durationMs == null || durationMs < 0) return "duration n/a"
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
