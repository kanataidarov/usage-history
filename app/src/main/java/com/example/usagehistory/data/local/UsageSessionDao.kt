package com.example.usagehistory.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UsageSessionDao {
    @Query(
        """
        SELECT * FROM usage_sessions
        WHERE dayStartEpochMillis = :dayStartEpochMillis
        ORDER BY startedAtEpochMillis DESC
        """,
    )
    suspend fun getSessionsForDay(dayStartEpochMillis: Long): List<UsageSessionEntity>

    @Query("DELETE FROM usage_sessions WHERE dayStartEpochMillis = :dayStartEpochMillis")
    suspend fun deleteSessionsForDay(dayStartEpochMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<UsageSessionEntity>)

    @Transaction
    suspend fun replaceDay(dayStartEpochMillis: Long, sessions: List<UsageSessionEntity>) {
        deleteSessionsForDay(dayStartEpochMillis)
        insertAll(sessions)
    }
}
