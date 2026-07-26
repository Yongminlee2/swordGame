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
    fun `잡몹은 0-29 보스는 30-39 칸을 쓴다`() {
        for (zone in Zone.entries) {
            for (m in zone.monsters) {
                assertTrue("${m.name}=${MonsterSheet.cellOf(m.name)}", MonsterSheet.cellOf(m.name) in 0..29)
            }
            assertTrue(
                "${zone.bossName}=${MonsterSheet.cellOf(zone.bossName)}",
                MonsterSheet.cellOf(zone.bossName) in 30..39,
            )
        }
    }

    @Test
    fun `좌표는 시트 범위 안이다`() {
        for (cell in 0 until 40) {
            val o = MonsterSheet.offsetOf(cell)
            assertTrue(o.x in 0 until MonsterSheet.CELL * MonsterSheet.COLUMNS)
            assertTrue(o.y >= 0)
        }
    }

    @Test
    fun `펫은 40-49 칸을 쓰고 겹치지 않는다`() {
        val cells = com.geomgang.core.PetKind.entries.map { MonsterSheet.petCellOf(it.id) }
        assertEquals(cells.size, cells.toSet().size)
        assertTrue(cells.all { it in 40..49 })
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
