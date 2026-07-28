package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import com.geomgang.game.R

/**
 * 검 그림 (강화 화면의 큰 검).
 *
 * 큰 화면은 64px 시트2(계열×티어 큐레이션)를 쓴다. 고유검은 전용 칸이 있다.
 * 오라는 초창기 벡터 아트에서 살아남은 유산 - 시트 위에 그대로 얹는다.
 * 픽셀아트라 확대할 때 보간을 끄는 것이 중요하다 — 켜 두면 흐물흐물해진다.
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
    Box(
        modifier = modifier
            .graphicsLayer { translationX = shake }
            .drawWithContent {
                drawContent()
                if (flash > 0f) {
                    drawRect(color = flashColor.copy(alpha = flash * 0.5f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (sword != null) {
            Sprite2Box(sword, Modifier.fillMaxSize())
        }
    }
}

/**
 * 검 한 자루를 그린다. 오라 포함.
 *
 * 단계마다 다른 그림을 쓰므로 시트3이 기본이고, 고유검만 시트2의 전용 칸을 쓴다.
 * 어느 시트인지는 [SwordSheet3.sourceFor] 가 정한다.
 */
@Composable
private fun Sprite2Box(sword: Sword, modifier: Modifier) {
    val source = SwordSheet3.sourceFor(sword)

    // 낱장 그림을 쓰는 구간은 시트를 보지 않는다. 오라도 그리지 않는다 -
    // 그림 자체가 이미 빛으로 가득해서 겹치면 뭉갠다.
    if (sword.uniqueId == null && LegendArt.has(sword.level)) {
        val bitmap = rememberLegendBitmap(sword.level)
        Canvas(modifier) {
            val side = size.minDimension
            val left = ((size.width - side) / 2f).toInt()
            val top = ((size.height - side) / 2f).toInt()
            drawImage(
                image = bitmap,
                srcOffset = IntOffset(0, 0),
                srcSize = IntSize(bitmap.width, bitmap.height),
                dstOffset = IntOffset(left, top),
                dstSize = IntSize(side.toInt(), side.toInt()),
                filterQuality = FilterQuality.None,
            )
        }
        return
    }

    val sheet = if (source.useSheet3) rememberSheet3() else rememberSheet2()
    val src = if (source.useSheet3) {
        SwordSheet3.offsetOf(source.cell)
    } else {
        SwordSheet2.offsetOf(source.cell)
    }
    val cellSize = if (source.useSheet3) SwordSheet3.CELL else SwordSheet2.CELL
    val aura = SwordSheet.auraFor(sword.level)

    Canvas(modifier) {
        if (aura.alpha > 0f) {
            val r = size.minDimension * 0.46f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(aura.color.copy(alpha = aura.alpha), Color.Transparent),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = r,
                ),
                radius = r,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }
        val side = size.minDimension
        val left = ((size.width - side) / 2f).toInt()
        val top = ((size.height - side) / 2f).toInt()
        drawImage(
            image = sheet,
            srcOffset = IntOffset(src.x, src.y),
            srcSize = IntSize(cellSize, cellSize),
            dstOffset = IntOffset(left, top),
            dstSize = IntSize(side.toInt(), side.toInt()),
            filterQuality = FilterQuality.None,
        )
    }
}

/**
 * 도감 칸용 — 계열과 단계로 정확히 한 그림. 미발견은 어둡게.
 *
 * [family] 가 null 이면 전설 그림이다 — +21 위는 계열과 무관하게 같은 그림을 쓴다.
 * 강화 화면에서 본 그림과 같아야 "그 검"으로 읽히므로 시트3의 같은 칸을 본다.
 */
@Composable
fun LevelThumb(
    family: WeaponFamily?,
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    dimmed: Boolean = false,
) {
    if (family == null && LegendArt.has(level)) {
        LegendSwordArt(level, modifier, size, dimmed)
        return
    }

    val cell = if (family == null) {
        SwordSheet3.legendCellOf(level)
    } else {
        SwordSheet3.cellOf(family, level)
    }
    val sheet = rememberSheet3()
    val src = SwordSheet3.offsetOf(cell)
    Canvas(modifier.size(size)) {
        drawImage(
            image = sheet,
            srcOffset = IntOffset(src.x, src.y),
            srcSize = IntSize(SwordSheet3.CELL, SwordSheet3.CELL),
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            filterQuality = FilterQuality.None,
            colorFilter = if (dimmed) ColorFilter.tint(Color(0xFF2E2740)) else null,
        )
    }
}

/** 도감 고유검 칸용. */
@Composable
fun UniqueThumb(
    uniqueId: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    dimmed: Boolean = false,
) {
    val sheet = rememberSheet2()
    val src = SwordSheet2.offsetOf(SwordSheet2.uniqueCellOf(uniqueId))
    Canvas(modifier.size(size)) {
        drawImage(
            image = sheet,
            srcOffset = IntOffset(src.x, src.y),
            srcSize = IntSize(SwordSheet2.CELL, SwordSheet2.CELL),
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            filterQuality = FilterQuality.None,
            colorFilter = if (dimmed) ColorFilter.tint(Color(0xFF2E2740)) else null,
        )
    }
}

/**
 * 목록에서 쓰는 작은 검.
 *
 * 그림을 고르는 길이 [SwordView] 와 **똑같아야 한다.** 예전에는 여기만 시트2 를 봐서
 * 보관함의 검과 강화 화면의 검이 같은 자루인데도 다르게 생겼다. 시트 선택을
 * [SwordSheet3.sourceFor] 한 곳에 맡겨 그 어긋남을 없앴다.
 */
@Composable
fun SwordThumb(
    sword: Sword,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    dimmed: Boolean = false,
) {
    if (sword.uniqueId == null && LegendArt.has(sword.level)) {
        LegendSwordArt(sword.level, modifier, size, dimmed)
        return
    }

    val source = SwordSheet3.sourceFor(sword)
    val sheet = if (source.useSheet3) rememberSheet3() else rememberSheet2()
    val src = if (source.useSheet3) {
        SwordSheet3.offsetOf(source.cell)
    } else {
        SwordSheet2.offsetOf(source.cell)
    }
    val cellSize = if (source.useSheet3) SwordSheet3.CELL else SwordSheet2.CELL
    Canvas(modifier.size(size)) {
        drawImage(
            image = sheet,
            srcOffset = IntOffset(src.x, src.y),
            srcSize = IntSize(cellSize, cellSize),
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            filterQuality = FilterQuality.None,
            colorFilter = if (dimmed) ColorFilter.tint(Color(0xFF2E2740)) else null,
        )
    }
}

/**
 * 스프라이트시트를 한 번만 읽어 재사용한다.
 *
 * 검을 그리는 곳이 여러 군데라 매번 디코딩하면 낭비다.
 *
 * `inScaled = false` 가 중요하다. 켜 두면 안드로이드가 화면 밀도에 맞춰 비트맵을 늘려
 * 48×480 이 몇 배로 커지고, 그러면 16px 격자를 세는 [SwordSheet] 의 좌표가 전부 어긋난다.
 */
@Composable
private fun rememberSheet2(): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(resources, R.drawable.sword_sheet2, options).asImageBitmap()
    }
}

@Composable
private fun rememberSheet3(): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(resources, R.drawable.sword_sheet3, options).asImageBitmap()
    }
}
