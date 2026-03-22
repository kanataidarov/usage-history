package com.example.usagehistory.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TimelineAccent,
    secondary = ColorTokens.Secondary,
    tertiary = ColorTokens.Tertiary,
    background = TimelineBackground,
    surface = SurfaceWarm,
)

@Composable
fun UsageHistoryTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}

private object ColorTokens {
    val Secondary = androidx.compose.ui.graphics.Color(0xFF7B5E57)
    val Tertiary = androidx.compose.ui.graphics.Color(0xFF6D5E98)
}
