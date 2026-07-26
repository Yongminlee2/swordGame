package com.geomgang.game.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.geomgang.core.Zone

/**
 * 구역마다 배경 분위기.
 *
 * 어두운 테마 위에 얹는 은은한 세로 그라디언트다. 초원은 녹색, 화산은 적색 -
 * 구역을 옮겼다는 것이 색으로 먼저 느껴져야 한다.
 */
fun zoneBrush(zone: Zone): Brush {
    val (top, bottom) = when (zone) {
        Zone.MEADOW -> Color(0xFF17251A) to Color(0xFF0E1410)
        Zone.FOREST -> Color(0xFF152417) to Color(0xFF0D130D)
        Zone.CAVE -> Color(0xFF1B1B22) to Color(0xFF101014)
        Zone.MINE -> Color(0xFF221D16) to Color(0xFF141110)
        Zone.SWAMP -> Color(0xFF18221E) to Color(0xFF0E1412)
        Zone.VOLCANO -> Color(0xFF261314) to Color(0xFF140D0D)
        Zone.SNOWFIELD -> Color(0xFF1A2026) to Color(0xFF10131A)
        Zone.DRAGON_NEST -> Color(0xFF221520) to Color(0xFF120D12)
        Zone.ABYSS -> Color(0xFF151226) to Color(0xFF0C0A16)
        Zone.ENDLESS_HALL -> Color(0xFF1F1A26) to Color(0xFF120F16)
    }
    return Brush.verticalGradient(listOf(top, bottom))
}
