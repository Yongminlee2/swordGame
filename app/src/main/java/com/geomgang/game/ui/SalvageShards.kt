package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val FIELD_SIZE = 190.dp

/**
 * 파편은 고정된 각도·거리로 배치한다.
 *
 * 난수로 흩뿌리면 재구성될 때마다 위치가 바뀌어 누르기 어려워진다.
 * 각 원소는 (각도 degree, 중심에서의 거리 비율, 크기 비율).
 */
private val SHARDS = listOf(
    Triple(20f, 0.55f, 0.055f),
    Triple(75f, 0.80f, 0.040f),
    Triple(130f, 0.50f, 0.065f),
    Triple(175f, 0.75f, 0.045f),
    Triple(225f, 0.60f, 0.050f),
    Triple(270f, 0.85f, 0.038f),
    Triple(310f, 0.48f, 0.060f),
)

/**
 * 파괴된 검의 파편. 영역 아무 데나 누르면 주워진다.
 *
 * 방지권과 달리 넉넉한 영역을 준다. 줍기를 놓치는 것은 손해일 뿐 파국이 아니라서,
 * 여기까지 빡빡하게 만들면 짜증만 남는다.
 */
@Composable
fun SalvageShards(
    progress: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    // 시간이 흐를수록 파편이 밖으로 퍼지며 흐려진다
    val spread = 1f - progress
    val alpha = (0.35f + progress * 0.65f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .size(FIELD_SIZE)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(FIELD_SIZE)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            SHARDS.forEach { (angleDeg, distance, scale) ->
                val rad = angleDeg * PI.toFloat() / 180f
                val d = radius * distance * (0.7f + spread * 0.5f)
                drawCircle(
                    color = Color(0xFFB8C0C8).copy(alpha = alpha),
                    radius = radius * scale,
                    center = Offset(
                        x = center.x + cos(rad) * d,
                        y = center.y + sin(rad) * d,
                    ),
                )
            }
        }

        Text(
            text = "줍기",
            color = Color(0xFFD8DEE8).copy(alpha = alpha),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
