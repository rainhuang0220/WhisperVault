package com.whispervault.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.whispervault.data.AudioFile
import com.whispervault.storage.StorageGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val statusMessage: String = "Idle — select audio to begin",
    val selectedFiles: List<AudioFile> = emptyList(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onAudioUrisSelected(uris: List<Uri>) {
        if (uris.isEmpty()) {
            _uiState.update {
                it.copy(statusMessage = "No files selected")
            }
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

    fun clearSelection() {
        _uiState.update {
            it.copy(
                selectedFiles = emptyList(),
                statusMessage = "Idle — select audio to begin",
            )
        }
    }
}
