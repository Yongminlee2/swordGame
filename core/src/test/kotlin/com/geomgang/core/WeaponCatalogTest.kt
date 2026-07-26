package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeaponCatalogTest {

    @Test
    fun `티어는 11종이고 무한 전용은 3종이다`() {
        assertEquals(11, WeaponTier.entries.size)
        assertEquals(3, WeaponTier.entries.count { it.endlessOnly })
    }

    @Test
    fun `단계별 티어 경계가 스펙과 일치한다`() {
        assertEquals(WeaponTier.RUSTY, WeaponCatalog.tierFor(0))
        assertEquals(WeaponTier.RUSTY, WeaponCatalog.tierFor(2))
        assertEquals(WeaponTier.STEEL, WeaponCatalog.tierFor(3))
        assertEquals(WeaponTier.STEEL, WeaponCatalog.tierFor(5))
        assertEquals(WeaponTier.SILVER, WeaponCatalog.tierFor(6))
        assertEquals(WeaponTier.RUNE, WeaponCatalog.tierFor(11))
        assertEquals(WeaponTier.FLAME, WeaponCatalog.tierFor(12))
        assertEquals(WeaponTier.THUNDER, WeaponCatalog.tierFor(15))
        assertEquals(WeaponTier.DAWN, WeaponCatalog.tierFor(17))
        assertEquals(WeaponTier.BLACK_DRAGON, WeaponCatalog.tierFor(19))
        assertEquals(WeaponTier.BLACK_DRAGON, WeaponCatalog.tierFor(20))
    }

    @Test
    fun `무한 구간 티어 경계가 스펙과 일치한다`() {
        assertEquals(WeaponTier.DRAGON_SCALE, WeaponCatalog.tierFor(21))
        assertEquals(WeaponTier.DRAGON_SCALE, WeaponCatalog.tierFor(25))
        assertEquals(WeaponTier.ABYSS, WeaponCatalog.tierFor(26))
        assertEquals(WeaponTier.ABYSS, WeaponCatalog.tierFor(30))
        assertEquals(WeaponTier.NAMELESS, WeaponCatalog.tierFor(31))
        assertEquals(WeaponTier.NAMELESS, WeaponCatalog.tierFor(9999))
    }

    @Test
    fun `0부터 40까지 어느 단계에도 대응 티어가 있다`() {
        for (level in 0..40) {
            WeaponCatalog.tierFor(level)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `음수 단계에는 티어가 없다`() {
        WeaponCatalog.tierFor(-1)
    }

    @Test
    fun `티어 구간은 빈틈도 겹침도 없다`() {
        val sorted = WeaponTier.entries.sortedBy { it.minLevel }
        assertEquals(0, sorted.first().minLevel)
        for (i in 1 until sorted.size) {
            assertEquals(
                "${sorted[i].id} 앞 구간과 이어지지 않는다",
                sorted[i - 1].maxLevel + 1,
                sorted[i].minLevel,
            )
        }
        assertEquals(Int.MAX_VALUE, sorted.last().maxLevel)
    }

    @Test
    fun `일반 티어는 네 모드 모두에서 얻을 수 있다`() {
        assertEquals(4, WeaponCatalog.difficultiesFor(WeaponTier.RUSTY).size)
        assertEquals(4, WeaponCatalog.difficultiesFor(WeaponTier.BLACK_DRAGON).size)
    }

    @Test
    fun `무한 전용 티어는 무한 모드에서만 얻을 수 있다`() {
        assertEquals(
            listOf(Difficulty.ENDLESS),
            WeaponCatalog.difficultiesFor(WeaponTier.ABYSS),
        )
    }

    @Test
    fun `도감 엔트리는 계열 12종 곱하기 티어 11종으로 132개다`() {
        assertEquals(132, WeaponCatalog.ENTRIES.size)
        assertEquals(132, WeaponCatalog.ENTRIES.toSet().size)
    }

    @Test
    fun `모든 계열이 모든 티어를 하나씩 갖는다`() {
        for (family in WeaponFamily.entries) {
            val tiers = WeaponCatalog.ENTRIES.filter { it.family == family }.map { it.tier }
            assertEquals(WeaponTier.entries.size, tiers.size)
            assertEquals(WeaponTier.entries.toSet(), tiers.toSet())
        }
    }

    @Test
    fun `오라는 15단계부터 그린다`() {
        assertEquals(15, WeaponCatalog.AURA_MIN_LEVEL)
        assertTrue(WeaponTier.THUNDER.minLevel == WeaponCatalog.AURA_MIN_LEVEL)
    }
}
