package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 도감은 저절로 채워지지 않는다. 검을 바쳐야 칸이 열린다.
 *
 * 예전에는 검을 얻으면 자동으로 올라서 도감이 "지나간 자취" 일 뿐이었다.
 * 이제 "계속 강화할까, 여기서 바칠까" 가 매번 선택이 된다.
 */
class CodexOfferTest {

    private val empty = ProgressState()
    private val sword = Sword(WeaponFamily.STRAIGHT, 7)

    @Test
    fun `빈 칸이면 바칠 수 있다`() {
        assertTrue(CodexOffer.canOffer(empty, sword))
    }

    @Test
    fun `바치면 그 칸이 열린다`() {
        val after = CodexOffer.offer(empty, Difficulty.ENDLESS, sword)
        assertTrue(CodexEntry(WeaponFamily.STRAIGHT, 7) in Progress.entriesOf(after))
    }

    /** 이미 찬 칸에 또 바치면 검만 사라진다. 헛되이 태우지 않게 막는다. */
    @Test
    fun `이미 찬 칸은 못 바친다`() {
        val after = CodexOffer.offer(empty, Difficulty.ENDLESS, sword)
        assertFalse(CodexOffer.canOffer(after, sword))
    }

    /**
     * **한 칸마다** 보너스가 오른다(v2.3).
     *
     * 계단식(10칸마다)이었을 때는 아홉 칸을 바쳐도 숫자가 0이라
     * "도감에 넣었는데 왜 +가 없냐"는 말이 실기기에서 실제로 나왔다.
     */
    @Test
    fun `한 칸마다 보너스가 오른다`() {
        var p = empty
        p = CodexOffer.offer(p, Difficulty.ENDLESS, Sword(WeaponFamily.STRAIGHT, 0))
        assertEquals(CodexOffer.PER_SLOT_BONUS, CodexOffer.bonusOf(p).successRate, 1e-9)

        for (level in 1 until 10) {
            p = CodexOffer.offer(p, Difficulty.ENDLESS, Sword(WeaponFamily.STRAIGHT, level))
        }
        val bonus = CodexOffer.bonusOf(p)
        assertEquals(CodexOffer.PER_SLOT_BONUS * 10, bonus.successRate, 1e-9)
        assertEquals(CodexOffer.PER_SLOT_BONUS * 10, bonus.dropGuard, 1e-9)
    }

    /** 전체를 다 채워도 상한을 넘지 않는다. */
    @Test
    fun `다 채우면 상한에 닿는다`() {
        var p = empty
        for (entry in WeaponCatalog.ENTRIES) {
            val family = entry.family ?: WeaponFamily.STRAIGHT
            p = CodexOffer.offer(p, Difficulty.ENDLESS, Sword(family, entry.level))
        }
        assertEquals(CodexOffer.MAX_BONUS, CodexOffer.bonusOf(p).successRate, 1e-9)
    }

    /** 보너스가 실제로 합산에 들어가는지. */
    @Test
    fun `도감이 강화 보너스 출처에 들어간다`() {
        var p = empty
        for (level in 0 until 10) {
            p = CodexOffer.offer(p, Difficulty.ENDLESS, Sword(WeaponFamily.STRAIGHT, level))
        }
        val fresh = GameState(Difficulty.ENDLESS)
        assertTrue(ForgeBonuses.of(fresh, p).successRate > 0.0)
        assertTrue(ForgeBonuses.sourcesOf(fresh, p).any { it.label == "도감" })
    }
}
