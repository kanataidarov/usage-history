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

    @Query(
        """
        DELETE FROM usage_sessions
        WHERE dayStartEpochMillis = :dayStartEpochMillis
        AND sessionSource = :sessionSource
        """,
    )
    suspend fun deleteSessionsForDayAndSource(dayStartEpochMillis: Long, sessionSource: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<UsageSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: UsageSessionEntity)

    @Query(
        """
        SELECT * FROM usage_sessions
        WHERE dayStartEpochMillis = :dayStartEpochMillis
        AND sessionSource = :sessionSource
        AND packageName = :packageName
        """,
    )
    suspend fun getSessionsForDayAndSource(
        dayStartEpochMillis: Long,
        sessionSource: String,
        packageName: String,
    ): List<UsageSessionEntity>

    @Transaction
    suspend fun replaceDayForSource(
        dayStartEpochMillis: Long,
        sessionSource: String,
        sessions: List<UsageSessionEntity>,
    ) {
        deleteSessionsForDayAndSource(dayStartEpochMillis, sessionSource)
        if (sessions.isNotEmpty()) {
            insertAll(sessions)
        }
    }
}
