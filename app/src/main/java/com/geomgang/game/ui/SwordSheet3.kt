package com.geomgang.game.ui

import androidx.compose.ui.unit.IntOffset
import com.geomgang.core.Sword
import com.geomgang.core.UniqueSwords
import com.geomgang.core.WeaponFamily

/**
 * 검 그림 3 — **단계마다 다른 그림.**
 *
 * sword_sheet3.png 는 64px 칸 21열 15행이다.
 * - 행 0~13 = 계열(WeaponFamily 선언 순서), 열 0~20 = 강화 단계 +0~+20
 * - 행 14 = 전설 20칸, +21~+40 구간이 한 단계마다 하나씩 쓴다
 * - +41 이상은 마지막 전설 칸을 쓰고 오라만 짙어진다 (상한 없는 구간을 그림으로 좇을 수 없다)
 *
 * 칸 배치를 바꾸면 tools/make_sword_sheet3.ps1 과 함께 바꿔야 한다.
 *
 * 고유검은 [SwordSheet2] 의 전용 칸을 그대로 쓴다 — 고유검은 단계와 무관하게
 * 자기 그림을 가져야 하기 때문이다.
 *
 * 출처: Dungeon Crawl 계열 무기 타일(CC0)을 계열·단계별로 선별·배치한 모음.
 * 표기는 설정 → 라이선스 화면에 있다.
 */
object SwordSheet3 {

    const val CELL = 64
    const val COLUMNS = 21

    /** 계열 행이 덮는 마지막 강화 단계. */
    const val MAX_LEVEL_ROW = 20

    /** 전설 칸이 놓인 행. */
    private const val LEGEND_ROW = 14

    /** 전설 칸 수. +21 부터 한 단계에 하나씩 쓴다. */
    const val LEGEND_COUNT = 20

    /** 전설 칸이 덮는 마지막 단계. 그 위는 마지막 칸을 계속 쓴다. */
    const val LEGEND_MAX_LEVEL = MAX_LEVEL_ROW + LEGEND_COUNT

    /** 계열 × 단계 칸. 단계는 0..20 으로 잘린다. */
    fun cellOf(family: WeaponFamily, level: Int): Int {
        require(level >= 0) { "level must be >= 0, was $level" }
        val col = level.coerceAtMost(MAX_LEVEL_ROW)
        return family.ordinal * COLUMNS + col
    }

    /** 무한 구간(+21~)의 전설 칸. +41 이상은 마지막 칸을 쓴다. */
    fun legendCellOf(level: Int): Int {
        require(level > MAX_LEVEL_ROW) { "legend starts above $MAX_LEVEL_ROW, was $level" }
        val index = (level - MAX_LEVEL_ROW - 1).coerceAtMost(LEGEND_COUNT - 1)
        return LEGEND_ROW * COLUMNS + index
    }

    /** 이 단계가 전설 칸을 쓰는지. */
    fun isLegend(level: Int): Boolean = level > MAX_LEVEL_ROW

    fun offsetOf(cell: Int): IntOffset =
        IntOffset((cell % COLUMNS) * CELL, (cell / COLUMNS) * CELL)

    /**
     * 검 한 자루가 쓸 시트와 칸.
     *
     * 고유검은 시트2, 나머지는 시트3다. 어느 시트를 쓰는지까지 여기서 정해 주면
     * 화면은 분기 없이 그리기만 한다.
     */
    fun sourceFor(sword: Sword): SwordSource {
        sword.uniqueId?.let { id ->
            if (UniqueSwords.byId(id) != null) {
                return SwordSource(useSheet3 = false, cell = SwordSheet2.uniqueCellOf(id))
            }
        }
        val cell = if (isLegend(sword.level)) {
            legendCellOf(sword.level)
        } else {
            cellOf(sword.family, sword.level)
        }
        return SwordSource(useSheet3 = true, cell = cell)
    }
}

/** 검 하나를 그리는 데 필요한 시트 선택과 칸 번호. */
data class SwordSource(val useSheet3: Boolean, val cell: Int)
