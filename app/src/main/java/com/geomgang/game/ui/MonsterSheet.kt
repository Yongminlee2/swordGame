package com.geomgang.game.ui

import androidx.compose.ui.unit.IntOffset
import com.geomgang.core.PetKind
import com.geomgang.core.Zone

/**
 * 몬스터 그림의 단일 출처.
 *
 * monster_sheet.png 는 32px 칸 8열 시트다.
 * 0~59 잡몹(구역 순서 × 몬스터 5), 60~71 보스(구역 순서), 72~81 펫(PetKind 선언 순서).
 * 칸 배치를 바꾸면 여기와 tools/monster_cells.txt 를 함께 바꿔야 한다.
 *
 * 잡몹 칸은 손 표가 아니라 **구역 순회**로 만든다 — 구역이나 몬스터가 늘면 저절로 따라온다.
 * 다만 시트의 칸 순서가 그 순회와 일치해야 하며, 그 계약은 MonsterSheetTest 가 지킨다.
 *
 * 출처: Dungeon Crawl 32x32 tiles — Dungeon Crawl Stone Soup 팀, CC0.
 * 표기는 설정 → 라이선스 화면에 있다.
 */
object MonsterSheet {

    const val CELL = 32
    const val COLUMNS = 8

    /** 보스 칸의 시작. 잡몹 칸(구역 × 5) 바로 뒤다. */
    private val BOSS_BASE = Zone.entries.sumOf { it.monsters.size }

    /** 펫 칸의 시작. 보스 칸 뒤다. */
    private val PET_BASE = BOSS_BASE + Zone.entries.size

    /** 구역 순서가 곧 칸 순서다 - 이름 목록을 Zone 에서 뽑아 표를 만든다. */
    private val cells: Map<String, Int> = buildMap {
        var mob = 0
        for (zone in Zone.entries) {
            for (m in zone.monsters) put(m.name, mob++)
        }
        var boss = BOSS_BASE
        for (zone in Zone.entries) put(zone.bossName, boss++)
    }

    fun petCellOf(petId: String): Int {
        val index = PetKind.entries.indexOfFirst { it.id == petId }
        require(index >= 0) { "unknown pet: $petId" }
        return PET_BASE + index
    }

    fun hasCell(name: String): Boolean = name in cells

    fun cellOf(name: String): Int =
        requireNotNull(cells[name]) { "unknown monster: $name" }

    fun offsetOf(cell: Int): IntOffset =
        IntOffset((cell % COLUMNS) * CELL, (cell / COLUMNS) * CELL)
}
