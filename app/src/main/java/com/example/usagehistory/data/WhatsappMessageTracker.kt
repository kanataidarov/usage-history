package com.example.usagehistory.data

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.example.usagehistory.data.local.UsageSessionDao
import com.example.usagehistory.data.local.UsageSessionEntity
import java.time.Instant
import java.time.ZoneId

class WhatsappMessageTracker(
    private val usageSessionDao: UsageSessionDao,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    suspend fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        if (statusBarNotification.packageName != WHATSAPP_PACKAGE) return

        val notification = statusBarNotification.notification ?: return
        if (!notification.shouldTrackWhatsappMessage()) return

        val postedAtEpochMillis = statusBarNotification.postTime.takeIf { it > 0 } ?: nowProvider()
        val title = notification.extractConversationTitle().orEmpty().ifBlank { DEFAULT_MESSAGE_TITLE }
        val contentKey = buildContentKey(statusBarNotification, notification, postedAtEpochMillis)

        usageSessionDao.insert(
            UsageSessionEntity(
                id = "whatsapp_$contentKey",
                packageName = WHATSAPP_PACKAGE,
                appLabel = WHATSAPP_LABEL,
                startedAtEpochMillis = postedAtEpochMillis,
                endedAtEpochMillis = postedAtEpochMillis,
                durationMillis = 0L,
                dayStartEpochMillis = dayStart(postedAtEpochMillis),
                sessionSource = UsageSessionEntity.SESSION_SOURCE_NOTIFICATION_LISTENER,
                contentType = UsageSessionEntity.CONTENT_TYPE_WHATSAPP_MESSAGE,
                contentTitle = title,
                contentKey = contentKey,
                notificationPostedAtEpochMillis = postedAtEpochMillis,
            ),
        )
    }

    private fun buildContentKey(
        statusBarNotification: StatusBarNotification,
        notification: Notification,
        postedAtEpochMillis: Long,
    ): String {
        val title = notification.extractConversationTitle().orEmpty()
        val text = notification.extras
            ?.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()

        return buildString {
            append(statusBarNotification.packageName)
            append(':')
            append(statusBarNotification.id)
            append(':')
            append(statusBarNotification.tag.orEmpty())
            append(':')
            append(postedAtEpochMillis)
            append(':')
            append(title.hashCode())
            append(':')
            append(text.hashCode())
        }
    }

    private fun Notification.shouldTrackWhatsappMessage(): Boolean {
        val extras = extras ?: return false
        if ((flags and Notification.FLAG_GROUP_SUMMARY) != 0) return false
        if (category != Notification.CATEGORY_MESSAGE) return false

        val title = extractConversationTitle()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim()

        if (title.isNullOrBlank() && text.isNullOrBlank()) return false
        if (!summaryText.isNullOrBlank() && text.isNullOrBlank()) return false

        return text != null && !text.startsWith("Checking for new messages", ignoreCase = true)
    }

    private fun Notification.extractConversationTitle(): String? {
        return extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?.trim()
                ?.takeIf(String::isNotBlank)
    }

    private fun dayStart(epochMillis: Long): Long {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    companion object {
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val WHATSAPP_LABEL = "WhatsApp"
        private const val DEFAULT_MESSAGE_TITLE = "Incoming message"
    }
}
