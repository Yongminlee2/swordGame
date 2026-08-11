package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.min

/**
 * 사냥과 무한 회랑이 함께 쓰는 2D 전투 무대.
 *
 * 몬스터 뒤를 단색 패널로 비워 두지 않고, 낮은 대비의 광원과 원근 격자로
 * "싸우는 자리"를 만든다. 선은 의도적으로 굵고 단순하게 두어 게임의 B급 픽셀
 * 감성과 어울리게 한다.
 */
@Composable
fun BattleArenaBackdrop(
    modifier: Modifier = Modifier,
    danger: Boolean = false,
) {
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val accent = if (danger) ForgeRed else MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    surface.copy(alpha = 0.92f),
                    background.copy(alpha = 0.80f),
                    background,
                ),
            ),
        )

        val horizon = size.height * 0.68f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accent.copy(alpha = 0.18f),
                    accent.copy(alpha = 0.05f),
                    Color.Transparent,
                ),
                center = Offset(size.width / 2f, size.height * 0.46f),
                radius = min(size.width, size.height) * 0.58f,
            ),
            radius = min(size.width, size.height) * 0.58f,
            center = Offset(size.width / 2f, size.height * 0.46f),
        )

        drawRect(
            color = accent.copy(alpha = 0.035f),
            topLeft = Offset(0f, horizon),
            size = Size(size.width, size.height - horizon),
        )
        val thin = 1.15f * density
        val strong = 2f * density
        drawLine(
            color = accent.copy(alpha = 0.28f),
            start = Offset(0f, horizon),
            end = Offset(size.width, horizon),
            strokeWidth = strong,
        )

        // 바닥의 투박한 원근 격자. 몬스터 실루엣을 방해하지 않을 만큼만 보인다.
        for (index in -4..4) {
            drawLine(
                color = accent.copy(alpha = 0.10f),
                start = Offset(size.width / 2f + index * size.width * 0.018f, horizon),
                end = Offset(size.width / 2f + index * size.width * 0.19f, size.height),
                strokeWidth = thin,
            )
        }
        listOf(0.73f, 0.80f, 0.89f, 0.99f).forEach { fraction ->
            drawLine(
                color = accent.copy(alpha = 0.10f),
                start = Offset(0f, size.height * fraction),
                end = Offset(size.width, size.height * fraction),
                strokeWidth = thin,
            )
        }

        // 네 귀퉁이 표식은 패널을 HUD처럼 보이게 하고 클릭 영역의 경계를 알려 준다.
        val inset = 10f * density
        val mark = 18f * density
        val markerColor = accent.copy(alpha = 0.58f)
        listOf(
            Offset(inset, inset) to Offset(inset + mark, inset),
            Offset(inset, inset) to Offset(inset, inset + mark),
            Offset(size.width - inset, inset) to Offset(size.width - inset - mark, inset),
            Offset(size.width - inset, inset) to Offset(size.width - inset, inset + mark),
            Offset(inset, size.height - inset) to Offset(inset + mark, size.height - inset),
            Offset(inset, size.height - inset) to Offset(inset, size.height - inset - mark),
            Offset(size.width - inset, size.height - inset) to
                Offset(size.width - inset - mark, size.height - inset),
            Offset(size.width - inset, size.height - inset) to
                Offset(size.width - inset, size.height - inset - mark),
        ).forEach { (start, end) ->
            drawLine(markerColor, start, end, strokeWidth = strong)
        }
    }
}
