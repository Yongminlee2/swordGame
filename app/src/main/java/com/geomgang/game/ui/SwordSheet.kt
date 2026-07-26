package com.geomgang.game.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.geomgang.core.WeaponFamily

/**
 * 검 그림의 단일 출처.
 *
 * 단색 실루엣 아이콘을 버리고 **색이 들어간 16비트 픽셀아트**로 바꿨다.
 * 스프라이트시트 하나(48×480, 7KB)에 검 30종 × 낡음 3단계 = 90칸이 들어 있고,
 * 필요한 칸만 잘라 그린다. 파일을 90개로 쪼개지 않으므로 리소스가 늘지 않는다.
 *
 * 출처: The Humble Sword Pack — The Wise Hedgehog, CC BY 4.0.
 * 표기는 설정 → 라이선스 화면에 있다.
 *
 * 다른 에셋으로 갈아 끼우려면 이 파일의 표만 바꾸면 된다.
 */
object SwordSheet {

    /** 한 칸의 크기(px). */
    const val CELL = 16

    /** 시트의 열 수. 0=새것, 1=쓴것, 2=낡은것. */
    private const val COLUMNS = 3

    /** 시트의 행 수. 검 종류다. */
    private const val ROWS = 30

    val cellSize = IntSize(CELL, CELL)

    /**
     * 계열마다 쓰는 검 종류(행)의 묶음. 인덱스는 `단계 / 2` 다.
     *
     * 손으로 고르지 않고 `(인덱스 × 8 + 계열번호) % 30` 으로 뽑았다.
     * 계열이 8종이고 시트가 30행이라, 이렇게 두면 **같은 인덱스에서 여덟 계열이
     * 연속된 서로 다른 행**을 쓰게 되어 겹칠 수가 없다.
     * 손으로 골랐을 때는 실제로 겹쳐서 테스트가 잡아냈다.
     */
    private val ROWS_BY_FAMILY: Map<WeaponFamily, IntArray> = mapOf(
        WeaponFamily.STRAIGHT to intArrayOf(0, 8, 16, 24, 2, 10, 18, 26, 4, 12),
        WeaponFamily.CURVED to intArrayOf(1, 9, 17, 25, 3, 11, 19, 27, 5, 13),
        WeaponFamily.GREAT to intArrayOf(2, 10, 18, 26, 4, 12, 20, 28, 6, 14),
        WeaponFamily.RAPIER to intArrayOf(3, 11, 19, 27, 5, 13, 21, 29, 7, 15),
        WeaponFamily.TWIN to intArrayOf(4, 12, 20, 28, 6, 14, 22, 0, 8, 16),
        WeaponFamily.DEMON to intArrayOf(5, 13, 21, 29, 7, 15, 23, 1, 9, 17),
        WeaponFamily.HOLY to intArrayOf(6, 14, 22, 0, 8, 16, 24, 2, 10, 18),
        WeaponFamily.DRAGON to intArrayOf(7, 15, 23, 1, 9, 17, 25, 3, 11, 19),
    )

    /**
     * 이 계열·단계에 해당하는 칸의 좌상단 좌표(px).
     *
     * 낡음 단계는 저단계에서만 쓴다. "녹슨 쇠칼"이 반짝이면 이름과 어긋난다.
     */
    fun spriteOffset(family: WeaponFamily, level: Int): IntOffset {
        require(level >= 0) { "level must be >= 0, was $level" }
        val set = ROWS_BY_FAMILY.getValue(family)
        val row = set[(level / 2).coerceAtMost(set.lastIndex)] % ROWS
        val column = when (level) {
            0 -> 2 // 낡은 것
            1, 2 -> 1 // 쓴 것
            else -> 0 // 새것
        }.coerceAtMost(COLUMNS - 1)
        return IntOffset(column * CELL, row * CELL)
    }

    /**
     * 단계에 따라 검 뒤에 깔리는 빛.
     *
     * 스프라이트에 이미 색이 있으므로 색을 덮지 않는다. 오라만 얹는다.
     */
    fun auraFor(level: Int): SwordAura = when {
        level <= 8 -> SwordAura(Color.Transparent, 0f)
        level <= 11 -> SwordAura(Color(0xFF6FD3E8), 0.16f)
        level <= 14 -> SwordAura(Color(0xFFFF8A3C), 0.24f)
        level <= 16 -> SwordAura(Color(0xFFFFE94A), 0.34f)
        level <= 18 -> SwordAura(Color(0xFFFFF6C8), 0.44f)
        level <= 20 -> SwordAura(Color(0xFFA05CFF), 0.56f)
        level <= 25 -> SwordAura(Color(0xFFFF5C93), 0.66f)
        level <= 30 -> SwordAura(Color(0xFF5666FF), 0.76f)
        else -> SwordAura(Color.White, 0.88f)
    }

    /** 테스트가 쓰는 값. */
    internal fun rowsByFamily(): Map<WeaponFamily, IntArray> = ROWS_BY_FAMILY

    internal fun rows(): Int = ROWS

    internal fun columns(): Int = COLUMNS
}

/** 검 뒤에 깔리는 빛. */
data class SwordAura(val color: Color, val alpha: Float)
