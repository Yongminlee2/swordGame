package com.geomgang.game.ui

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.geomgang.game.R

/**
 * 최상단 단계의 전용 그림.
 *
 * 검 그림은 원칙적으로 [SwordSheet3] 의 64px 격자에서 온다. 여기는 그 원칙의 예외다 —
 * +40 부터는 단계마다 128px 짜리 낱장 그림을 쓴다. 시트의 전설 칸은 +21~+39 만 덮는다.
 *
 * 격자에 넣지 않은 이유: 낱장이 시트 칸보다 크고, 이 구간은 그림이 한 장씩 늘어나는
 * 자리라 시트를 매번 다시 합성하는 것보다 파일을 더하는 편이 싸다.
 *
 * [MAX_LEVEL] 위는 마지막 그림을 계속 쓴다. 상한 없는 구간을 그림으로 좇을 수는 없다.
 */
object LegendArt {

    /** 낱장 그림이 시작되는 단계. 여기부터는 시트를 보지 않는다. */
    const val MIN_LEVEL = 40

    /** 낱장 그림이 있는 마지막 단계. */
    const val MAX_LEVEL = 50

    fun has(level: Int): Boolean = level >= MIN_LEVEL

    @DrawableRes
    fun drawableFor(level: Int): Int = when (level.coerceIn(MIN_LEVEL, MAX_LEVEL)) {
        40 -> R.drawable.sword_lv40
        41 -> R.drawable.sword_lv41
        42 -> R.drawable.sword_lv42
        43 -> R.drawable.sword_lv43
        44 -> R.drawable.sword_lv44
        45 -> R.drawable.sword_lv45
        46 -> R.drawable.sword_lv46
        47 -> R.drawable.sword_lv47
        48 -> R.drawable.sword_lv48
        49 -> R.drawable.sword_lv49
        else -> R.drawable.sword_lv50
    }
}

/**
 * 낱장 전설 그림 하나. 시트와 달리 칸을 잘라내지 않고 통째로 그린다.
 *
 * 픽셀아트라 [FilterQuality.None] 이다 — 보간을 켜면 128px 그림이 뭉개진다.
 */
@Composable
fun LegendSwordArt(
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp,
    dimmed: Boolean = false,
) {
    val bitmap = rememberLegendBitmap(level)
    Canvas(modifier.size(size)) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset(0, 0),
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            filterQuality = FilterQuality.None,
            colorFilter = if (dimmed) ColorFilter.tint(Color(0xFF2E2740)) else null,
        )
    }
}

/**
 * 전설검 그림 한 장.
 *
 * 도감의 전설 구획은 단계마다 다른 그림을 쓰므로 자리가 서른 개다. `remember` 로는
 * 자리마다 다시 읽으니 [SpriteCache] 에 맡긴다 — 128×128 열한 장이라 다 물고 있어도
 * 0.7MB 다.
 */
@Composable
fun rememberLegendBitmap(level: Int): ImageBitmap =
    rememberSheet(LegendArt.drawableFor(level))
