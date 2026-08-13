package com.geomgang.game.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.unit.IntOffset
import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import com.geomgang.game.R

/**
 * 강화 단계별 전용 검 그림.
 *
 * 계열마다 원본 크기를 유지한 7열 × 3행 시트를 쓰고, 전설 +21~+50은
 * 10열 × 3행 시트에 둔다. 계열 시트를 나누어 현재 화면에 필요한 그림만 읽는다.
 * 모든 칸은 서로 다른 원본 실루엣을 사용하므로 강화 직후 검날·가드·장식 변화가
 * 작은 화면에서도 바로 보인다.
 */
object CustomSwordArt {

    const val CELL = 328
    private const val FAMILY_ATLAS_COLUMNS = 7
    private const val LEGEND_ATLAS_COLUMNS = 10
    private const val LEGEND_MIN_LEVEL = 21
    private const val LEGEND_MAX_LEVEL = 50

    private val familyDrawables = mapOf(
        WeaponFamily.STRAIGHT to R.drawable.sword_custom_straight_sheet,
        WeaponFamily.CURVED to R.drawable.sword_custom_curved_sheet,
        WeaponFamily.GREAT to R.drawable.sword_custom_great_sheet,
        WeaponFamily.RAPIER to R.drawable.sword_custom_rapier_sheet,
        WeaponFamily.DEMON to R.drawable.sword_custom_demon_sheet,
        WeaponFamily.HOLY to R.drawable.sword_custom_holy_sheet,
        WeaponFamily.DRAGON to R.drawable.sword_custom_dragon_sheet,
    )

    fun has(sword: Sword): Boolean =
        sword.uniqueId == null && has(sword.family, sword.level)

    fun has(family: WeaponFamily?, level: Int): Boolean = when {
        level >= LEGEND_MIN_LEVEL -> true
        family == null -> false
        else -> level >= 0 && family in familyDrawables
    }

    fun sourceFor(family: WeaponFamily?, level: Int): CustomSwordSource {
        require(has(family, level)) { "custom sword art is unavailable: $family +$level" }
        return if (level >= LEGEND_MIN_LEVEL) {
            val column = (level.coerceAtMost(LEGEND_MAX_LEVEL) - LEGEND_MIN_LEVEL)
            CustomSwordSource(
                drawable = R.drawable.sword_custom_legend_sheet,
                cell = column,
                columns = LEGEND_ATLAS_COLUMNS,
            )
        } else {
            CustomSwordSource(
                drawable = requireNotNull(familyDrawables[family]),
                cell = level.coerceIn(0, 20),
                columns = FAMILY_ATLAS_COLUMNS,
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
