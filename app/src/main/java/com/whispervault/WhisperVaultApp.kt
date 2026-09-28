package com.whispervault

import android.app.Application
import androidx.work.Configuration
import com.whispervault.data.TranscriptionQueueRepository
import com.whispervault.worker.TranscriptionWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WhisperVaultApp : Application(), Configuration.Provider {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var queueRepository: TranscriptionQueueRepository
        private set

    override fun onCreate() {
        super.onCreate()
        TranscriptionWorker.ensureChannel(this)
        queueRepository = TranscriptionQueueRepository(this)
        appScope.launch {
            queueRepository.resumeIfNeeded()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
