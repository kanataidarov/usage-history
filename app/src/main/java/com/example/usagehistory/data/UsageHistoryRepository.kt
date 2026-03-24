package com.example.usagehistory.data

import android.content.Context
import com.example.usagehistory.data.local.AppDatabase
import com.example.usagehistory.data.local.UsageSessionEntity
import java.time.LocalDate
import java.time.ZoneId

class UsageHistoryRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val usageEventReader: UsageEventReader,
    private val packageMetadataResolver: PackageMetadataResolver,
    private val sessionBuilder: SessionBuilder = SessionBuilder(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun refreshDay(date: LocalDate): List<UsageSessionEntity> {
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayKey = startMillis
        val usageSessionDao = database.usageSessionDao()
        val detailedYoutubeSessions =
            usageSessionDao.getSessionsForDayAndSource(
                dayStartEpochMillis = dayKey,
                sessionSource = UsageSessionEntity.SESSION_SOURCE_MEDIA_SESSION,
                packageName = YoutubeSessionTracker.YOUTUBE_PACKAGE,
            )

        val sessions = sessionBuilder.build(
            transitions = usageEventReader.readTransitions(startMillis, endMillis),
            queryEndMillis = endMillis.coerceAtMost(System.currentTimeMillis()),
            ignoredPackages = setOf(context.packageName),
        )
            .filter { session ->
                packageMetadataResolver.shouldTrackInTimeline(session.packageName) &&
                    !(
                        session.packageName == YoutubeSessionTracker.YOUTUBE_PACKAGE &&
                            detailedYoutubeSessions.any { detailed ->
                                session.startedAtEpochMillis < detailed.endedAtEpochMillis &&
                                    session.endedAtEpochMillis > detailed.startedAtEpochMillis
                            }
                    )
            }

        val entities = sessions.map { session ->
            val appLabel = packageMetadataResolver.resolveLabel(session.packageName)
            UsageSessionEntity(
                id = "${session.packageName}_${session.startedAtEpochMillis}",
                packageName = session.packageName,
                appLabel = appLabel,
                startedAtEpochMillis = session.startedAtEpochMillis,
                endedAtEpochMillis = session.endedAtEpochMillis,
                durationMillis = session.durationMillis,
                dayStartEpochMillis = dayKey,
                sessionSource = UsageSessionEntity.SESSION_SOURCE_USAGE_STATS,
                contentType = UsageSessionEntity.CONTENT_TYPE_APP,
            )
        }

        usageSessionDao.replaceDayForSource(
            dayStartEpochMillis = dayKey,
            sessionSource = UsageSessionEntity.SESSION_SOURCE_USAGE_STATS,
            sessions = entities,
        )
        return usageSessionDao.getSessionsForDay(dayKey)
    }

    suspend fun loadCachedDay(date: LocalDate): List<UsageSessionEntity> {
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        return database.usageSessionDao().getSessionsForDay(startMillis)
    }

    fun hasUsageAccess(): Boolean = UsageAccessManager.hasUsageAccess(context)

    fun hasNotificationAccess(): Boolean = NotificationAccessManager.hasNotificationAccess(context)
}
