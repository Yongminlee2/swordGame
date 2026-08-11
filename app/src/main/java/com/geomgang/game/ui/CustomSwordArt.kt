package com.geomgang.game.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.IntOffset
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import com.geomgang.game.R

/**
 * 강화 단계별 전용 검 그림.
 *
 * 기본·조합 계열은 7행 × 21열, 전설은 +21~+50을 한 줄에 둔다.
 * 모든 칸은 서로 다른 원본 실루엣을 사용하므로 강화 직후 검날·가드·장식 변화가
 * 작은 화면에서도 바로 보인다.
 */
object CustomSwordArt {

    const val CELL = 128
    private const val FAMILY_COLUMNS = 21
    private const val LEGEND_MIN_LEVEL = 21
    private const val LEGEND_MAX_LEVEL = 50

    private val familyRows = mapOf(
        WeaponFamily.STRAIGHT to 0,
        WeaponFamily.CURVED to 1,
        WeaponFamily.GREAT to 2,
        WeaponFamily.RAPIER to 3,
        WeaponFamily.DEMON to 4,
        WeaponFamily.HOLY to 5,
        WeaponFamily.DRAGON to 6,
    )

    fun has(sword: Sword): Boolean =
        sword.uniqueId == null && has(sword.family, sword.level)

    fun has(family: WeaponFamily?, level: Int): Boolean = when {
        level >= LEGEND_MIN_LEVEL -> true
        family == null -> false
        else -> level >= 0 && family in familyRows
    }

    fun sourceFor(family: WeaponFamily?, level: Int): CustomSwordSource {
        require(has(family, level)) { "custom sword art is unavailable: $family +$level" }
        return if (level >= LEGEND_MIN_LEVEL) {
            val column = (level.coerceAtMost(LEGEND_MAX_LEVEL) - LEGEND_MIN_LEVEL)
            CustomSwordSource(
                drawable = R.drawable.sword_custom_legend_sheet,
                cell = column,
                columns = LEGEND_MAX_LEVEL - LEGEND_MIN_LEVEL + 1,
            )
        } else {
            val row = requireNotNull(familyRows[family])
            CustomSwordSource(
                drawable = R.drawable.sword_custom_family_sheet,
                cell = row * FAMILY_COLUMNS + level.coerceIn(0, FAMILY_COLUMNS - 1),
                columns = FAMILY_COLUMNS,
            )
        }
    }

    fun offsetOf(source: CustomSwordSource): IntOffset =
        IntOffset(
            (source.cell % source.columns) * CELL,
            (source.cell / source.columns) * CELL,
        )
}

data class CustomSwordSource(
    @DrawableRes val drawable: Int,
    val cell: Int,
    val columns: Int,
)
