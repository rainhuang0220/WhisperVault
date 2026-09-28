package com.whispervault.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HomeUiState(
    val statusMessage: String = "Idle — select audio to begin",
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onSelectAudioClicked() {
        // Real SAF picker arrives in a later phase.
        _uiState.update {
            it.copy(statusMessage = "Ready — audio picker will open in the next release step")
        }
    }
}
