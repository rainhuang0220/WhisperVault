package com.whispervault.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.whispervault.data.AudioFile
import com.whispervault.ui.theme.WhisperVaultTheme
import com.whispervault.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    private val audioPicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        homeViewModel.onAudioUrisSelected(uris)
    }

    private val modelPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        homeViewModel.onModelUriSelected(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhisperVaultTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onSelectAudio = {
                            audioPicker.launch(AudioFile.PICKER_MIME_TYPES)
                        },
                        onSelectModel = {
                            modelPicker.launch(
                                arrayOf("application/octet-stream", "*/*"),
                            )
                        },
                    )
                }
            }
        }
    }
}
