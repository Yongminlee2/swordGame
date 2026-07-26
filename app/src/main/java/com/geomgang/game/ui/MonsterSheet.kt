package com.geomgang.game.ui

import androidx.compose.ui.unit.IntOffset
import com.geomgang.core.Zone

/**
 * 몬스터 그림의 단일 출처.
 *
 * monster_sheet.png 는 32px 칸 8열 시트다. 0~29 잡몹(구역 순서×3), 30~39 보스(구역 순서),
 * 40~55 예약(펫). 칸 배치를 바꾸면 여기와 tools/monster_cells.txt 를 함께 바꿔야 한다.
 *
 * 출처: Dungeon Crawl 32x32 tiles — Dungeon Crawl Stone Soup 팀, CC0.
 * 표기는 설정 → 라이선스 화면에 있다.
 */
object MonsterSheet {

    const val CELL = 32
    const val COLUMNS = 8

    /** 구역 순서가 곧 칸 순서다 - 이름 목록을 Zone 에서 뽑아 표를 만든다. */
    private val cells: Map<String, Int> = buildMap {
        var mob = 0
        for (zone in Zone.entries) {
            for (m in zone.monsters) put(m.name, mob++)
        }
        var boss = 30
        for (zone in Zone.entries) put(zone.bossName, boss++)
    }

    fun hasCell(name: String): Boolean = name in cells

    fun cellOf(name: String): Int =
        requireNotNull(cells[name]) { "unknown monster: $name" }

    fun offsetOf(cell: Int): IntOffset =
        IntOffset((cell % COLUMNS) * CELL, (cell / COLUMNS) * CELL)
}
