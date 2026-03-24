package com.example.usagehistory.data

import android.content.Context
import com.example.usagehistory.data.local.AppDatabase
import com.example.usagehistory.data.local.UsageSessionDao
import com.example.usagehistory.data.local.UsageSessionEntity
import java.time.LocalDate
import java.time.ZoneId

class UsageHistoryRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val usageEventReader: UsageEventReader,
    private val packageMetadataResolver: PackageMetadataResolver,
    private val sessionBuilder: SessionBuilder = SessionBuilder(),
    private val whatsappMessageReadResolver: WhatsappMessageReadResolver = WhatsappMessageReadResolver(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun refreshDay(date: LocalDate): List<UsageSessionEntity> {
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayKey = startMillis
        val usageSessionDao = database.usageSessionDao()
        val pendingWhatsappMessages =
            usageSessionDao.getPendingMessageSessionsBefore(
                packageName = WhatsappMessageTracker.WHATSAPP_PACKAGE,
                beforeEpochMillis = endMillis,
            )
        val earliestPendingMessageAt = pendingWhatsappMessages.firstOrNull()?.notificationPostedAtEpochMillis
        val readStartMillis = minOf(startMillis, earliestPendingMessageAt ?: startMillis)
        val detailedYoutubeSessions =
            usageSessionDao.getSessionsForDayAndSource(
                dayStartEpochMillis = dayKey,
                sessionSource = UsageSessionEntity.SESSION_SOURCE_MEDIA_SESSION,
                packageName = YoutubeSessionTracker.YOUTUBE_PACKAGE,
            )
        val allSessions = sessionBuilder.build(
            transitions = usageEventReader.readTransitions(readStartMillis, endMillis),
            queryEndMillis = endMillis.coerceAtMost(System.currentTimeMillis()),
            ignoredPackages = setOf(context.packageName),
        )
        resolveWhatsappMessages(
            pendingMessages = pendingWhatsappMessages,
            allSessions = allSessions,
            usageSessionDao = usageSessionDao,
        )
        val detailedWhatsappSessions =
            usageSessionDao.getSessionsForDayAndSource(
                dayStartEpochMillis = dayKey,
                sessionSource = UsageSessionEntity.SESSION_SOURCE_NOTIFICATION_LISTENER,
                packageName = WhatsappMessageTracker.WHATSAPP_PACKAGE,
            ).filter { it.readInferredAtEpochMillis != null }

        val sessions = allSessions
            .filter { session ->
                session.startedAtEpochMillis in startMillis until endMillis
            }
            .filter { session ->
                packageMetadataResolver.shouldTrackInTimeline(session.packageName) &&
                    !(
                        session.packageName == YoutubeSessionTracker.YOUTUBE_PACKAGE &&
                            detailedYoutubeSessions.any { detailed ->
                                session.startedAtEpochMillis < detailed.endedAtEpochMillis &&
                                    session.endedAtEpochMillis > detailed.startedAtEpochMillis
                            }
                    )
                    &&
                    !(
                        session.packageName == WhatsappMessageTracker.WHATSAPP_PACKAGE &&
                            detailedWhatsappSessions.any { detailed ->
                                val readAt = detailed.readInferredAtEpochMillis ?: detailed.startedAtEpochMillis
                                readAt in session.startedAtEpochMillis..session.endedAtEpochMillis
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

    private suspend fun resolveWhatsappMessages(
        pendingMessages: List<UsageSessionEntity>,
        allSessions: List<UsageSession>,
        usageSessionDao: UsageSessionDao,
    ) {
        val resolvedMessages = whatsappMessageReadResolver.resolve(
            pendingMessages = pendingMessages,
            allSessions = allSessions,
        )

        if (resolvedMessages.isNotEmpty()) {
            usageSessionDao.insertAll(resolvedMessages)
        }
    }
}
