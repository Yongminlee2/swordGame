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
     * v2.5 — 용검도 계열 칸을 갖고, 조합검은 +0 칸이 없다.
     *
     * 기본 4계열 × 21(+0~+20) = 84, 조합검 3계열 × 20(+1~+20) = 60, 전설 30 → 174.
     * 숨긴 계열의 칸은 도감 어디에도 없다.
     */
    @Test
    fun `도감 엔트리는 계열칸 144에 전설칸 30을 더해 174개다`() {
        assertEquals(174, WeaponCatalog.ENTRIES.size)
        assertEquals(174, WeaponCatalog.ENTRIES.toSet().size)
        assertEquals(144, WeaponCatalog.ENTRIES.count { it.family != null })
        assertEquals(30, WeaponCatalog.ENTRIES.count { it.family == null })
    }

    /**
     * 조합검은 **+1 이 시작이다.** 상점에 나오지 않고 조합이 +1 을 내놓으며,
     * +1~+5 는 안전구간이라 하락으로도 +0 에 닿지 않는다 — +0 칸을 두면 영원히
     * 못 채우는 칸이 된다.
     */
    @Test
    fun `조합검은 1부터 기본 계열은 0부터 20단계까지 한 칸씩 갖는다`() {
        for (family in WeaponFamily.entries) {
            val levels = WeaponCatalog.ENTRIES.filter { it.family == family }.map { it.level }
            when {
                family in LegendForge.REFINED_FAMILIES -> {
                    assertEquals("$family", 20, levels.size)
                    assertEquals("$family", (1..20).toSet(), levels.toSet())
                }
                family in WeaponFamily.CODEX_FAMILIES -> {
                    assertEquals("$family", 21, levels.size)
                    assertEquals("$family", (0..20).toSet(), levels.toSet())
                }
                // 숨긴 계열은 계열 칸이 없다
                else -> assertEquals("$family", 0, levels.size)
            }
        }
    }

    /** +21 위는 계열과 무관하게 같은 그림이라 칸도 계열마다 두지 않는다. */
    @Test
    fun `21단계 위는 계열 없는 전설 칸으로 간다`() {
        assertEquals(CodexEntry(WeaponFamily.DRAGON, 20), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 20))
        assertEquals(CodexEntry(null, 21), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 21))
        assertEquals(CodexEntry(null, 21), WeaponCatalog.slotFor(WeaponFamily.HOLY, 21))
    }

    /** +21~+50 은 저마다 칸을 갖는다. */
    @Test
    fun `21부터 50까지는 저마다 칸을 갖는다`() {
        assertEquals(CodexEntry(null, 21), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 21))
        assertEquals(CodexEntry(null, 40), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 40))
        assertEquals(CodexEntry(null, 45), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 45))
        assertEquals(CodexEntry(null, 50), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 50))
    }

    /** 그림이 더 없는 구간은 마지막 칸으로 모인다. */
    @Test
    fun `51단계 위는 마지막 칸을 쓴다`() {
        assertEquals(CodexEntry(null, 50), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 51))
        assertEquals(CodexEntry(null, 50), WeaponCatalog.slotFor(WeaponFamily.DRAGON, 9999))
    }

    @Test
    fun `오라는 15단계부터 그린다`() {
        assertEquals(15, WeaponCatalog.AURA_MIN_LEVEL)
        assertTrue(WeaponTier.THUNDER.minLevel == WeaponCatalog.AURA_MIN_LEVEL)
    }
}
