package com.geomgang.game.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.geomgang.core.WeaponCatalog
import com.geomgang.core.WeaponFamily
import com.geomgang.core.WeaponTier
import kotlin.math.sin

/**
 * 검 그림의 단일 출처.
 *
 * 통 이미지 88장을 두지 않는다. **계열이 형태를, 티어가 색과 장식을 정하는** 두 축의
 * 파라미터로 조립한다. 그림 파일이 0개라 APK 가 커지지 않고, 톤이 저절로 일관되며,
 * 계열이나 티어를 늘릴 때 파라미터 한 줄만 추가하면 된다.
 *
 * 나중에 CC0 스프라이트로 갈아 끼우게 되면 이 파일만 바꾸면 된다.
 *
 * 저작권: 타사 게임 리소스를 쓰지 않는다. 곡선날·가시·비늘·십자 코등이 같은 것은
 * 판타지 무기의 일반적 표현 관습이라 특정 주체의 저작물이 아니다.
 */

/** 날에 붙는 특징. 계열을 구분하는 핵심이다. */
enum class BladeMotif {
    /** 아무 장식 없는 매끈한 날. */
    PLAIN,

    /** 날 양옆에 가시가 돋는다. 마검. */
    THORNS,

    /** 날 가운데를 따라 홈이 파인다. 성검. */
    FULLER,

    /** 날 표면에 비늘이 겹친다. 용검. */
    SCALES,
}

/** 코등이 모양. */
enum class GuardStyle { BAR, CROSS, WING }

/**
 * 계열이 정하는 날의 형태.
 *
 * @param widthRatio  캔버스 너비 대비 날 폭
 * @param lengthRatio 캔버스 높이 대비 날 길이
 * @param curve       휘어짐. 0이면 곧고, 클수록 한쪽으로 휜다
 * @param wave        물결침. 0이 아니면 날이 구불거린다
 * @param singleEdge  한쪽날이면 true (등이 두껍고 날이 한쪽만 선다)
 * @param twin        두 자루가 X 로 겹친다
 */
data class BladeSpec(
    val widthRatio: Float,
    val lengthRatio: Float,
    val curve: Float,
    val wave: Float,
    val singleEdge: Boolean,
    val twin: Boolean,
    val motif: BladeMotif,
    val guard: GuardStyle,
)

/** 티어가 정하는 색과 광택. */
data class TierStyle(
    val blade: Color,
    val edge: Color,
    val guard: Color,
    val gem: Color,
    val auraAlpha: Float,
)

fun bladeSpecOf(family: WeaponFamily): BladeSpec = when (family) {
    WeaponFamily.STRAIGHT -> BladeSpec(
        widthRatio = 0.11f, lengthRatio = 0.56f, curve = 0f, wave = 0f,
        singleEdge = false, twin = false, motif = BladeMotif.PLAIN, guard = GuardStyle.BAR,
    )

    WeaponFamily.CURVED -> BladeSpec(
        widthRatio = 0.12f, lengthRatio = 0.58f, curve = 0.20f, wave = 0f,
        singleEdge = true, twin = false, motif = BladeMotif.PLAIN, guard = GuardStyle.BAR,
    )

    WeaponFamily.GREAT -> BladeSpec(
        widthRatio = 0.22f, lengthRatio = 0.64f, curve = 0f, wave = 0f,
        singleEdge = false, twin = false, motif = BladeMotif.PLAIN, guard = GuardStyle.BAR,
    )

    WeaponFamily.RAPIER -> BladeSpec(
        widthRatio = 0.045f, lengthRatio = 0.66f, curve = 0f, wave = 0f,
        singleEdge = false, twin = false, motif = BladeMotif.PLAIN, guard = GuardStyle.WING,
    )

    WeaponFamily.TWIN -> BladeSpec(
        widthRatio = 0.085f, lengthRatio = 0.44f, curve = 0.08f, wave = 0f,
        singleEdge = true, twin = true, motif = BladeMotif.PLAIN, guard = GuardStyle.BAR,
    )

    WeaponFamily.DEMON -> BladeSpec(
        widthRatio = 0.15f, lengthRatio = 0.58f, curve = 0f, wave = 0f,
        singleEdge = false, twin = false, motif = BladeMotif.THORNS, guard = GuardStyle.WING,
    )

    WeaponFamily.HOLY -> BladeSpec(
        widthRatio = 0.16f, lengthRatio = 0.60f, curve = 0f, wave = 0f,
        singleEdge = false, twin = false, motif = BladeMotif.FULLER, guard = GuardStyle.CROSS,
    )

    WeaponFamily.DRAGON -> BladeSpec(
        widthRatio = 0.17f, lengthRatio = 0.62f, curve = 0.10f, wave = 0.045f,
        singleEdge = true, twin = false, motif = BladeMotif.SCALES, guard = GuardStyle.WING,
    )
}

fun tierStyleOf(tier: WeaponTier): TierStyle = when (tier) {
    WeaponTier.RUSTY -> TierStyle(
        Color(0xFF7C7468), Color(0xFF9A9184), Color(0xFF5E5346), Color(0xFF6B6257), 0f,
    )

    WeaponTier.STEEL -> TierStyle(
        Color(0xFFB2BAC4), Color(0xFFDDE4EC), Color(0xFF7A6A4E), Color(0xFF8A8F98), 0f,
    )

    WeaponTier.SILVER -> TierStyle(
        Color(0xFFD5DCE6), Color(0xFFF4F8FF), Color(0xFFA8935F), Color(0xFFBFD0E4), 0f,
    )

    WeaponTier.RUNE -> TierStyle(
        Color(0xFF8FC9D6), Color(0xFFCDF2FA), Color(0xFF4E6E86), Color(0xFF5FD2E8), 0f,
    )

    WeaponTier.FLAME -> TierStyle(
        Color(0xFFE08240), Color(0xFFFFC98A), Color(0xFF7A3A1C), Color(0xFFFF7A3C), 0f,
    )

    WeaponTier.THUNDER -> TierStyle(
        Color(0xFFE8D556), Color(0xFFFFF6B8), Color(0xFF6E5E1E), Color(0xFFFFE94A), 0.28f,
    )

    WeaponTier.DAWN -> TierStyle(
        Color(0xFFF3EEDA), Color(0xFFFFFFFF), Color(0xFFC0A86A), Color(0xFFFFF3B0), 0.34f,
    )

    WeaponTier.BLACK_DRAGON -> TierStyle(
        Color(0xFF6B45A8), Color(0xFFC79BFF), Color(0xFF2E1B4A), Color(0xFFA05CFF), 0.42f,
    )

    WeaponTier.DRAGON_SCALE -> TierStyle(
        Color(0xFFD8497E), Color(0xFFFFA8C6), Color(0xFF5E1730), Color(0xFFFF5C93), 0.50f,
    )

    WeaponTier.ABYSS -> TierStyle(
        Color(0xFF3A46B0), Color(0xFF8A96FF), Color(0xFF141A48), Color(0xFF5666FF), 0.58f,
    )

    WeaponTier.NAMELESS -> TierStyle(
        Color(0xFFF2F2F2), Color(0xFFFFFFFF), Color(0xFF9A9A9A), Color(0xFFFFFFFF), 0.68f,
    )
}

/**
 * 검 한 자루를 그린다.
 *
 * 겹치는 순서: 오라 → 날 → 코등이 → 손잡이 → 손잡이 끝 보석.
 * 앞의 것이 뒤의 것에 가려지므로 순서를 바꾸면 안 된다.
 */
fun DrawScope.drawSword(family: WeaponFamily, level: Int) {
    val tier = WeaponCatalog.tierFor(level)
    val spec = bladeSpecOf(family)
    val style = tierStyleOf(tier)

    val w = size.width
    val h = size.height
    val cx = w / 2f

    if (style.auraAlpha > 0f) {
        drawCircle(
            color = style.gem.copy(alpha = style.auraAlpha * 0.45f),
            radius = w * 0.44f,
            center = Offset(cx, h * 0.40f),
        )
        drawCircle(
            color = style.edge.copy(alpha = style.auraAlpha * 0.25f),
            radius = w * 0.30f,
            center = Offset(cx, h * 0.38f),
        )
    }

    val bladeTop = h * 0.06f
    val bladeBottom = bladeTop + h * spec.lengthRatio

    if (spec.twin) {
        // 두 자루가 X 로 겹친다. 뒤쪽을 먼저 그려 앞쪽이 위로 오게 한다.
        rotate(degrees = 16f, pivot = Offset(cx, h * 0.6f)) {
            drawBlade(spec, style, cx, bladeTop, bladeBottom, w)
        }
        rotate(degrees = -16f, pivot = Offset(cx, h * 0.6f)) {
            drawBlade(spec, style, cx, bladeTop, bladeBottom, w)
        }
    } else {
        drawBlade(spec, style, cx, bladeTop, bladeBottom, w)
    }

    drawGuard(spec, style, cx, bladeBottom, w, h)
    drawGrip(style, cx, bladeBottom, w, h)
}

private fun DrawScope.drawBlade(
    spec: BladeSpec,
    style: TierStyle,
    cx: Float,
    top: Float,
    bottom: Float,
    w: Float,
) {
    val half = w * spec.widthRatio / 2f
    val curveShift = w * spec.curve
    val steps = 24

    // 날의 좌우 윤곽을 위에서 아래로 훑으며 만든다.
    // t=0 이 칼끝, t=1 이 코등이 쪽이다.
    fun centerAt(t: Float): Float {
        val bend = curveShift * t * t
        val ripple = if (spec.wave == 0f) 0f else sin(t * 3.1f) * w * spec.wave
        return cx + bend + ripple
    }

    fun halfWidthAt(t: Float): Float = when {
        t < 0.18f -> half * (t / 0.18f) // 칼끝으로 갈수록 좁아진다
        else -> half
    }

    val path = Path()
    // 왼쪽 윤곽 (칼끝 → 아래)
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val y = top + (bottom - top) * t
        val x = centerAt(t) - halfWidthAt(t)
        if (i == 0) path.moveTo(centerAt(0f), top) else path.lineTo(x, y)
    }
    // 오른쪽 윤곽 (아래 → 칼끝)
    for (i in steps downTo 0) {
        val t = i / steps.toFloat()
        val y = top + (bottom - top) * t
        val backThickness = if (spec.singleEdge) 1.35f else 1f
        path.lineTo(centerAt(t) + halfWidthAt(t) * backThickness, y)
    }
    path.close()

    drawPath(path, color = style.blade)

    // 날끝 하이라이트 — 한쪽날은 날이 서는 쪽에만 넣는다
    val edgePath = Path()
    for (i in 0..steps) {
        val t = i / steps.toFloat()
        val y = top + (bottom - top) * t
        val x = centerAt(t) - halfWidthAt(t)
        if (i == 0) edgePath.moveTo(x, y) else edgePath.lineTo(x, y)
    }
    drawPath(edgePath, color = style.edge, style = Stroke(width = w * 0.012f))

    when (spec.motif) {
        BladeMotif.PLAIN -> Unit

        BladeMotif.FULLER -> {
            // 가운데 홈
            drawLine(
                color = style.guard.copy(alpha = 0.55f),
                start = Offset(centerAt(0.25f), top + (bottom - top) * 0.25f),
                end = Offset(centerAt(0.95f), top + (bottom - top) * 0.95f),
                strokeWidth = w * 0.02f,
            )
        }

        BladeMotif.THORNS -> {
            // 양옆으로 돋은 가시
            listOf(0.42f, 0.62f, 0.82f).forEach { t ->
                val y = top + (bottom - top) * t
                val c = centerAt(t)
                val hw = halfWidthAt(t)
                val spike = w * 0.075f
                drawPath(
                    Path().apply {
                        moveTo(c - hw, y - w * 0.02f)
                        lineTo(c - hw - spike, y + w * 0.03f)
                        lineTo(c - hw, y + w * 0.05f)
                        close()
                    },
                    color = style.edge,
                )
                drawPath(
                    Path().apply {
                        moveTo(c + hw, y - w * 0.02f)
                        lineTo(c + hw + spike, y + w * 0.03f)
                        lineTo(c + hw, y + w * 0.05f)
                        close()
                    },
                    color = style.edge,
                )
            }
        }

        BladeMotif.SCALES -> {
            // 겹치는 비늘
            var t = 0.30f
            while (t < 0.95f) {
                val y = top + (bottom - top) * t
                drawArc(
                    color = style.edge.copy(alpha = 0.5f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerAt(t) - halfWidthAt(t) * 0.7f, y),
                    size = Size(halfWidthAt(t) * 1.4f, halfWidthAt(t) * 1.1f),
                    style = Stroke(width = w * 0.008f),
                )
                t += 0.10f
            }
        }
    }
}

private fun DrawScope.drawGuard(
    spec: BladeSpec,
    style: TierStyle,
    cx: Float,
    bladeBottom: Float,
    w: Float,
    h: Float,
) {
    val thickness = h * 0.035f
    when (spec.guard) {
        GuardStyle.BAR -> drawRect(
            color = style.guard,
            topLeft = Offset(cx - w * 0.19f, bladeBottom),
            size = Size(w * 0.38f, thickness),
        )

        GuardStyle.CROSS -> {
            drawRect(
                color = style.guard,
                topLeft = Offset(cx - w * 0.26f, bladeBottom),
                size = Size(w * 0.52f, thickness),
            )
            drawRect(
                color = style.guard,
                topLeft = Offset(cx - w * 0.05f, bladeBottom - h * 0.02f),
                size = Size(w * 0.10f, thickness + h * 0.03f),
            )
        }

        GuardStyle.WING -> {
            drawRect(
                color = style.guard,
                topLeft = Offset(cx - w * 0.14f, bladeBottom),
                size = Size(w * 0.28f, thickness),
            )
            // 위로 젖혀진 날개
            drawPath(
                Path().apply {
                    moveTo(cx - w * 0.14f, bladeBottom + thickness)
                    lineTo(cx - w * 0.30f, bladeBottom - h * 0.03f)
                    lineTo(cx - w * 0.14f, bladeBottom)
                    close()
                },
                color = style.guard,
            )
            drawPath(
                Path().apply {
                    moveTo(cx + w * 0.14f, bladeBottom + thickness)
                    lineTo(cx + w * 0.30f, bladeBottom - h * 0.03f)
                    lineTo(cx + w * 0.14f, bladeBottom)
                    close()
                },
                color = style.guard,
            )
        }
    }
}

private fun DrawScope.drawGrip(
    style: TierStyle,
    cx: Float,
    bladeBottom: Float,
    w: Float,
    h: Float,
) {
    val gripTop = bladeBottom + h * 0.04f
    val gripHeight = h * 0.20f

    drawRect(
        color = Color(0xFF4A3A28),
        topLeft = Offset(cx - w * 0.045f, gripTop),
        size = Size(w * 0.09f, gripHeight),
    )
    // 감은 자국
    var y = gripTop + gripHeight * 0.15f
    while (y < gripTop + gripHeight * 0.9f) {
        drawLine(
            color = Color(0xFF32261A),
            start = Offset(cx - w * 0.045f, y),
            end = Offset(cx + w * 0.045f, y),
            strokeWidth = h * 0.006f,
        )
        y += gripHeight * 0.18f
    }

    // 손잡이 끝 보석
    translate(top = 0f) {
        drawCircle(
            color = style.gem,
            radius = w * 0.05f,
            center = Offset(cx, gripTop + gripHeight + h * 0.015f),
        )
        drawCircle(
            color = style.edge.copy(alpha = 0.7f),
            radius = w * 0.02f,
            center = Offset(cx - w * 0.012f, gripTop + gripHeight + h * 0.008f),
        )
    }
}
