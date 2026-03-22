package com.example.usagehistory.data

data class UsageSession(
    val packageName: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
) {
    val durationMillis: Long
        get() = (endedAtEpochMillis - startedAtEpochMillis).coerceAtLeast(0)
}

class SessionBuilder {
    fun build(
        transitions: List<AppTransition>,
        queryEndMillis: Long,
        ignoredPackages: Set<String> = emptySet(),
    ): List<UsageSession> {
        val sessions = mutableListOf<UsageSession>()
        var activePackage: String? = null
        var activeStart: Long? = null

        transitions.sortedBy { it.timestamp }.forEach { transition ->
            if (transition.packageName in ignoredPackages) return@forEach

            when (transition.type) {
                TransitionType.Foreground -> {
                    val currentPackage = activePackage
                    val currentStart = activeStart

                    if (currentPackage == transition.packageName) {
                        return@forEach
                    }

                    if (currentPackage != null && currentStart != null && transition.timestamp > currentStart) {
                        sessions += UsageSession(
                            packageName = currentPackage,
                            startedAtEpochMillis = currentStart,
                            endedAtEpochMillis = transition.timestamp,
                        )
                    }

                    activePackage = transition.packageName
                    activeStart = transition.timestamp
                }

                TransitionType.Background -> {
                    val currentPackage = activePackage
                    val currentStart = activeStart
                    if (currentPackage == transition.packageName &&
                        currentStart != null &&
                        transition.timestamp > currentStart
                    ) {
                        sessions += UsageSession(
                            packageName = currentPackage,
                            startedAtEpochMillis = currentStart,
                            endedAtEpochMillis = transition.timestamp,
                        )
                        activePackage = null
                        activeStart = null
                    }
                }
            }
        }

        val currentPackage = activePackage
        val currentStart = activeStart
        if (currentPackage != null && currentStart != null && queryEndMillis > currentStart) {
            sessions += UsageSession(
                packageName = currentPackage,
                startedAtEpochMillis = currentStart,
                endedAtEpochMillis = queryEndMillis,
            )
        }

        return sessions
            .filter { it.durationMillis > 0 }
            .sortedByDescending { it.startedAtEpochMillis }
    }
}
