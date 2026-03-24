package com.example.usagehistory.data

import com.example.usagehistory.data.local.UsageSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsappMessageReadResolverTest {
    private val zoneId = ZoneId.of("UTC")
    private val resolver = WhatsappMessageReadResolver(zoneId = zoneId)

    @Test
    fun `resolves multiple unread messages to same next whatsapp open`() {
        val openAt = epochMillis(day = 24, hour = 14, minute = 32)
        val resolved = resolver.resolve(
            pendingMessages = listOf(
                pendingMessage(id = "a", postedAt = epochMillis(day = 24, hour = 14, minute = 10)),
                pendingMessage(id = "b", postedAt = epochMillis(day = 24, hour = 14, minute = 20)),
            ),
            allSessions = listOf(
                UsageSession(
                    packageName = WhatsappMessageTracker.WHATSAPP_PACKAGE,
                    startedAtEpochMillis = openAt,
                    endedAtEpochMillis = openAt + 60_000,
                ),
            ),
        )

        assertEquals(listOf(openAt, openAt), resolved.mapNotNull { it.readInferredAtEpochMillis })
        assertEquals(listOf(openAt, openAt), resolved.map { it.startedAtEpochMillis })
    }

    @Test
    fun `message arriving during active whatsapp session waits for next open`() {
        val firstOpenAt = epochMillis(day = 24, hour = 14, minute = 0)
        val secondOpenAt = epochMillis(day = 24, hour = 15, minute = 0)
        val resolved = resolver.resolve(
            pendingMessages = listOf(
                pendingMessage(id = "c", postedAt = epochMillis(day = 24, hour = 14, minute = 15)),
            ),
            allSessions = listOf(
                UsageSession(
                    packageName = WhatsappMessageTracker.WHATSAPP_PACKAGE,
                    startedAtEpochMillis = firstOpenAt,
                    endedAtEpochMillis = firstOpenAt + 30 * 60_000,
                ),
                UsageSession(
                    packageName = WhatsappMessageTracker.WHATSAPP_PACKAGE,
                    startedAtEpochMillis = secondOpenAt,
                    endedAtEpochMillis = secondOpenAt + 30 * 60_000,
                ),
            ),
        )

        assertEquals(secondOpenAt, resolved.single().readInferredAtEpochMillis)
    }

    @Test
    fun `resolved message moves to read day when whatsapp is opened after midnight`() {
        val postedAt = epochMillis(day = 24, hour = 23, minute = 58)
        val openAt = epochMillis(day = 25, hour = 0, minute = 5)
        val resolved = resolver.resolve(
            pendingMessages = listOf(
                pendingMessage(id = "d", postedAt = postedAt),
            ),
            allSessions = listOf(
                UsageSession(
                    packageName = WhatsappMessageTracker.WHATSAPP_PACKAGE,
                    startedAtEpochMillis = openAt,
                    endedAtEpochMillis = openAt + 60_000,
                ),
            ),
        ).single()

        assertEquals(openAt, resolved.readInferredAtEpochMillis)
        assertEquals(epochMillis(day = 25, hour = 0, minute = 0), resolved.dayStartEpochMillis)
    }

    @Test
    fun `leaves message unresolved when whatsapp is never reopened`() {
        val resolved = resolver.resolve(
            pendingMessages = listOf(
                pendingMessage(id = "e", postedAt = epochMillis(day = 24, hour = 14, minute = 10)),
            ),
            allSessions = emptyList(),
        )

        assertEquals(0, resolved.size)
    }

    private fun pendingMessage(id: String, postedAt: Long): UsageSessionEntity {
        return UsageSessionEntity(
            id = id,
            packageName = WhatsappMessageTracker.WHATSAPP_PACKAGE,
            appLabel = "WhatsApp",
            startedAtEpochMillis = postedAt,
            endedAtEpochMillis = postedAt,
            durationMillis = 0L,
            dayStartEpochMillis = dayStart(postedAt),
            sessionSource = UsageSessionEntity.SESSION_SOURCE_NOTIFICATION_LISTENER,
            contentType = UsageSessionEntity.CONTENT_TYPE_WHATSAPP_MESSAGE,
            contentTitle = "Incoming message",
            contentKey = id,
            notificationPostedAtEpochMillis = postedAt,
            readInferredAtEpochMillis = null,
        )
    }

    private fun dayStart(epochMillis: Long): Long {
        return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), zoneId)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private fun epochMillis(day: Int, hour: Int, minute: Int): Long {
        return java.time.LocalDateTime.of(2026, 3, day, hour, minute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
