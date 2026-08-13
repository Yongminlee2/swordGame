package com.geomgang.game.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
        impact.snapTo(0f)
        impact.animateTo(
            0f,
            keyframes {
                durationMillis = 190
                1f at 28
                0.72f at 72
                0.22f at 132
                0f at 190
            },
        )
    }

    val base = if (isBoss) 214.dp else 176.dp
    val statusTint = when {
        hpRatio <= 0f -> Color(0xAA4A5260)
        enraged -> Color(0x88E43F39)
        isRare -> Color(0x66FFD65A)
        else -> null
    }

    Box(modifier.size(base), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(base)
                .graphicsLayer {
                    translationX = if (impact.value > 0.66f) 9f else -impact.value * 6f
                    scaleX = 1f - impact.value * 0.06f
                    scaleY = 1f + impact.value * 0.045f
                    alpha = if (hpRatio <= 0f) 0.58f else 1f
                },
        ) {
            val destination = IntSize(size.width.toInt(), size.height.toInt())
            drawImage(
                image = sheet,
                srcOffset = source.offset,
                srcSize = IntSize(MonsterSheet.COMBAT_CELL, MonsterSheet.COMBAT_CELL),
                dstSize = destination,
                filterQuality = FilterQuality.High,
            )
            if (statusTint != null) {
                drawImage(
                    image = sheet,
                    srcOffset = source.offset,
                    srcSize = IntSize(MonsterSheet.COMBAT_CELL, MonsterSheet.COMBAT_CELL),
                    dstSize = destination,
                    filterQuality = FilterQuality.High,
                    colorFilter = ColorFilter.tint(statusTint),
                    alpha = 0.42f,
                )
            }
            if (impact.value > 0f) {
                drawImage(
                    image = sheet,
                    srcOffset = source.offset,
                    srcSize = IntSize(MonsterSheet.COMBAT_CELL, MonsterSheet.COMBAT_CELL),
                    dstSize = destination,
                    filterQuality = FilterQuality.High,
                    colorFilter = ColorFilter.tint(Color.White),
                    alpha = impact.value * 0.68f,
                )
            }
        }
    }
}
