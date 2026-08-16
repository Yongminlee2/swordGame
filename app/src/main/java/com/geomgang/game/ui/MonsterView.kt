package com.geomgang.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.geomgang.game.R

@Composable
private fun rememberMonsterSheet(drawable: Int) = rememberSheet(drawable)

/** Pets intentionally keep the compact original artwork and stable atlas cells. */
@Composable
fun PetSprite(
    petId: String,
    owned: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val sheet = rememberSheet(R.drawable.monster_sheet)
    val src = MonsterSheet.offsetOf(MonsterSheet.petCellOf(petId))
    Canvas(modifier.size(size)) {
        drawImage(
            image = sheet,
            srcOffset = src,
            srcSize = IntSize(MonsterSheet.CELL, MonsterSheet.CELL),
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            filterQuality = FilterQuality.None,
            colorFilter = if (owned) null else ColorFilter.tint(Color(0xFF596079)),
        )
    }
}

/**
 * High-resolution combat monster with recoil and a brief white hit flash.
 * The illustration no longer shrinks as HP falls; health is communicated by the HP bar.
 */
@Composable
fun MonsterSprite(
    name: String,
    hpRatio: Float,
    isBoss: Boolean,
    isRare: Boolean,
    enraged: Boolean,
    hitSeq: Long,
    modifier: Modifier = Modifier,
) {
    val source = MonsterSheet.combatSourceOf(name)
    val sheet = rememberMonsterSheet(source.drawable)
    val impact = remember { Animatable(0f) }

    LaunchedEffect(hitSeq) {
        if (hitSeq == 0L) return@LaunchedEffect
        impact.playHitRealtime()
    }

    val base = if (isBoss) 214.dp else 176.dp
    // 144종 모두 같은 안전 여백 안에 놓는다. 긴 팔·뿔·날개가 있는 대상도
    // 타격 반동 중 프레임 밖으로 나가지 않도록 원본 셀을 84%로 Fit 한다.
    val artworkSize = base * 0.84f
    val statusTint = when {
        hpRatio <= 0f -> Color(0xAA4A5260)
        enraged -> Color(0x88E43F39)
        isRare -> Color(0x66FFD65A)
        else -> null
    }

    Box(modifier.size(base), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(artworkSize)
                .graphicsLayer {
                    translationX = if (impact.value > 0.66f) 9f else -impact.value * 6f
                    scaleX = 1f - impact.value * 0.06f
                    scaleY = 1f + impact.value * 0.045f
                    alpha = if (hpRatio <= 0f) 0.58f else 1f
                },
        ) {
            val destination = IntSize(size.width.toInt(), size.height.toInt())
            // 원본 셀을 한 픽셀도 자르지 않는다. 셀 자체에 투명 여백이 있으므로
            // Medium 보간이면 옆 칸의 흰 선 없이 팔·무기 끝까지 그대로 보인다.
            val safeSourceOffset = source.offset
            val safeSourceSize = IntSize(MonsterSheet.COMBAT_CELL, MonsterSheet.COMBAT_CELL)
            drawImage(
                image = sheet,
                srcOffset = safeSourceOffset,
                srcSize = safeSourceSize,
                dstSize = destination,
                filterQuality = FilterQuality.Medium,
            )
            if (statusTint != null) {
                drawImage(
                    image = sheet,
                    srcOffset = safeSourceOffset,
                    srcSize = safeSourceSize,
                    dstSize = destination,
                    filterQuality = FilterQuality.Medium,
                    colorFilter = ColorFilter.tint(statusTint),
                    alpha = 0.42f,
                )
            }
            if (impact.value > 0f) {
                drawImage(
                    image = sheet,
                    srcOffset = safeSourceOffset,
                    srcSize = safeSourceSize,
                    dstSize = destination,
                    filterQuality = FilterQuality.Medium,
                    colorFilter = ColorFilter.tint(Color.White),
                    alpha = impact.value * 0.68f,
                )
            }
        }
    }
}

/** 기기 애니메이션 배율이 0이어도 190ms 타격 반동을 그대로 재생한다. */
private suspend fun Animatable<Float, AnimationVector1D>.playHitRealtime() {
    val times = floatArrayOf(0f, 28f / 190f, 72f / 190f, 132f / 190f, 1f)
    val values = floatArrayOf(0f, 1f, 0.72f, 0.22f, 0f)
    snapTo(0f)
    val startNanos = withFrameNanos { it }
    while (true) {
        val frameNanos = withFrameNanos { it }
        val progress = ((frameNanos - startNanos) / 1_000_000f / 190f).coerceIn(0f, 1f)
        val end = times.indexOfFirst { progress <= it }.coerceAtLeast(1)
        val start = end - 1
        val local = ((progress - times[start]) / (times[end] - times[start])).coerceIn(0f, 1f)
        snapTo(values[start] + (values[end] - values[start]) * local)
        if (progress >= 1f) break
    }
    snapTo(0f)
}
