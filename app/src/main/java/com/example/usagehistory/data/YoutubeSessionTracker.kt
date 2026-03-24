package com.example.usagehistory.data

import android.app.Notification
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import com.example.usagehistory.data.local.UsageSessionDao
import com.example.usagehistory.data.local.UsageSessionEntity
import java.time.Instant
import java.time.ZoneId

class YoutubeSessionTracker(
    private val usageSessionDao: UsageSessionDao,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    private var isPlaybackActive = false
    private var pendingTitle: String? = null
    private var activeSessionTitle: String? = null
    private var activeSessionStartedAt: Long? = null

    suspend fun onControllerChanged(controller: MediaController?) {
        val now = nowProvider()
        if (controller?.packageName != YOUTUBE_PACKAGE) {
            closeActiveSession(now)
            pendingTitle = null
            isPlaybackActive = false
            return
        }

        pendingTitle = controller.metadata.extractTitle()
        val playbackActive = controller.playbackState.isPlaybackActive()
        updatePlaybackState(playbackActive, now)
        reconcileTitle(now)
    }

    suspend fun onMetadataChanged(metadata: MediaMetadata?) {
        pendingTitle = metadata.extractTitle()
        reconcileTitle(nowProvider())
    }

    suspend fun onPlaybackStateChanged(playbackState: PlaybackState?) {
        updatePlaybackState(playbackState.isPlaybackActive(), nowProvider())
    }

    suspend fun onSessionEnded() {
        closeActiveSession(nowProvider())
        pendingTitle = null
        isPlaybackActive = false
    }

    suspend fun onNotificationChanged(notification: Notification?) {
        val notificationTitle = notification?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?.trim()
            ?.takeIf(String::isNotBlank)
        if (notificationTitle != null && notificationTitle != pendingTitle) {
            pendingTitle = notificationTitle
            reconcileTitle(nowProvider())
        }
    }

    private suspend fun updatePlaybackState(active: Boolean, now: Long) {
        val previousPlaybackState = isPlaybackActive
        isPlaybackActive = active

        when {
            active && !previousPlaybackState -> maybeStartActiveSession(now)
            !active && previousPlaybackState -> closeActiveSession(now)
        }
    }

    private suspend fun reconcileTitle(now: Long) {
        val title = pendingTitle
        val currentTitle = activeSessionTitle

        when {
            currentTitle != null && currentTitle != title -> {
                closeActiveSession(now)
                maybeStartActiveSession(now)
            }
            currentTitle == null -> maybeStartActiveSession(now)
        }
    }

    private fun maybeStartActiveSession(now: Long) {
        val title = pendingTitle ?: return
        if (!isPlaybackActive || activeSessionStartedAt != null) return
        activeSessionTitle = title
        activeSessionStartedAt = now
    }

    private suspend fun closeActiveSession(endedAtEpochMillis: Long) {
        val startedAtEpochMillis = activeSessionStartedAt
        val title = activeSessionTitle
        activeSessionStartedAt = null
        activeSessionTitle = null

        if (startedAtEpochMillis == null || title.isNullOrBlank()) return
        if (endedAtEpochMillis <= startedAtEpochMillis) return

        persistSegments(
            title = title,
            startedAtEpochMillis = startedAtEpochMillis,
            endedAtEpochMillis = endedAtEpochMillis,
        )
    }

    private suspend fun persistSegments(
        title: String,
        startedAtEpochMillis: Long,
        endedAtEpochMillis: Long,
    ) {
        var segmentStart = startedAtEpochMillis

        while (segmentStart < endedAtEpochMillis) {
            val dayStart = dayStart(segmentStart)
            val nextDayStart = dayStart(segmentStart, dayOffset = 1)
            val segmentEnd = minOf(endedAtEpochMillis, nextDayStart)

            if (segmentEnd > segmentStart) {
                usageSessionDao.insert(
                    UsageSessionEntity(
                        id = "youtube_${segmentStart}_${title.hashCode()}",
                        packageName = YOUTUBE_PACKAGE,
                        appLabel = YOUTUBE_LABEL,
                        startedAtEpochMillis = segmentStart,
                        endedAtEpochMillis = segmentEnd,
                        durationMillis = segmentEnd - segmentStart,
                        dayStartEpochMillis = dayStart,
                        sessionSource = UsageSessionEntity.SESSION_SOURCE_MEDIA_SESSION,
                        contentType = UsageSessionEntity.CONTENT_TYPE_YOUTUBE_VIDEO,
                        contentTitle = title,
                    ),
                )
            }

            segmentStart = segmentEnd
        }
    }

    private fun dayStart(epochMillis: Long, dayOffset: Long = 0): Long {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()
            .plusDays(dayOffset)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun MediaMetadata?.extractTitle(): String? {
        return this?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: this?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)?.toString()
                ?.trim()
                ?.takeIf(String::isNotBlank)
    }

    private fun PlaybackState?.isPlaybackActive(): Boolean {
        return when (this?.state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
                -> true

            else -> false
        }
    }

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        private const val YOUTUBE_LABEL = "YouTube"
    }
}
