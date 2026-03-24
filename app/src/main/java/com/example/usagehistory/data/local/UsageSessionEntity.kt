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
    val sessionSource: String = SESSION_SOURCE_USAGE_STATS,
    val contentType: String = CONTENT_TYPE_APP,
    val contentTitle: String? = null,
    val contentKey: String? = null,
    val notificationPostedAtEpochMillis: Long? = null,
    val readInferredAtEpochMillis: Long? = null,
) {
    companion object {
        const val SESSION_SOURCE_USAGE_STATS = "usage_stats"
        const val SESSION_SOURCE_MEDIA_SESSION = "media_session"
        const val SESSION_SOURCE_NOTIFICATION_LISTENER = "notification_listener"

        const val CONTENT_TYPE_APP = "app"
        const val CONTENT_TYPE_YOUTUBE_VIDEO = "youtube_video"
        const val CONTENT_TYPE_WHATSAPP_MESSAGE = "whatsapp_message"
    }
}
