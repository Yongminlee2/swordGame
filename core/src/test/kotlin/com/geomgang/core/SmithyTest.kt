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

    /**
     * **값은 검 단계를 따라가지 않는다**(v2.3).
     *
     * 한때 `강화 비용(최고 단계) × 5` 였다 — 검이 오를수록 같은 스킬 한 칸이
     * 비싸져서, 미루면 손해라 "지금 사야 하나"를 늘 계산해야 했다. 무엇보다
     * 값이 왜 바뀌는지 화면이 설명하지 못했다. 스킬은 영구 성장이니 사다리는
     * 레벨만 보고 오른다.
     */
    @Test
    fun `값은 검 단계와 무관하다`() {
        assertEquals(Smithy.priceOf(rich(10), 0), Smithy.priceOf(rich(40), 0))
        assertEquals(Smithy.priceOf(rich(10), 7), Smithy.priceOf(rich(40), 7))
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

    /** 화면에서 읽는 이름은 "스킬" 이다. 도메인 이름([Smithy])과 달라도 된다. */
    @Test
    fun `스킬이 강화 보너스 출처에 들어간다`() {
        val leveled = ProgressState(smithyLevel = 3)
        assertTrue(ForgeBonuses.of(rich(), leveled).successRate > 0.0)
        assertTrue(ForgeBonuses.sourcesOf(rich(), leveled).any { it.label == "스킬" })
    }
}
