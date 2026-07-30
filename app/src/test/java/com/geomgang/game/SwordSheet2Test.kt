package com.geomgang.game

import com.geomgang.core.Sword
import com.geomgang.core.UniqueSwords
import com.geomgang.core.WeaponFamily
import com.geomgang.core.WeaponTier
import com.geomgang.game.ui.SwordSheet2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwordSheet2Test {

    @Test
    fun `계열x티어 154칸이 전부 겹치지 않는다`() {
        val cells = WeaponFamily.entries.flatMap { f ->
            WeaponTier.entries.map { t -> SwordSheet2.cellOf(f, t) }
        }
        assertEquals(154, cells.size)
        assertEquals(154, cells.toSet().size)
    }

    @Test
    fun `고유검이 전부 자기 칸을 갖고 겹치지 않는다`() {
        val cells = UniqueSwords.RECIPES.map { SwordSheet2.uniqueCellOf(it.id) }
        assertEquals(UniqueSwords.RECIPES.size, cells.toSet().size)
        // 고유검 칸은 계열x티어 칸과 겹치지 않는다 (행 14)
        val familyCells = WeaponFamily.entries.flatMap { f ->
            WeaponTier.entries.map { t -> SwordSheet2.cellOf(f, t) }
        }.toSet()
        assertTrue(cells.none { it in familyCells })
    }

    @Test
    fun `고유검이면 계열과 무관하게 고유 칸을 쓴다`() {
        val unique = Sword(WeaponFamily.HOLY, 12, uniqueId = "trinity")
        assertEquals(SwordSheet2.uniqueCellOf("trinity"), SwordSheet2.cellFor(unique))
        val plain = Sword(WeaponFamily.HOLY, 12)
        assertTrue(SwordSheet2.cellFor(plain) != SwordSheet2.cellFor(unique))
    }

    @Test
    fun `좌표는 시트 안이다`() {
        val maxCell = SwordSheet2.cellFor(Sword(WeaponFamily.VOID, 40))
        val all = (0..maxCell) + UniqueSwords.RECIPES.map { SwordSheet2.uniqueCellOf(it.id) }
        for (cell in all) {
            val o = SwordSheet2.offsetOf(cell)
            assertTrue(o.x in 0 until SwordSheet2.CELL * SwordSheet2.COLUMNS)
            assertTrue(o.y in 0 until SwordSheet2.CELL * 15)
        }
    }
}
