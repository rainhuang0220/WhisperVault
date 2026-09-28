package com.whispervault.data

import android.content.Context
import com.whispervault.worker.TranscriptionWorker
import kotlinx.coroutines.flow.Flow

class TranscriptionQueueRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = AppDatabase.get(appContext).transcriptionTaskDao()

    fun observeTasks(): Flow<List<TranscriptionTaskEntity>> = dao.observeAll()

    suspend fun enqueue(files: List<AudioFile>, modelPath: String): Int {
        require(modelPath.isNotBlank()) { "Model path required" }
        if (files.isEmpty()) return 0
        val capped = files.take(MAX_QUEUE)
        val entities = capped.map { file ->
            TranscriptionTaskEntity(
                uriString = file.uri.toString(),
                displayName = file.displayName,
                mimeType = file.mimeType,
                sizeBytes = file.sizeBytes,
                modelPath = modelPath,
                status = TaskStatus.WAITING,
            )
        }
        dao.insertAll(entities)
        TranscriptionWorker.enqueue(appContext)
        return entities.size
    }

    suspend fun clearFinished() {
        val all = dao.getAll()
        // Keep unfinished; for simplicity clear only DONE/FAILED by rewriting unfinished.
        val unfinished = all.filter {
            it.status == TaskStatus.WAITING || it.status == TaskStatus.RUNNING
        }
        dao.clearAll()
        if (unfinished.isNotEmpty()) {
            dao.insertAll(unfinished.map { it.copy(id = 0) })
        }
    }

    suspend fun resumeIfNeeded() {
        if (dao.countUnfinished() > 0) {
            TranscriptionWorker.enqueue(appContext)
        }
    }

    companion object {
        const val MAX_QUEUE = 100
    }
}
