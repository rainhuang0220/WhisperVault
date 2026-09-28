package com.whispervault.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcription_tasks")
data class TranscriptionTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val modelPath: String,
    val status: TaskStatus = TaskStatus.WAITING,
    val errorMessage: String? = null,
    val resultText: String? = null,
    /** JSON array of whisper segments for SRT/JSON export. */
    val segmentsJson: String? = null,
    val exportTxtUri: String? = null,
    val exportJsonUri: String? = null,
    val exportSrtUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
