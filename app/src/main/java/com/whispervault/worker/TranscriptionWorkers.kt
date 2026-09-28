package com.whispervault.worker

/**
 * WorkManager / foreground-service orchestration for batch transcription.
 */
object TranscriptionWorkers {
    const val QUEUE_WORK_NAME = TranscriptionWorker.UNIQUE_WORK_NAME
}
