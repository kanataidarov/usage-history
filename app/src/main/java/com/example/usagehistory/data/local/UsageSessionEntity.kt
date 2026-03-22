package com.example.usagehistory.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_sessions",
    indices = [
        Index(value = ["dayStartEpochMillis"]),
        Index(value = ["packageName"]),
        Index(value = ["startedAtEpochMillis"]),
    ],
)
data class UsageSessionEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val appLabel: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
    val dayStartEpochMillis: Long,
)
