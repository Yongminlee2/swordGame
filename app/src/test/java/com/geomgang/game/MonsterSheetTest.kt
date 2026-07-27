package com.geomgang.game

import com.geomgang.core.Zone
import com.geomgang.game.ui.MonsterSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 시트 칸 배치의 계약: 0~29 잡몹(구역 순서 × 몬스터 3), 30~39 보스(구역 순서).
 * tools/monster_cells.txt 가 이 순서로 시트를 만들고, MonsterSheet 가 같은 순서로 읽는다.
 */
class MonsterSheetTest {

    @Test
    fun `모든 구역의 잡몹과 보스 이름에 칸이 있다`() {
        for (zone in Zone.entries) {
            for (m in zone.monsters) {
                assertTrue("${zone.displayName}/${m.name} 누락", MonsterSheet.hasCell(m.name))
            }
            assertTrue("${zone.displayName} 보스 누락", MonsterSheet.hasCell(zone.bossName))
        }
    }

    @Test
    fun `칸 번호는 겹치지 않는다`() {
        val cells = Zone.entries.flatMap { z -> z.monsters.map { MonsterSheet.cellOf(it.name) } } +
            Zone.entries.map { MonsterSheet.cellOf(it.bossName) }
        assertEquals(cells.size, cells.toSet().size)
    }

    @Test
    fun `잡몹 칸이 먼저 오고 보스 칸이 그 뒤다`() {
        val mobCount = Zone.entries.sumOf { it.monsters.size }
        for (zone in Zone.entries) {
            for (m in zone.monsters) {
                assertTrue(
                    "${m.name}=${MonsterSheet.cellOf(m.name)}",
                    MonsterSheet.cellOf(m.name) in 0 until mobCount,
                )
            }
            assertTrue(
                "${zone.bossName}=${MonsterSheet.cellOf(zone.bossName)}",
                MonsterSheet.cellOf(zone.bossName) in mobCount until mobCount + Zone.entries.size,
            )
        }
    }

    @Test
    fun `구역 12개에 몬스터 5종씩 총 60종이다`() {
        assertEquals(12, Zone.entries.size)
        for (zone in Zone.entries) {
            assertEquals("${zone.displayName} 몬스터 수", 5, zone.monsters.size)
        }
        assertEquals(60, Zone.entries.sumOf { it.monsters.size })
    }

    @Test
    fun `좌표는 시트 범위 안이다`() {
        val total = Zone.entries.sumOf { it.monsters.size } + Zone.entries.size +
            com.geomgang.core.PetKind.entries.size
        for (cell in 0 until total) {
            val o = MonsterSheet.offsetOf(cell)
            assertTrue(o.x in 0 until MonsterSheet.CELL * MonsterSheet.COLUMNS)
            assertTrue(o.y >= 0)
        }
    }

    @Test
    fun `펫 칸은 보스 칸 뒤에 오고 겹치지 않는다`() {
        val petCells = com.geomgang.core.PetKind.entries.map { MonsterSheet.petCellOf(it.id) }
        assertEquals(petCells.size, petCells.toSet().size)
        val monsterCells = Zone.entries.flatMap { z ->
            z.monsters.map { MonsterSheet.cellOf(it.name) } + MonsterSheet.cellOf(z.bossName)
        }.toSet()
        assertTrue("펫 칸이 몬스터 칸과 겹친다", petCells.none { it in monsterCells })
    }

    @Test
    fun `모르는 이름은 예외를 던진다`() {
        var thrown = false
        try {
            MonsterSheet.cellOf("없는 몬스터")
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
