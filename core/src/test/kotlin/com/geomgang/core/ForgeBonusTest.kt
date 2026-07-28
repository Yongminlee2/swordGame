package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForgeBonusTest {

    private val empty = ProgressState()
    private val fresh = GameState(Difficulty.ENDLESS)

    @Test
    fun `아무것도 없으면 보너스가 0이다`() {
        val bonus = ForgeBonuses.of(fresh, empty)
        assertEquals(0.0, bonus.successRate, 1e-9)
        assertEquals(0.0, bonus.destroyGuard, 1e-9)
    }

    @Test
    fun `더하면 항목별로 더해진다`() {
        val a = ForgeBonus(0.01, 0.02)
        val b = ForgeBonus(0.03, 0.04)
        assertEquals(ForgeBonus(0.04, 0.06), a + b)
    }

    @Test
    fun `출처 목록을 다 더하면 합계와 같다`() {
        val sources = ForgeBonuses.sourcesOf(fresh, empty)
        val summed = sources.fold(ForgeBonus.NONE) { acc, s -> acc + s.bonus }
        assertEquals(ForgeBonuses.of(fresh, empty), summed)
    }

    /** 보너스가 0이면 확률표가 예전과 똑같아야 한다. 시뮬레이터가 보는 상태다. */
    @Test
    fun `보너스 0이면 예전 확률 그대로다`() {
        assertEquals(
            RateTable.baseSuccessRate(45) * Difficulty.ENDLESS.multiplier,
            RateTable.successRate(Difficulty.ENDLESS, 45, bonus = 0.0),
            1e-9,
        )
    }

    @Test
    fun `보너스는 성공률에 더해진다`() {
        val plain = RateTable.successRate(Difficulty.ENDLESS, 45)
        val boosted = RateTable.successRate(Difficulty.ENDLESS, 45, bonus = 0.05)
        assertEquals(plain + 0.05, boosted, 1e-9)
    }

    @Test
    fun `보너스를 얹어도 최종 상한을 넘지 않는다`() {
        val rate = RateTable.successRate(Difficulty.ENDLESS, 1, blessing = true, bonus = 0.5)
        assertTrue("rate=$rate", rate <= RateTable.MAX_SUCCESS_RATE)
    }
}
