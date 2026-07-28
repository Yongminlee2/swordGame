package com.geomgang.game.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.size
import com.geomgang.game.R

/**
 * 시트 칸 대신 통짜 그림을 쓰는 예외.
 *
 * 검 그림은 원칙적으로 [SwordSheet3] 의 64px 격자에서 온다. 여기는 그 원칙의 예외로,
 * **마지막 전설 칸**(+41 이상과 도감 +40 칸이 함께 쓰는 자리)만 전용 그림으로 덮는다.
 *
 * 시트에 넣지 않은 이유: 이 그림은 격자 칸이 아니라 1254px 짜리 통짜 일러스트라
 * 64px 칸에 욱여넣으면 아무것도 안 보인다.
 */
object SwordOverride {

    /** 이 칸을 전용 그림으로 덮을지. */
    fun has(cell: Int): Boolean = cell == SwordSheet3.LAST_LEGEND_CELL
}

/**
 * 덮어쓴 전용 그림 하나. 시트와 달리 칸을 잘라내지 않고 통째로 그린다.
 *
 * 이 그림은 투명한 배경이 없다 - 어두운 보라 사각형이 그대로 보인다.
 * 다른 검처럼 배경이 뚫려 보이지 않는 것은 그림 자체의 성질이다.
 */
@Composable
fun OverrideSwordArt(
    modifier: Modifier = Modifier,
    size: Dp,
    dimmed: Boolean = false,
) {
    val bitmap = rememberOverrideBitmap()
    Canvas(modifier.size(size)) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(0, 0),
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            // 픽셀아트가 아니라 부드러운 그림이라 보간을 켠다.
            filterQuality = FilterQuality.Medium,
            colorFilter = if (dimmed) ColorFilter.tint(Color(0xFF2E2740)) else null,
        )
    }
}

@Composable
fun rememberOverrideBitmap(): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory
            .decodeResource(resources, R.drawable.sword_legend_max, options)
            .asImageBitmap()
    }
}
