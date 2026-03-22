package com.example.usagehistory.data

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

data class AppTransition(
    val packageName: String,
    val timestamp: Long,
    val type: TransitionType,
)

enum class TransitionType {
    Foreground,
    Background,
}

class UsageEventReader(
    context: Context,
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun readTransitions(startTimeMillis: Long, endTimeMillis: Long): List<AppTransition> {
        val result = mutableListOf<AppTransition>()
        val events = usageStatsManager.queryEvents(startTimeMillis, endTimeMillis)
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val packageName = event.packageName.orEmpty()
            if (packageName.isBlank()) continue

            val transitionType = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> TransitionType.Foreground

                UsageEvents.Event.ACTIVITY_PAUSED -> TransitionType.Background

                else -> null
            } ?: continue

            result += AppTransition(
                packageName = packageName,
                timestamp = event.timeStamp,
                type = transitionType,
            )
        }

        return result.sortedBy { it.timestamp }
    }
}
