package com.geomgang.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemperedRateTest {

    @Test
    fun `담금질 인자를 안 주면 예전과 같은 값이다`() {
        assertEquals(
            RateTable.baseSuccessRate(45) * Difficulty.ENDLESS.multiplier,
            RateTable.successRate(Difficulty.ENDLESS, 45),
            1e-9,
        )
    }

    @Test
    fun `실패가 쌓이면 성공률이 오른다`() {
        val cold = RateTable.successRate(Difficulty.ENDLESS, 45, temperFails = 0)
        val warm = RateTable.successRate(Difficulty.ENDLESS, 45, temperFails = 20)
        assertTrue("cold=$cold warm=$warm", warm > cold)
    }

    /** 축복서는 "이번 판만 얹는 것" 이므로 누적분 위에 얹혀야 말이 된다. */
    @Test
    fun `축복서는 담금질 뒤에 더해진다`() {
        val tempered = RateTable.successRate(Difficulty.ENDLESS, 45, temperFails = 10)
        val withScroll =
            RateTable.successRate(Difficulty.ENDLESS, 45, blessing = true, temperFails = 10)
        assertEquals(tempered + RateTable.BLESSING_BONUS, withScroll, 1e-9)
    }

    @Test
    fun `최종 상한을 넘지 않는다`() {
        val rate = RateTable.successRate(
            Difficulty.ENDLESS,
            45,
            blessing = true,
            temperFails = 100_000,
        )
        assertTrue("rate=$rate", rate <= RateTable.MAX_SUCCESS_RATE)
    }

    @Test
    fun `유한 구간은 담금질을 받지 않는다`() {
        assertEquals(
            RateTable.successRate(Difficulty.NORMAL, 10),
            RateTable.successRate(Difficulty.NORMAL, 10, temperFails = 50),
            1e-9,
        )
    }

    @Test
    fun `확률 표시도 담금질을 반영한다`() {
        val cold = ForgeOdds.of(Difficulty.ENDLESS, 45, temperFails = 0)
        val warm = ForgeOdds.of(Difficulty.ENDLESS, 45, temperFails = 30)
        assertTrue("cold=${cold.success} warm=${warm.success}", warm.success > cold.success)
        // 성공이 오르면 파괴가 그만큼 줄어야 한다. 넷의 합은 언제나 1이다.
        assertTrue(warm.destroy < cold.destroy)
        assertEquals(1.0, warm.success + warm.stay + warm.drop + warm.destroy, 1e-9)
    }

    @Test
    fun `부적을 켜면 실패분이 전부 유지로 간다`() {
        val odds = ForgeOdds.of(
            Difficulty.ENDLESS,
            45,
            UsedItems(luckCharm = true),
            temperFails = 30,
        )
        assertEquals(0.0, odds.destroy, 1e-9)
        assertEquals(0.0, odds.drop, 1e-9)
        assertEquals(1.0 - odds.success, odds.stay, 1e-9)
    }
}
