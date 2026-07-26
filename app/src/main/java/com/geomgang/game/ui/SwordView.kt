package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.geomgang.core.Sword

/**
 * 검 그림.
 *
 * 형태와 색은 전부 [drawSword] 가 정한다. 이 파일은 연출(흔들림·번쩍임)만 얹는다.
 *
 * @param shake 좌우 흔들림(px). 실패·하락 연출에서 준다.
 * @param flash 0~1. 1에 가까울수록 [flashColor] 로 덮인다. 성공·파괴 연출에서 준다.
 */
@Composable
fun SwordView(
    sword: Sword?,
    modifier: Modifier = Modifier,
    shake: Float = 0f,
    flash: Float = 0f,
    flashColor: Color = Color.White,
) {
    Canvas(
        modifier
            .graphicsLayer { translationX = shake }
            .drawWithContent {
                drawContent()
                if (flash > 0f) {
                    drawRect(color = flashColor.copy(alpha = flash * 0.6f))
                }
            },
    ) {
        if (sword != null) {
            drawSword(sword.family, sword.level)
        }
    }
}
