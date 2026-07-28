package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmithyTest {

    private fun rich(bestLevel: Int = 20) = GameState(
        difficulty = Difficulty.ENDLESS,
        gold = 1_000_000_000_000_000L,
        bestLevel = bestLevel,
    )

    @Test
    fun `레벨당 같은 몫이 오른다`() {
        assertEquals(0.0, Smithy.bonusOf(ProgressState()).successRate, 1e-9)
        assertEquals(
            Smithy.PER_LEVEL * 5,
            Smithy.bonusOf(ProgressState(smithyLevel = 5)).successRate,
            1e-9,
        )
    }

    @Test
    fun `상한을 넘지 않는다`() {
        val capped = ProgressState(smithyLevel = Smithy.MAX_LEVEL)
        assertEquals(Smithy.PER_LEVEL * Smithy.MAX_LEVEL, Smithy.bonusOf(capped).successRate, 1e-9)
        assertFalse(Smithy.canUpgrade(rich(), capped))
    }

    @Test
    fun `값은 레벨이 오를수록 비싸진다`() {
        val s = rich()
        assertTrue(Smithy.priceOf(s, 1) > Smithy.priceOf(s, 0))
        assertTrue(Smithy.priceOf(s, 10) > Smithy.priceOf(s, 5))
    }

    /** 고정값이면 초반엔 못 사고 후반엔 공짜가 된다. 지금 강화 비용에 연동한다. */
    @Test
    fun `값이 최고 단계를 따라 오른다`() {
        assertTrue(Smithy.priceOf(rich(40), 0) > Smithy.priceOf(rich(10), 0))
    }

    @Test
    fun `골드가 모자라면 못 올린다`() {
        val poor = GameState(Difficulty.ENDLESS, gold = 1, bestLevel = 20)
        assertFalse(Smithy.canUpgrade(poor, ProgressState()))
    }

    @Test
    fun `올리면 레벨이 하나 오르고 골드가 빠진다`() {
        val state = rich()
        val price = Smithy.priceOf(state, 0)
        val (nextState, nextProgress) = Smithy.upgrade(state, ProgressState())
        assertEquals(1, nextProgress.smithyLevel)
        assertEquals(state.gold - price, nextState.gold)
    }

    @Test
    fun `대장간이 강화 보너스 출처에 들어간다`() {
        val leveled = ProgressState(smithyLevel = 3)
        assertTrue(ForgeBonuses.of(rich(), leveled).successRate > 0.0)
        assertTrue(ForgeBonuses.sourcesOf(rich(), leveled).any { it.label == "대장간" })
    }
}
