package com.whispervault.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.whispervault.R
import com.whispervault.data.AppDatabase
import com.whispervault.data.TaskStatus
import com.whispervault.transcription.TranscriptionEngine
import com.whispervault.transcription.TranscriptionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranscriptionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    private val dao = AppDatabase.get(appContext).transcriptionTaskDao()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        setForeground(createForegroundInfo("Preparing transcription…"))
        dao.resetRunningToWaiting()

        val engine = TranscriptionEngine()
        try {
            var processed = 0
            while (true) {
                val task = dao.getNextWithStatus(TaskStatus.WAITING) ?: break
                setForeground(createForegroundInfo("Transcribing: ${task.displayName}"))
                dao.updateStatus(task.id, TaskStatus.RUNNING, null, null)
                try {
                    if (!engine.isModelLoaded() || engine.currentModelPath() != task.modelPath) {
                        engine.loadModel(task.modelPath)
                    }
                    val result = engine.transcribeUri(
                        applicationContext,
                        Uri.parse(task.uriString),
                    )
                    dao.updateStatus(
                        id = task.id,
                        status = TaskStatus.DONE,
                        error = null,
                        resultText = result.text,
                    )
                } catch (oom: OutOfMemoryError) {
                    dao.updateStatus(
                        id = task.id,
                        status = TaskStatus.FAILED,
                        error = "Out of memory",
                        resultText = null,
                    )
                } catch (e: TranscriptionException) {
                    dao.updateStatus(
                        id = task.id,
                        status = TaskStatus.FAILED,
                        error = e.message,
                        resultText = null,
                    )
                } catch (e: Exception) {
                    dao.updateStatus(
                        id = task.id,
                        status = TaskStatus.FAILED,
                        error = e.message ?: e.javaClass.simpleName,
                        resultText = null,
                    )
                }
                processed++
                if (processed >= MAX_BATCH) {
                    // Re-enqueue so WorkManager can continue remaining work.
                    enqueue(applicationContext)
                    break
                }
            }
            Result.success()
        } finally {
            engine.release()
        }
    }

    private fun createForegroundInfo(content: String): ForegroundInfo {
        ensureChannel(applicationContext)
        val notification: Notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "whispervault_transcription_queue"
        private const val CHANNEL_ID = "transcription"
        private const val NOTIFICATION_ID = 42
        private const val MAX_BATCH = 100

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(UNIQUE_WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
