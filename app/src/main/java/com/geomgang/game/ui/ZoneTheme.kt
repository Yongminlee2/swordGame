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
        Zone.SKY_GALLERY -> Color(0xFF16202C) to Color(0xFF0B1018)
        Zone.RUINED_CAPITAL -> Color(0xFF241E1A) to Color(0xFF130F0D)
        Zone.SILENT_TEMPLE -> Color(0xFF211F18) to Color(0xFF12110C)
        Zone.GLASS_DESERT -> Color(0xFF26221A) to Color(0xFF14120D)
        Zone.FLOATING_ISLE -> Color(0xFF17242A) to Color(0xFF0C1317)
        Zone.WARPED_WOOD -> Color(0xFF1B2418) to Color(0xFF0E130C)
        Zone.SUNKEN_CITY -> Color(0xFF122029) to Color(0xFF091116)
        Zone.ASH_PLAIN -> Color(0xFF231F1D) to Color(0xFF121010)
        Zone.STAR_TOMB -> Color(0xFF191A2A) to Color(0xFF0D0E17)
        Zone.TIME_RIFT -> Color(0xFF1E1730) to Color(0xFF100C1A)
        Zone.BLOOD_KEEP -> Color(0xFF2A1417) to Color(0xFF150A0C)
        Zone.FROST_HEART -> Color(0xFF16232B) to Color(0xFF0B1217)
        Zone.FIRST_FORGE -> Color(0xFF2A1D12) to Color(0xFF150F0A)
        Zone.FINAL_GATE -> Color(0xFF221226) to Color(0xFF110914)
    }
    return Brush.verticalGradient(listOf(top, bottom))
}
