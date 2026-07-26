package com.geomgang.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily

/**
 * 아이콘을 세워서 보여 주기 위한 회전 각도.
 *
 * game-icons 의 무기는 대각선으로 그려져 있다. 강화 게임에서는 검이 서 있어야
 * 단계가 오르는 느낌이 나므로 통째로 돌려 세운다.
 */
private const val UPRIGHT_DEGREES = -45f

/**
 * 검 그림.
 *
 * 실루엣과 색은 [SwordArt] 가 정한다. 이 파일은 겹쳐 그리기와 연출만 맡는다.
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
                    drawRect(color = flashColor.copy(alpha = flash * 0.55f))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (sword != null) {
            SwordLayers(sword.family, sword.level, Modifier.fillMaxSize())
        }
    }
}

/** 도감 칸이나 목록에서 쓰는 작은 검. */
@Composable
fun SwordThumb(
    family: WeaponFamily,
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    dimmed: Boolean = false,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (dimmed) {
            // 아직 못 얻은 칸. 같은 실루엣을 어둡게만 보여 준다.
            Image(
                painter = painterResource(SwordArt.drawableFor(family, level)),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF3E3552)),
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { rotationZ = UPRIGHT_DEGREES },
            )
        } else {
            SwordLayers(family, level, Modifier.size(size))
        }
    }
}

@Composable
private fun SwordLayers(family: WeaponFamily, level: Int, modifier: Modifier) {
    val palette = SwordArt.paletteFor(level)
    val painter = painterResource(SwordArt.drawableFor(family, level))

    Box(modifier, contentAlignment = Alignment.Center) {
        // 뒤에서 번지는 빛. 확대 사본을 겹치는 대신 그라디언트로 깐다 —
        // 아이콘이 뷰박스 중앙에 있지 않아 중심 확대를 하면 그림자가 어긋나 보인다.
        if (palette.auraAlpha > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.glow.copy(alpha = palette.auraAlpha * 0.55f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension * 0.5f,
                    ),
                    radius = size.minDimension * 0.5f,
                    center = Offset(size.width / 2f, size.height / 2f),
                )
            }
        }

        // 단색 실루엣은 납작해 보인다. 같은 그림을 위·아래로 나눠 다른 밝기로 칠하면
        // 위에서 빛이 드는 금속처럼 읽힌다. 아래 두 겹의 그리는 순서가 곧 명암이다.
        SwordBand(painter, palette.glow, top = 0f, bottom = LIGHT_BAND)
        SwordBand(painter, palette.blade, top = LIGHT_BAND, bottom = MID_BAND)
        SwordBand(painter, palette.shade, top = MID_BAND, bottom = 1f)
    }
}

/** 밝은 부분이 차지하는 높이 비율. */
private const val LIGHT_BAND = 0.38f

/** 중간 밝기가 끝나는 높이 비율. */
private const val MID_BAND = 0.72f

/**
 * 검의 일부 높이만 [color] 로 칠해 그린다.
 *
 * 벡터를 그라디언트로 채울 수는 없으므로, 같은 그림을 여러 번 그리며
 * 높이 구간을 잘라 다른 색을 입히는 방식으로 명암을 만든다.
 */
@Composable
private fun SwordBand(
    painter: androidx.compose.ui.graphics.painter.Painter,
    color: Color,
    top: Float,
    bottom: Float,
) {
    Image(
        painter = painter,
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = UPRIGHT_DEGREES }
            .drawWithContent {
                clipRect(
                    top = size.height * top,
                    bottom = size.height * bottom,
                ) {
                    this@drawWithContent.drawContent()
                }
            },
    )
}
