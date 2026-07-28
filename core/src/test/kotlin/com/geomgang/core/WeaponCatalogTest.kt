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

    /**
     * 그림 한 장에 칸 하나다.
     *
     * 계열 14 × 단계 21 = 294 에 전설 29(+21~+49)를 더해 323.
     * 전설 29 는 시트3 의 전설 칸 20(+21~+40)과 낱장 그림 9(+41~+49)를 합한 수다.
     * 어긋나면 도감에 자리가 없는 그림이 생기거나 그림 없는 칸이 생긴다.
     */
    @Test
    fun `도감 엔트리는 계열칸 294에 전설칸 29를 더해 323개다`() {
        assertEquals(323, WeaponCatalog.ENTRIES.size)
        assertEquals(323, WeaponCatalog.ENTRIES.toSet().size)
        assertEquals(294, WeaponCatalog.ENTRIES.count { it.family != null })
        assertEquals(29, WeaponCatalog.ENTRIES.count { it.family == null })
    }

    @Test
    fun `모든 계열이 0부터 20단계까지 한 칸씩 갖는다`() {
        for (family in WeaponFamily.entries) {
            val levels = WeaponCatalog.ENTRIES.filter { it.family == family }.map { it.level }
            assertEquals(21, levels.size)
            assertEquals((0..20).toSet(), levels.toSet())
        }
    }

    /** +21 위는 계열과 무관하게 같은 그림이라 칸도 계열마다 두지 않는다. */
    @Test
    fun `21단계 위는 계열 없는 전설 칸으로 간다`() {
        assertEquals(CodexEntry(WeaponFamily.DRAGON, 20), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 20))
        assertEquals(CodexEntry(null, 21), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 21))
        assertEquals(CodexEntry(null, 21), WeaponCatalog.slotFor(WeaponFamily.HOLY, 21))
    }

    /** +41~+49 는 낱장 그림이라 각자 칸을 갖는다. */
    @Test
    fun `41부터 49까지는 저마다 칸을 갖는다`() {
        assertEquals(CodexEntry(null, 41), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 41))
        assertEquals(CodexEntry(null, 45), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 45))
        assertEquals(CodexEntry(null, 49), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 49))
    }

    /** 그림이 더 없는 구간은 마지막 칸으로 모인다. */
    @Test
    fun `50단계 위는 마지막 칸을 쓴다`() {
        assertEquals(CodexEntry(null, 49), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 50))
        assertEquals(CodexEntry(null, 49), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 9999))
    }

    @Test
    fun `오라는 15단계부터 그린다`() {
        assertEquals(15, WeaponCatalog.AURA_MIN_LEVEL)
        assertTrue(WeaponTier.THUNDER.minLevel == WeaponCatalog.AURA_MIN_LEVEL)
    }
}
