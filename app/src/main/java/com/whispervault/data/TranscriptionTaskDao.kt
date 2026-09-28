package com.whispervault.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionTaskDao {
    @Query("SELECT * FROM transcription_tasks ORDER BY id ASC")
    fun observeAll(): Flow<List<TranscriptionTaskEntity>>

    @Query("SELECT * FROM transcription_tasks ORDER BY id ASC")
    suspend fun getAll(): List<TranscriptionTaskEntity>

    @Query("SELECT * FROM transcription_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TranscriptionTaskEntity?

    @Query("SELECT * FROM transcription_tasks WHERE status = :status ORDER BY id ASC LIMIT 1")
    suspend fun getNextWithStatus(status: TaskStatus): TranscriptionTaskEntity?

    @Query("SELECT COUNT(*) FROM transcription_tasks WHERE status IN ('WAITING', 'RUNNING')")
    suspend fun countUnfinished(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TranscriptionTaskEntity>): List<Long>

    @Query(
        """
        UPDATE transcription_tasks
        SET status = :status,
            errorMessage = :error,
            resultText = :resultText,
            segmentsJson = :segmentsJson,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateStatus(
        id: Long,
        status: TaskStatus,
        error: String?,
        resultText: String?,
        segmentsJson: String? = null,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE transcription_tasks
        SET exportTxtUri = :txtUri,
            exportJsonUri = :jsonUri,
            exportSrtUri = :srtUri,
            updatedAt = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun updateExports(
        id: Long,
        txtUri: String?,
        jsonUri: String?,
        srtUri: String?,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE transcription_tasks SET status = 'WAITING', errorMessage = NULL, updatedAt = :updatedAt WHERE status = 'RUNNING'")
    suspend fun resetRunningToWaiting(updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM transcription_tasks")
    suspend fun clearAll()
}
