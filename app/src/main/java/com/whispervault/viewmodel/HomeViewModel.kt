package com.whispervault.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whispervault.WhisperVaultApp
import com.whispervault.data.AppDatabase
import com.whispervault.data.AudioFile
import com.whispervault.data.TaskStatus
import com.whispervault.data.TranscriptionQueueRepository
import com.whispervault.data.TranscriptionTaskEntity
import com.whispervault.storage.StorageGateway
import com.whispervault.storage.TranscriptExporter
import com.whispervault.storage.TranscriptFormatter
import com.whispervault.transcription.TranscriptionEngine
import com.whispervault.transcription.TranscriptionException
import com.whispervault.transcription.WhisperModelSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class HomeUiState(
    val statusMessage: String = "Idle — select a ggml model, then audio",
    val selectedFiles: List<AudioFile> = emptyList(),
    val modelPath: String? = null,
    val modelLabel: String? = null,
    val modelReady: Boolean = false,
    val engineVersion: String = "",
    val isEnqueueing: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TranscriptionEngine()
    private val queueRepository: TranscriptionQueueRepository =
        (application as WhisperVaultApp).queueRepository
    private val dao = AppDatabase.get(application).transcriptionTaskDao()

    private val _uiState = MutableStateFlow(
        HomeUiState(engineVersion = TranscriptionEngine.engineVersion()),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val tasks: StateFlow<List<TranscriptionTaskEntity>> = queueRepository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onAudioUrisSelected(uris: List<Uri>) {
        if (uris.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "No files selected") }
            return
        }
        val files = StorageGateway.resolveAudioFiles(getApplication(), uris)
        _uiState.update {
            it.copy(
                selectedFiles = files,
                statusMessage = when {
                    files.isEmpty() -> "No supported audio files in selection"
                    files.size > TranscriptionQueueRepository.MAX_QUEUE ->
                        "Selected ${files.size}; will enqueue first ${TranscriptionQueueRepository.MAX_QUEUE}"
                    else -> "Selected ${files.size} audio file(s)"
                },
            )
        }
    }

    fun onModelUriSelected(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(statusMessage = "No model selected") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Copying model into app storage…") }
            try {
                val localPath = withContext(Dispatchers.IO) {
                    copyModelToAppFiles(uri)
                }
                withContext(Dispatchers.IO) {
                    engine.loadModel(localPath)
                }
                val size = WhisperModelSize.fromFileName(File(localPath).name)
                _uiState.update {
                    it.copy(
                        modelPath = localPath,
                        modelLabel = size?.recommendedFileName ?: File(localPath).name,
                        modelReady = true,
                        statusMessage = "Model ready: ${File(localPath).name}",
                    )
                }
            } catch (e: TranscriptionException) {
                _uiState.update {
                    it.copy(modelReady = false, statusMessage = e.message ?: "Model load failed")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(modelReady = false, statusMessage = "Model error: ${e.message}")
                }
            }
        }
    }

    fun enqueueSelected() {
        val modelPath = _uiState.value.modelPath
        val files = _uiState.value.selectedFiles
        if (modelPath.isNullOrBlank()) {
            _uiState.update { it.copy(statusMessage = "Select a ggml model first") }
            return
        }
        if (files.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Select audio files first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isEnqueueing = true) }
            val count = withContext(Dispatchers.IO) {
                queueRepository.enqueue(files, modelPath)
            }
            _uiState.update {
                it.copy(
                    isEnqueueing = false,
                    selectedFiles = emptyList(),
                    statusMessage = "Enqueued $count file(s). Exports go to Download/WhisperVault/.",
                )
            }
        }
    }

    fun reExport(taskId: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val task = dao.getById(taskId) ?: return@withContext
                    if (task.status != TaskStatus.DONE || task.resultText.isNullOrBlank()) {
                        return@withContext
                    }
                    val segments = TranscriptFormatter.segmentsFromJson(task.segmentsJson)
                    val export = TranscriptExporter.exportAll(
                        context = getApplication(),
                        baseName = task.displayName,
                        text = task.resultText,
                        segments = segments,
                        modelPath = task.modelPath,
                    )
                    dao.updateExports(task.id, export.txtUri, export.jsonUri, export.srtUri)
                }
                _uiState.update {
                    it.copy(statusMessage = "Re-exported to ${TranscriptExporter.exportDirectoryLabel()}")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Export failed: ${e.message}") }
            }
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedFiles = emptyList(),
                statusMessage = if (it.modelReady) {
                    "Model ready — select audio files"
                } else {
                    "Idle — select a ggml model, then audio"
                },
            )
        }
    }

    fun transcriptionEngine(): TranscriptionEngine = engine

    override fun onCleared() {
        engine.release()
        super.onCleared()
    }

    private fun copyModelToAppFiles(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver
        val name = StorageGateway.resolveAudioFile(resolver, uri)?.displayName
            ?: "ggml-model.bin"
        val modelsDir = File(getApplication<Application>().filesDir, "models").apply { mkdirs() }
        val target = File(modelsDir, name)
        resolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw TranscriptionException.ModelMissing(uri.toString())
        return target.absolutePath
    }
}

fun TaskStatus.label(): String = when (this) {
    TaskStatus.WAITING -> "WAITING"
    TaskStatus.RUNNING -> "RUNNING"
    TaskStatus.DONE -> "DONE"
    TaskStatus.FAILED -> "FAILED"
}
