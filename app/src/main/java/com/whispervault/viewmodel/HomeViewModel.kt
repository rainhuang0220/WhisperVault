package com.whispervault.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.whispervault.data.AudioFile
import com.whispervault.storage.StorageGateway
import com.whispervault.transcription.TranscriptionEngine
import com.whispervault.transcription.TranscriptionException
import com.whispervault.transcription.WhisperModelSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class HomeUiState(
    val statusMessage: String = "Idle — select audio to begin",
    val selectedFiles: List<AudioFile> = emptyList(),
    val modelPath: String? = null,
    val modelLabel: String? = null,
    val modelReady: Boolean = false,
    val engineVersion: String = "",
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = TranscriptionEngine()

    private val _uiState = MutableStateFlow(
        HomeUiState(engineVersion = TranscriptionEngine.engineVersion()),
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
                    it.copy(
                        modelReady = false,
                        statusMessage = e.message ?: "Model load failed",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        modelReady = false,
                        statusMessage = "Model error: ${e.message}",
                    )
                }
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

    /** Exposed for the queue phase; keeps a single engine instance. */
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
