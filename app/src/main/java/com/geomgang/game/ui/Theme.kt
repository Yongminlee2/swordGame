package com.geomgang.game.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 대장간의 어두운 화면. 강화 게임은 밝은 배경에서 긴장이 살지 않는다. */
private val ForgeColors = darkColorScheme(
    primary = Color(0xFFE0A458),
    onPrimary = Color(0xFF241704),
    secondary = Color(0xFF7FA5C4),
    background = Color(0xFF0E0B14),
    onBackground = Color(0xFFE6E1F0),
    surface = Color(0xFF1A1426),
    onSurface = Color(0xFFE6E1F0),
    error = Color(0xFFE05A5A),
)

@Composable
fun SwordForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ForgeColors,
        typography = Typography(),
        content = content,
    )
}
