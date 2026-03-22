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

        val sessions = sessionBuilder.build(
            transitions = usageEventReader.readTransitions(startMillis, endMillis),
            queryEndMillis = endMillis.coerceAtMost(System.currentTimeMillis()),
            ignoredPackages = setOf(context.packageName),
        )

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
            )
        }

        database.usageSessionDao().replaceDay(dayKey, entities)
        return database.usageSessionDao().getSessionsForDay(dayKey)
    }

    suspend fun loadCachedDay(date: LocalDate): List<UsageSessionEntity> {
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        return database.usageSessionDao().getSessionsForDay(startMillis)
    }

    fun hasUsageAccess(): Boolean = UsageAccessManager.hasUsageAccess(context)
}
