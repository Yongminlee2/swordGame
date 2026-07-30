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

    /**
     * 시트 15행(고유검)의 열 순서 — **그림이 그려진 순서라 바꿀 수 없다.**
     *
     * 예전에는 `RECIPES` 선언 순서를 썼는데, v2.1에서 레시피가 6종으로 줄고
     * 매칭 우선순위 때문에 순서도 바뀌었다. 목록 순서에 묶어 두면 레시피를 만질
     * 때마다 그림이 엉뚱한 검에 붙는다. 열은 그림의 사실이므로 여기 굳힌다.
     */
    private val UNIQUE_COLUMNS = listOf(
        "abyss_eater", "trinity", "dragon_fang", "phoenix", "cleaver",
        "tempest", "lucky", "origin", "glutton", "bloom",
    )

    /** 고유검 칸 번호. */
    fun uniqueCellOf(uniqueId: String): Int {
        val index = UNIQUE_COLUMNS.indexOf(uniqueId)
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
