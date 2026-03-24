package com.example.usagehistory.data

import com.example.usagehistory.data.local.UsageSessionEntity
import java.time.Instant
import java.time.ZoneId

class WhatsappMessageReadResolver(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    fun resolve(
        pendingMessages: List<UsageSessionEntity>,
        allSessions: List<UsageSession>,
    ): List<UsageSessionEntity> {
        if (pendingMessages.isEmpty()) return emptyList()

        val whatsappSessions = allSessions
            .filter { it.packageName == WhatsappMessageTracker.WHATSAPP_PACKAGE }
            .sortedBy { it.startedAtEpochMillis }
            .toMutableList()
        if (whatsappSessions.isEmpty()) return emptyList()

        val resolvedMessages = mutableListOf<UsageSessionEntity>()
        var sessionIndex = 0

        pendingMessages.forEach { message ->
            val postedAt = message.notificationPostedAtEpochMillis ?: return@forEach
            while (
                sessionIndex < whatsappSessions.size &&
                whatsappSessions[sessionIndex].startedAtEpochMillis < postedAt
            ) {
                sessionIndex += 1
            }

            val matchingSession = whatsappSessions.getOrNull(sessionIndex) ?: return@forEach
            val readAt = matchingSession.startedAtEpochMillis
            if (readAt < postedAt) return@forEach

            resolvedMessages += message.copy(
                startedAtEpochMillis = readAt,
                endedAtEpochMillis = readAt,
                durationMillis = 0L,
                dayStartEpochMillis = dayStart(readAt),
                readInferredAtEpochMillis = readAt,
            )
        }

        return resolvedMessages
    }

    private fun dayStart(epochMillis: Long): Long {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
