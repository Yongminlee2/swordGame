package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateTableTest {

    private val eps = 1e-9

    @Test
    fun `기준 성공률은 스펙 표와 일치한다`() {
        assertEquals(0.95, RateTable.baseSuccessRate(1), eps)
        assertEquals(0.75, RateTable.baseSuccessRate(5), eps)
        assertEquals(0.40, RateTable.baseSuccessRate(10), eps)
        assertEquals(0.15, RateTable.baseSuccessRate(15), eps)
        assertEquals(0.02, RateTable.baseSuccessRate(20), eps)
    }

    @Test
    fun `기준 성공률은 단계가 오를수록 단조 감소한다`() {
        for (level in 2..RateTable.MAX_FINITE_LEVEL) {
            val prev = RateTable.baseSuccessRate(level - 1)
            val cur = RateTable.baseSuccessRate(level)
            assertTrue("level=$level: $cur >= $prev", cur < prev)
        }
    }

    @Test
    fun `난이도 배수가 적용된다`() {
        assertEquals(0.40, RateTable.successRate(Difficulty.NORMAL, 10), eps)
        assertEquals(0.30, RateTable.successRate(Difficulty.HARD, 10), eps)
        assertEquals(0.50, RateTable.successRate(Difficulty.EASY, 10), eps)
    }

    @Test
    fun `쉬움 모드 저단계는 98퍼센트 상한에 걸린다`() {
        // 0.95 * 1.25 = 1.1875 이므로 상한으로 잘린다
        assertEquals(0.98, RateTable.successRate(Difficulty.EASY, 1), eps)
    }

    @Test
    fun `축복서는 난이도 배수를 적용한 뒤에 더해진다`() {
        // 지옥 +10: 0.40 * 0.75 = 0.30, 여기에 +0.10
        assertEquals(0.40, RateTable.successRate(Difficulty.HARD, 10, blessing = true), eps)
        assertEquals(0.50, RateTable.successRate(Difficulty.NORMAL, 10, blessing = true), eps)
    }

    @Test
    fun `축복서를 써도 98퍼센트를 넘지 못한다`() {
        assertEquals(0.98, RateTable.successRate(Difficulty.NORMAL, 1, blessing = true), eps)
    }

    @Test
    fun `무한 모드는 21단계부터 직전의 85퍼센트로 감쇠한다`() {
        assertEquals(0.02 * 0.85, RateTable.successRate(Difficulty.ENDLESS, 21), eps)
        assertEquals(0.02 * 0.85 * 0.85, RateTable.successRate(Difficulty.ENDLESS, 22), eps)
    }

    @Test
    fun `무한 모드 성공률은 0점5퍼센트 아래로 내려가지 않는다`() {
        assertEquals(0.005, RateTable.successRate(Difficulty.ENDLESS, 100), eps)
        assertTrue(RateTable.successRate(Difficulty.ENDLESS, 60) >= RateTable.ENDLESS_FLOOR)
    }

    @Test
    fun `모든 난이도 모든 단계에서 성공률은 0과 1 사이다`() {
        for (difficulty in Difficulty.entries) {
            for (level in 1..RateTable.MAX_FINITE_LEVEL) {
                for (blessing in listOf(false, true)) {
                    val rate = RateTable.successRate(difficulty, level, blessing)
                    assertTrue(
                        "$difficulty level=$level blessing=$blessing rate=$rate",
                        rate in 0.0..1.0,
                    )
                }
            }
        }
    }

    @Test
    fun `실패 구간 경계가 스펙과 일치한다`() {
        assertEquals(FailureBand.STAY, RateTable.failureBand(1))
        assertEquals(FailureBand.STAY, RateTable.failureBand(5))
        assertEquals(FailureBand.DROP, RateTable.failureBand(6))
        assertEquals(FailureBand.DROP, RateTable.failureBand(12))
        assertEquals(FailureBand.DESTROY_OR_DROP, RateTable.failureBand(13))
        assertEquals(FailureBand.DESTROY_OR_DROP, RateTable.failureBand(20))
        assertEquals(FailureBand.DESTROY_OR_DROP, RateTable.failureBand(21))
    }

    @Test
    fun `파괴 확률이 스펙 표와 일치한다`() {
        assertEquals(0.00, RateTable.destroyChance(12), eps)
        assertEquals(0.40, RateTable.destroyChance(13), eps)
        assertEquals(0.40, RateTable.destroyChance(15), eps)
        assertEquals(0.60, RateTable.destroyChance(16), eps)
        assertEquals(0.60, RateTable.destroyChance(18), eps)
        assertEquals(0.80, RateTable.destroyChance(19), eps)
        assertEquals(0.80, RateTable.destroyChance(20), eps)
    }

    @Test
    fun `무한 구간 실패는 항상 파괴다`() {
        assertEquals(1.00, RateTable.destroyChance(21), eps)
        assertEquals(1.00, RateTable.destroyChance(50), eps)
    }

    @Test
    fun `난이도별 상한이 스펙과 일치한다`() {
        assertEquals(20, Difficulty.EASY.maxLevel)
        assertEquals(20, Difficulty.NORMAL.maxLevel)
        assertEquals(20, Difficulty.HARD.maxLevel)
        assertEquals(null, Difficulty.ENDLESS.maxLevel)
        assertTrue(Difficulty.ENDLESS.isEndless)
        assertTrue(!Difficulty.NORMAL.isEndless)
    }

    @Test
    fun `아이디로 난이도를 찾을 수 있다`() {
        assertEquals(Difficulty.HARD, Difficulty.fromId("hard"))
    }
}
