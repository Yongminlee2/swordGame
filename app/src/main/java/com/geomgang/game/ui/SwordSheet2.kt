package com.geomgang.game.ui

import androidx.compose.ui.unit.IntOffset
import com.geomgang.core.Sword
import com.geomgang.core.UniqueSwords
import com.geomgang.core.WeaponCatalog
import com.geomgang.core.WeaponFamily
import com.geomgang.core.WeaponTier

/**
 * 검 그림 2 - 큰 화면용 64px 시트.
 *
 * sword_sheet2.png 는 64px 칸 11열 15행이다.
 * 행 0~13 = 계열(WeaponFamily 선언 순서), 열 0~10 = 티어(WeaponTier 선언 순서),
 * 행 14 = 고유검(UniqueSwords.RECIPES 선언 순서, 열 0~9).
 * 칸 배치를 바꾸면 tools/make_sword_sheet2.ps1 과 함께 바꿔야 한다.
 *
 * 기존 16px 시트(SwordSheet)는 보관함 썸네일에 남는다 - 작은 크기에선 도트가 잘 읽힌다.
 *
 * 출처: Dungeon Crawl 아이템 타일(CC0)을 사용자가 계열×티어로 큐레이션한 모음.
 * 표기는 설정 → 라이선스 화면에 있다.
 */
object SwordSheet2 {

    const val CELL = 64
    const val COLUMNS = 11

    private const val UNIQUE_ROW = 14

    /** 계열×티어 칸 번호. */
    fun cellOf(family: WeaponFamily, tier: WeaponTier): Int =
        family.ordinal * COLUMNS + tier.ordinal

    /** 고유검 칸 번호. 레시피 선언 순서가 곧 열이다. */
    fun uniqueCellOf(uniqueId: String): Int {
        val index = UniqueSwords.RECIPES.indexOfFirst { it.id == uniqueId }
        require(index >= 0) { "unknown unique: $uniqueId" }
        return UNIQUE_ROW * COLUMNS + index
    }

    /** 검 한 자루가 쓸 칸. 고유검이 최우선이다. */
    fun cellFor(sword: Sword): Int {
        sword.uniqueId?.let { return uniqueCellOf(it) }
        return cellOf(sword.family, WeaponCatalog.tierFor(sword.level))
    }

    fun offsetOf(cell: Int): IntOffset =
        IntOffset((cell % COLUMNS) * CELL, (cell / COLUMNS) * CELL)
}
