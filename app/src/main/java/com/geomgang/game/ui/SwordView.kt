package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.geomgang.core.Sword
import com.geomgang.core.WeaponCatalog

/**
 * 검 그림. M2 에서는 도형 플레이스홀더다.
 *
 * M5 에서 벡터 레이어 조립(날·코등이·손잡이·보석·오라)으로 교체할 때
 * 이 파일만 갈아 끼우면 되도록, 검을 그리는 지식을 여기 한 곳에 가둔다.
 */
@Composable
fun SwordView(sword: Sword?, modifier: Modifier = Modifier) {
    val tierIndex = sword?.let { WeaponCatalog.tierFor(it.level).ordinal } ?: 0
    val bladeColor = TIER_COLORS[tierIndex.coerceIn(TIER_COLORS.indices)]
    val hasAura = (sword?.level ?: 0) >= WeaponCatalog.AURA_MIN_LEVEL

    Canvas(modifier) {
        if (sword == null) return@Canvas

        val w = size.width
        val h = size.height
        val cx = w / 2f

        if (hasAura) {
            drawCircle(
                color = bladeColor.copy(alpha = 0.18f),
                radius = w * 0.38f,
                center = Offset(cx, h * 0.42f),
            )
        }

        // 날
        drawRect(
            color = bladeColor,
            topLeft = Offset(cx - w * 0.05f, h * 0.10f),
            size = Size(w * 0.10f, h * 0.55f),
        )
        // 코등이
        drawRect(
            color = Color(0xFF8A6A3B),
            topLeft = Offset(cx - w * 0.18f, h * 0.65f),
            size = Size(w * 0.36f, h * 0.05f),
        )
        // 손잡이
        drawRect(
            color = Color(0xFF5A4A32),
            topLeft = Offset(cx - w * 0.04f, h * 0.70f),
            size = Size(w * 0.08f, h * 0.20f),
        )
        // 손잡이 끝 보석
        drawCircle(
            color = bladeColor,
            radius = w * 0.045f,
            center = Offset(cx, h * 0.92f),
        )
    }
}

/** 티어 순서대로 색이 화려해진다. WeaponTier 선언 순서와 짝을 이룬다. */
private val TIER_COLORS = listOf(
    Color(0xFF8A8A8A), // 녹슨 검
    Color(0xFFB8C0C8), // 강철검
    Color(0xFFD8DEE8), // 은장검
    Color(0xFF9AD7E0), // 룬검
    Color(0xFFE08A4A), // 화염검
    Color(0xFFEBD75A), // 뇌전검
    Color(0xFFF5F0C8), // 여명의 성검
    Color(0xFF9A5AD0), // 흑룡참
    Color(0xFFE05A8A), // 용린참
    Color(0xFF5A5AD0), // 심연검
    Color(0xFFFFFFFF), // 이름 없는 검
)
