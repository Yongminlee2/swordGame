package com.geomgang.game

import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import com.geomgang.game.ui.SwordSheet2
import com.geomgang.game.ui.SwordSheet3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwordSheet3Test {

    @Test
    fun `계열 14 곱하기 단계 21 칸이 전부 겹치지 않는다`() {
        val cells = WeaponFamily.entries.flatMap { f ->
            (0..SwordSheet3.MAX_LEVEL_ROW).map { SwordSheet3.cellOf(f, it) }
        }
        assertEquals(294, cells.size)
        assertEquals(294, cells.toSet().size)
    }

    @Test
    fun `단계마다 칸이 다르다`() {
        val cells = (0..SwordSheet3.MAX_LEVEL_ROW).map {
            SwordSheet3.cellOf(WeaponFamily.STRAIGHT, it)
        }
        assertEquals(21, cells.toSet().size)
    }

    @Test
    fun `20단계까지는 계열 행을 쓴다`() {
        for (level in 0..SwordSheet3.MAX_LEVEL_ROW) {
            assertFalse("+$level", SwordSheet3.isLegend(level))
        }
        assertTrue(SwordSheet3.isLegend(SwordSheet3.MAX_LEVEL_ROW + 1))
    }

    @Test
    fun `무한 구간은 전설 칸을 순서대로 쓴다`() {
        val first = SwordSheet3.legendCellOf(21)
        val second = SwordSheet3.legendCellOf(22)
        assertEquals(first + 1, second)
        assertEquals(20, (21..40).map { SwordSheet3.legendCellOf(it) }.toSet().size)
    }

    @Test
    fun `41 이상은 마지막 전설 칸을 계속 쓴다`() {
        val last = SwordSheet3.legendCellOf(SwordSheet3.LEGEND_MAX_LEVEL)
        assertEquals(last, SwordSheet3.legendCellOf(41))
        assertEquals(last, SwordSheet3.legendCellOf(60))
        assertEquals(last, SwordSheet3.legendCellOf(999))
    }

    @Test
    fun `전설 칸은 계열 칸과 겹치지 않는다`() {
        val familyCells = WeaponFamily.entries.flatMap { f ->
            (0..SwordSheet3.MAX_LEVEL_ROW).map { SwordSheet3.cellOf(f, it) }
        }.toSet()
        for (level in 21..40) {
            assertFalse("+$level", SwordSheet3.legendCellOf(level) in familyCells)
        }
    }

    @Test
    fun `좌표는 시트 안이다`() {
        val all = WeaponFamily.entries.flatMap { f ->
            (0..SwordSheet3.MAX_LEVEL_ROW).map { SwordSheet3.cellOf(f, it) }
        } + (21..60).map { SwordSheet3.legendCellOf(it) }
        for (cell in all) {
            val o = SwordSheet3.offsetOf(cell)
            assertTrue(o.x in 0 until SwordSheet3.CELL * SwordSheet3.COLUMNS)
            assertTrue(o.y in 0 until SwordSheet3.CELL * 15)
        }
    }

    @Test
    fun `고유검은 시트2의 전용 칸을 쓴다`() {
        val unique = Sword(WeaponFamily.HOLY, 12, uniqueId = "trinity")
        val src = SwordSheet3.sourceFor(unique)
        assertFalse(src.useSheet3)
        assertEquals(SwordSheet2.uniqueCellOf("trinity"), src.cell)
    }

    @Test
    fun `평범한 검은 시트3의 단계 칸을 쓴다`() {
        val plain = Sword(WeaponFamily.HOLY, 12)
        val src = SwordSheet3.sourceFor(plain)
        assertTrue(src.useSheet3)
        assertEquals(SwordSheet3.cellOf(WeaponFamily.HOLY, 12), src.cell)
    }

    @Test
    fun `무한 구간 검은 전설 칸을 쓴다`() {
        val src = SwordSheet3.sourceFor(Sword(WeaponFamily.STRAIGHT, 30))
        assertTrue(src.useSheet3)
        assertEquals(SwordSheet3.legendCellOf(30), src.cell)
    }
}
