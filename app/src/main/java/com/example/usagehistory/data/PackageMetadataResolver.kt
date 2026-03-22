package com.example.usagehistory.data

import android.content.Context
import android.graphics.drawable.Drawable

class PackageMetadataResolver(
    private val context: Context,
) {
    fun resolveLabel(packageName: String): String {
        return runCatching {
            val applicationInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }

    fun resolveIcon(packageName: String): Drawable? {
        return runCatching {
            context.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }
}
