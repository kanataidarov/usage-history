package com.example.usagehistory.data

import android.content.Intent
import android.content.Context
import android.graphics.drawable.Drawable

class PackageMetadataResolver(
    context: Context,
) {
    private val packageManager = context.packageManager
    private val timelineVisibilityCache = mutableMapOf<String, Boolean>()

    fun resolveLabel(packageName: String): String {
        return runCatching {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }

    fun resolveIcon(packageName: String): Drawable? {
        return runCatching {
            packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }

    fun shouldTrackInTimeline(packageName: String): Boolean {
        return timelineVisibilityCache.getOrPut(packageName) {
            val normalizedPackageName = packageName.lowercase()
            val normalizedLabel = resolveLabel(packageName).lowercase()

            hasLauncherEntry(packageName) &&
                normalizedPackageName !in blockedPackages &&
                blockedPackagePrefixes.none(normalizedPackageName::startsWith) &&
                blockedLabelKeywords.none(normalizedLabel::contains)
        }
    }

    private fun hasLauncherEntry(packageName: String): Boolean {
        return packageManager.getLaunchIntentForPackage(packageName) != null ||
            runCatching {
                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    `package` = packageName
                }
                packageManager.queryIntentActivities(launcherIntent, 0).isNotEmpty()
            }.getOrDefault(false)
    }

    private companion object {
        val blockedPackages =
            setOf(
                "android",
                "com.android.settings",
                "com.android.permissioncontroller",
                "com.google.android.permissioncontroller",
                "com.android.packageinstaller",
                "com.google.android.packageinstaller",
                "com.android.systemui",
            )

        val blockedPackagePrefixes =
            setOf(
                "com.android.launcher",
                "com.google.android.launcher",
                "com.oppo.launcher",
                "com.coloros.launcher",
                "com.sec.android.app.launcher",
                "com.huawei.android.launcher",
                "com.miui.home",
                "com.vivo.launcher",
            )

        val blockedLabelKeywords =
            setOf(
                "launcher",
                "settings",
                "permission controller",
                "package installer",
                "system ui",
            )
    }
}
