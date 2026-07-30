package com.geomgang.game.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import com.geomgang.game.R

/**
 * 몬스터·펫 시트.
 *
 * 읽기는 [SpriteCache] 가 프로세스 수준에서 한 번만 한다 — 펫 도감처럼 자리가
 * 스물넷인 화면이 `remember` 를 쓰면 그 수만큼 다시 읽는다.
 */
@Composable
private fun rememberMonsterSheet(): ImageBitmap = rememberSheet(R.drawable.monster_sheet)

/** 펫 아이콘. 미소유는 회색 실루엣. */
@Composable
fun PetSprite(
    petId: String,
    owned: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val sheet = rememberMonsterSheet()
    val src = MonsterSheet.offsetOf(MonsterSheet.petCellOf(petId))
    Canvas(modifier.size(size)) {
        drawImage(
            image = sheet,
            srcOffset = src,
            srcSize = IntSize(MonsterSheet.CELL, MonsterSheet.CELL),
            dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
            filterQuality = FilterQuality.None,
            colorFilter = if (owned) null else ColorFilter.tint(Color(0xE0333344)),
        )
    }
}

/**
 * 몬스터 한 마리.
 *
 * - 체력이 줄면 쪼그라든다 (기존 MonsterBlob 의 감각 유지)
 * - 희귀 = 금색 틴트, 보스 발악 = 붉은 틴트
 * - hitSeq 가 바뀔 때마다 좌우로 짧게 흔들린다
 * - 체력 0 = 회색으로 바랜다
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
    val sheet = rememberMonsterSheet()
    val src = MonsterSheet.offsetOf(MonsterSheet.cellOf(name))

    val shake = remember { Animatable(0f) }
    LaunchedEffect(hitSeq) {
        if (hitSeq == 0L) return@LaunchedEffect
        shake.snapTo(0f)
        shake.animateTo(
            0f,
            keyframes {
                durationMillis = 120
                6f at 30
                -5f at 70
                0f at 120
            },
        )
    }

    val base: Dp = if (isBoss) 150.dp else 110.dp
    val side = base * (0.55f + hpRatio * 0.45f)
    val tint: Color? = when {
        hpRatio <= 0f -> Color(0xB3555555)
        enraged -> Color(0x66E05A5A)
        isRare -> Color(0x66FFD54A)
        else -> null
    }

    Canvas(modifier.size(side)) {
        val dst = IntSize(size.width.toInt(), size.height.toInt())
        val dstOffset = IntOffset(shake.value.toInt(), 0)
        drawImage(
            image = sheet,
            srcOffset = src,
            srcSize = IntSize(MonsterSheet.CELL, MonsterSheet.CELL),
            dstOffset = dstOffset,
            dstSize = dst,
            filterQuality = FilterQuality.None,
        )
        if (tint != null) {
            drawImage(
                image = sheet,
                srcOffset = src,
                srcSize = IntSize(MonsterSheet.CELL, MonsterSheet.CELL),
                dstOffset = dstOffset,
                dstSize = dst,
                filterQuality = FilterQuality.None,
                colorFilter = ColorFilter.tint(tint),
            )
        }
    }
}
