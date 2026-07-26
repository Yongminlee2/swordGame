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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 원의 지름. 일부러 작게 둔다 — 크고 편한 버튼이면 원작의 긴장이 사라진다. */
private val RING_SIZE = 108.dp

/** 이 비율 아래로 남으면 색이 붉어진다. */
private const val URGENT_THRESHOLD = 0.35f

/**
 * 방지권을 쓸 수 있는 작은 원.
 *
 * 테두리 호가 줄어드는 것으로 남은 시간을 보여 준다.
 * 원작의 "작은 원을 클릭해 살린다"는 감각을 그대로 가져온 것이라 크기를 키우지 않는다.
 */
@Composable
fun PreventRing(
    progress: Float,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val urgent = progress < URGENT_THRESHOLD
    val ringColor = when {
        !enabled -> Color(0xFF6B6B6B)
        urgent -> Color(0xFFE05A5A)
        else -> Color(0xFFE0A458)
    }
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(RING_SIZE)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(RING_SIZE)) {
            val stroke = size.minDimension * 0.08f
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            // 남은 시간이 없는 부분은 어둡게 깔아 둔다
            drawArc(
                color = Color(0xFF2A2340),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            // 남은 시간
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            // 누를 곳임을 알리는 옅은 채움
            drawCircle(
                color = ringColor.copy(alpha = 0.12f),
                radius = size.minDimension / 2f - stroke,
            )
        }

        Text(
            text = if (enabled) "살리기" else "방지권 없음",
            color = ringColor,
            fontSize = if (enabled) 16.sp else 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
