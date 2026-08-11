package com.geomgang.game

import com.geomgang.core.Sword
import com.geomgang.core.WeaponFamily
import com.geomgang.game.ui.CustomSwordArt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSwordArtTest {

    private val customFamilies = listOf(
        WeaponFamily.STRAIGHT,
        WeaponFamily.CURVED,
        WeaponFamily.GREAT,
        WeaponFamily.RAPIER,
        WeaponFamily.DEMON,
        WeaponFamily.HOLY,
        WeaponFamily.DRAGON,
    )

    @Test
    fun `선택한 일곱 계열은 0부터 20까지 단계마다 다른 칸을 쓴다`() {
        for (family in customFamilies) {
            val sources = (0..20).map { CustomSwordArt.sourceFor(family, it) }
            assertEquals(family.name, 21, sources.map { it.cell }.toSet().size)
            assertEquals(family.name, 1, sources.map { it.drawable }.toSet().size)
        }
    }

    @Test
    fun `전설 21부터 50까지 단계마다 다른 칸을 쓴다`() {
        val sources = (21..50).map { CustomSwordArt.sourceFor(null, it) }
        assertEquals(30, sources.map { it.cell }.toSet().size)
        assertEquals(1, sources.map { it.drawable }.toSet().size)
    }

    @Test
    fun `50 위는 최종 전설 그림을 유지한다`() {
        val final = CustomSwordArt.sourceFor(null, 50)
        assertEquals(final, CustomSwordArt.sourceFor(WeaponFamily.DRAGON, 51))
        assertEquals(final, CustomSwordArt.sourceFor(WeaponFamily.STRAIGHT, 999))
    }

    @Test
    fun `고유검과 미제작 계열은 기존 그림으로 보낸다`() {
        assertFalse(CustomSwordArt.has(Sword(WeaponFamily.HOLY, 12, uniqueId = "trinity")))
        assertFalse(CustomSwordArt.has(WeaponFamily.TWIN, 12))
        assertTrue(CustomSwordArt.has(WeaponFamily.TWIN, 21))
    }

    @Test
    fun `모든 좌표가 128 픽셀 격자에 맞는다`() {
        val sources = customFamilies.flatMap { family ->
            (0..20).map { CustomSwordArt.sourceFor(family, it) }
        } + (21..50).map { CustomSwordArt.sourceFor(null, it) }

        for (source in sources) {
            val offset = CustomSwordArt.offsetOf(source)
            assertEquals(0, offset.x % CustomSwordArt.CELL)
            assertEquals(0, offset.y % CustomSwordArt.CELL)
        }
    }
}
